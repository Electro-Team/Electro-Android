package com.example.melectro.domain.provider

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import com.example.melectro.domain.server.AbstractDnsServer
import com.example.melectro.domain.service.DaedalusVpnService
import java.io.*
import java.lang.Exception
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.*
import okhttp3.internal.and
import org.pcap4j.packet.IpPacket
import kotlin.experimental.or


open class TcpProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    UdpProvider(descriptor, service) {
    protected val dnsIn: WospList = WospList()
    override fun process() {
        try {
            val pipes = Os.pipe()
            mInterruptFd = pipes[0]
            mBlockFd = pipes[1]
            val inputStream: FileInputStream = FileInputStream(descriptor?.fileDescriptor)
            val outputStream: FileOutputStream = FileOutputStream(descriptor?.fileDescriptor)
            val packet = ByteArray(32767)
            while (running) {
                val deviceFd = StructPollfd()
                deviceFd.fd = inputStream.fd
                deviceFd.events = OsConstants.POLLIN.toShort()
                val blockFd = StructPollfd()
                blockFd.fd = mBlockFd
                blockFd.events = (OsConstants.POLLHUP or OsConstants.POLLERR).toShort()
                if (!deviceWrites.isEmpty()) deviceFd.events =
                    deviceFd.events or  OsConstants.POLLOUT.toShort()
                val polls = arrayOfNulls<StructPollfd>(2 + dnsIn.size())
                polls[0] = deviceFd
                polls[1] = blockFd
                run {
                    var i = -1
                    for (wosp in dnsIn) {
                        i++
                        polls[2 + i] = StructPollfd()
                        val pollFd = polls[2 + i]
                        pollFd!!.fd =
                            ParcelFileDescriptor.fromSocket(wosp.socket).fileDescriptor
                        pollFd.events = OsConstants.POLLIN.toShort()
                    }
                }
                Log.d(
                    TAG,
                    "doOne: Polling " + polls.size + " file descriptors"
                )
                Os.poll(polls, -1)
                if (blockFd.revents.toInt() != 0) {
                    Log.i(TAG, "Told to stop VPN")
                    running = false
                    return
                }

                // Need to do this before reading from the device, otherwise a new insertion there could
                // invalidate one of the sockets we want to read from either due to size or time out
                // constraints
                run {
                    var i = -1
                    val iter: MutableIterator<WaitingOnSocketPacket> =
                        dnsIn.iterator() as MutableIterator<WaitingOnSocketPacket>
                    while (iter.hasNext()) {
                        i++
                        val wosp: WaitingOnSocketPacket = iter.next()
                        if (polls[i + 2]!!.revents and OsConstants.POLLIN != 0) {
                            Log.d(
                                TAG,
                                "Read from TCP DNS socket" + wosp.socket
                            )
                            iter.remove()
                            handleRawDnsResponse(wosp.packet, wosp.socket)
                            wosp.socket.close()
                        }
                    }
                }
                if (deviceFd.revents and OsConstants.POLLOUT != 0) {
                    Log.d(TAG, "Write to device")
                    writeToDevice(outputStream)
                }
                if (deviceFd.revents and  OsConstants.POLLIN != 0) {
                    Log.d(TAG, "Read from device")
                    readPacketFromDevice(inputStream, packet)
                }
                service.providerLoopCallback()
            }
        } catch (e: Exception) {
        }
    }

    protected fun processUdpPacket(outPacket: DatagramPacket, parsedPacket: IpPacket?): ByteArray {
        return if (parsedPacket == null) {
            ByteArray(0)
        } else outPacket.data
    }


   // @Throws(DaedalusVpnService.VpnNetworkException::class)
    override fun forwardPacket(
       outPacket: DatagramPacket,
       parsedPacket: IpPacket?,
       dnsServer: AbstractDnsServer?
   ) {
        val dnsSocket: Socket
        try {
            // Packets to be sent to the real DNS server will need to be protected from the VPN
            dnsSocket = SocketChannel.open().socket()
            service.protect(dnsSocket)
            val address: SocketAddress = InetSocketAddress(outPacket.address, dnsServer!!.port)
            dnsSocket.connect(address, 5000)
            dnsSocket.soTimeout = 5000
//            Logger.info("TcpProvider: Sending DNS query request")
            val dos = DataOutputStream(dnsSocket.getOutputStream())
            val packet = processUdpPacket(outPacket, parsedPacket)
            dos.writeShort(packet.size)
            dos.write(packet)
            dos.flush()
            if (parsedPacket != null) {
                dnsIn.add(WaitingOnSocketPacket(dnsSocket, parsedPacket))
            } else {
                dnsSocket.close()
            }
        } catch (e: IOException) {
            if (e.cause is ErrnoException) {
                val errnoExc = e.cause as ErrnoException?
                if (errnoExc!!.errno == OsConstants.ENETUNREACH || errnoExc.errno == OsConstants.EPERM) {
                    throw DaedalusVpnService.VpnNetworkException("Cannot send message:", e)
                }
            }
            Log.w(
                TAG,
                "handleDnsRequest: Could not send packet to upstream",
                e
            )
        }
    }

    private fun handleRawDnsResponse(parsedPacket: IpPacket, dnsSocket: Socket) {
        try {
            val stream = DataInputStream(dnsSocket.getInputStream())
            val length = stream.readUnsignedShort()
            Log.d(TAG, "Reading length: $length")
            val data = ByteArray(length)
            stream.read(data)
            dnsSocket.close()
            handleDnsResponse(parsedPacket, data)
        } catch (ignored: Exception) {
        }
    }

    /**
     * Helper class holding a socket, the packet we are waiting the answer for, and a time
     */
    class WaitingOnSocketPacket internal constructor(val socket: Socket, packet: IpPacket) {
        val packet: IpPacket
        private val time: Long
        fun ageSeconds(): Long {
            return (System.currentTimeMillis() - time) / 1000
        }

        init {
            this.packet = packet
            time = System.currentTimeMillis()
        }
    }

    /**
     * Queue of WaitingOnSocketPacket, bound on time and space.
     */
    class WospList : Iterable<WaitingOnSocketPacket?> {
        private val list: LinkedList<WaitingOnSocketPacket> =
            LinkedList<WaitingOnSocketPacket>()

        fun add(wosp: WaitingOnSocketPacket) {
            try {
                if (list.size > 1024) {
                    Log.d(
                        TAG,
                        "Dropping socket due to space constraints: " + list.element().socket
                    )
                    list.element().socket.close()
                    list.remove()
                }
                while (!list.isEmpty() && list.element().ageSeconds() > 10) {
                    Log.d(TAG, "Timeout on socket " + list.element().socket)
                    list.element().socket.close()
                    list.remove()
                }
                list.add(wosp)
            } catch (ignored: Exception) {
            }
        }

        override fun iterator(): Iterator<WaitingOnSocketPacket> {
            return list.iterator()
        }

        fun size(): Int {
            return list.size
        }
    }

    companion object {
        private const val TAG = "TcpProvider"
    }
}