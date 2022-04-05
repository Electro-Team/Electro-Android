package com.example.melectro.provider

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import com.example.melectro.server.AbstractDnsServer
import com.example.melectro.service.DaedalusVpnService
import java.io.*
import java.lang.Exception
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.*
import com.example.melectro.utill.Logger
import okhttp3.internal.and
import org.pcap4j.packet.IpPacket
import kotlin.experimental.or


open class TcpProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    com.example.melectro.provider.UdpProvider(descriptor, service) {
    protected val dnsIn: TcpProvider.WospList = TcpProvider.WospList()
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
                    TcpProvider.Companion.TAG,
                    "doOne: Polling " + polls.size + " file descriptors"
                )
                Os.poll(polls, -1)
                if (blockFd.revents.toInt() != 0) {
                    Log.i(TcpProvider.Companion.TAG, "Told to stop VPN")
                    running = false
                    return
                }

                // Need to do this before reading from the device, otherwise a new insertion there could
                // invalidate one of the sockets we want to read from either due to size or time out
                // constraints
                run {
                    var i = -1
                    val iter: MutableIterator<TcpProvider.WaitingOnSocketPacket> =
                        dnsIn.iterator() as MutableIterator<WaitingOnSocketPacket>
                    while (iter.hasNext()) {
                        i++
                        val wosp: TcpProvider.WaitingOnSocketPacket = iter.next()
                        if (polls[i + 2]!!.revents and OsConstants.POLLIN != 0) {
                            Log.d(
                                TcpProvider.Companion.TAG,
                                "Read from TCP DNS socket" + wosp.socket
                            )
                            iter.remove()
                            handleRawDnsResponse(wosp.packet, wosp.socket)
                            wosp.socket.close()
                        }
                    }
                }
                if (deviceFd.revents and OsConstants.POLLOUT != 0) {
                    Log.d(TcpProvider.Companion.TAG, "Write to device")
                    writeToDevice(outputStream)
                }
                if (deviceFd.revents and  OsConstants.POLLIN != 0) {
                    Log.d(TcpProvider.Companion.TAG, "Read from device")
                    readPacketFromDevice(inputStream, packet)
                }
                service.providerLoopCallback()
            }
        } catch (e: Exception) {
            Logger.logException(e)
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
            Logger.info("TcpProvider: Sending DNS query request")
            val dos = DataOutputStream(dnsSocket.getOutputStream())
            val packet = processUdpPacket(outPacket, parsedPacket)
            dos.writeShort(packet.size)
            dos.write(packet)
            dos.flush()
            if (parsedPacket != null) {
                dnsIn.add(TcpProvider.WaitingOnSocketPacket(dnsSocket, parsedPacket))
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
                TcpProvider.Companion.TAG,
                "handleDnsRequest: Could not send packet to upstream",
                e
            )
        }
    }

    private fun handleRawDnsResponse(parsedPacket: IpPacket, dnsSocket: Socket) {
        try {
            val stream = DataInputStream(dnsSocket.getInputStream())
            val length = stream.readUnsignedShort()
            Log.d(TcpProvider.Companion.TAG, "Reading length: $length")
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
    class WospList : Iterable<TcpProvider.WaitingOnSocketPacket?> {
        private val list: LinkedList<TcpProvider.WaitingOnSocketPacket> =
            LinkedList<TcpProvider.WaitingOnSocketPacket>()

        fun add(wosp: TcpProvider.WaitingOnSocketPacket) {
            try {
                if (list.size > 1024) {
                    Log.d(
                        TcpProvider.Companion.TAG,
                        "Dropping socket due to space constraints: " + list.element().socket
                    )
                    list.element().socket.close()
                    list.remove()
                }
                while (!list.isEmpty() && list.element().ageSeconds() > 10) {
                    Log.d(TcpProvider.Companion.TAG, "Timeout on socket " + list.element().socket)
                    list.element().socket.close()
                    list.remove()
                }
                list.add(wosp)
            } catch (ignored: Exception) {
            }
        }

        override fun iterator(): Iterator<TcpProvider.WaitingOnSocketPacket> {
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