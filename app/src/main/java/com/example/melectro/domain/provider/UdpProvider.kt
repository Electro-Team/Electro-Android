package com.example.melectro.domain.provider

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import com.example.melectro.domain.server.AbstractDnsServer
import com.example.melectro.utill.Logger
import com.example.melectro.domain.service.DaedalusVpnService
import okhttp3.internal.and
import org.minidns.dnsmessage.DnsMessage
import org.pcap4j.packet.IpPacket
import org.pcap4j.packet.IpSelector
import org.pcap4j.packet.UdpPacket
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import kotlin.experimental.or

open class UdpProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    Provider(descriptor, service!!) {
    private val dnsIn = WospList()
    override fun process() {
        try {
            val pipes = Os.pipe()
            mInterruptFd = pipes[0]
            mBlockFd = pipes[1]
            val inputStream = FileInputStream(descriptor!!.fileDescriptor)
            val outputStream = FileOutputStream(descriptor!!.fileDescriptor)
            val packet = ByteArray(32767)
            while (running) {
                val deviceFd = StructPollfd()
                deviceFd.fd = inputStream.fd
                deviceFd.events = OsConstants.POLLIN.toShort()
                val blockFd = StructPollfd()
                blockFd.fd = mBlockFd
                blockFd.events = (OsConstants.POLLHUP or OsConstants.POLLERR).toShort()
                if (!deviceWrites.isEmpty()) deviceFd.events =
                    deviceFd.events or OsConstants.POLLOUT.toShort()
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
                            ParcelFileDescriptor.fromDatagramSocket(wosp.socket).fileDescriptor
                        pollFd.events = OsConstants.POLLIN.toShort()
                    }
                }
                Log.d(TAG, "doOne: Polling " + polls.size + " file descriptors")
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
                    val iter =
                        dnsIn.iterator()
                    while (iter.hasNext()) {
                        i++
                        val wosp = iter.next()
                        if (polls[i + 2]!!.revents and OsConstants.POLLIN != 0) {
                            Log.d(
                                TAG,
                                "Read from UDP DNS socket" + wosp.socket
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
                if (deviceFd.revents and OsConstants.POLLIN != 0) {
                    Log.d(TAG, "Read from device")
                    readPacketFromDevice(inputStream, packet)
                }
                service.providerLoopCallback()
            }
        } catch (e: Exception) {
        }
    }

    override fun handleDnsRequest(packetData: ByteArray?) {
        val parsedPacket: IpPacket = try {
            IpSelector.newPacket(packetData, 0, packetData!!.size) as IpPacket
        } catch (e: Exception) {
            Log.i(TAG, "handleDnsRequest: Discarding invalid IP packet", e)
            return
        }
        if (parsedPacket.payload !is UdpPacket) {
            try {
            } catch (ignored: Exception) {
            }
            return
        }
        var destAddr: InetAddress? = parsedPacket.header.dstAddr ?: return
        val dnsServer: AbstractDnsServer
        try {
            dnsServer = service.dnsServers?.get(destAddr?.hostAddress)!!
            destAddr = InetAddress.getByName(dnsServer.hostAddress)
        } catch (e: Exception) {
            return
        }
        val parsedUdp: UdpPacket = parsedPacket.getPayload() as UdpPacket
        if (parsedUdp.payload == null) {
            Log.i(
                TAG,
                "handleDnsRequest: Sending UDP packet without payload: $parsedUdp"
            )

            // Let's be nice to Firefox. Firefox uses an empty UDP packet to
            // the gateway to reduce the RTT. For further details, please see
            // https://bugzilla.mozilla.org/show_bug.cgi?id=888268
            val outPacket = DatagramPacket(ByteArray(0), 0, 0, destAddr, dnsServer.port)
//            forwardPacket(outPacket, null, dnsServer)
            return
        }
        val dnsRawData: ByteArray = parsedUdp.payload.rawData
        val dnsMsg: DnsMessage
        try {
            dnsMsg = DnsMessage(dnsRawData)
        } catch (e: IOException) {
            Log.i(TAG, "handleDnsRequest: Discarding non-DNS or invalid packet", e)
            return
        }
        if (dnsMsg.getQuestion() == null) {
            return
        }

    }


//    @Throws(DaedalusVpnService.VpnNetworkException::class)
    open fun forwardPacket(
        outPacket: DatagramPacket,
        parsedPacket: IpPacket?,
        dnsServer: AbstractDnsServer?
    ) {
        val dnsSocket: DatagramSocket
        try {
            // Packets to be sent to the real DNS server will need to be protected from the VPN
            dnsSocket = DatagramSocket()
            service.protect(dnsSocket)
            dnsSocket.send(outPacket)
            if (parsedPacket != null) {
                dnsIn.add(WaitingOnSocketPacket(dnsSocket, parsedPacket))
            } else {
                dnsSocket.close()
            }
        } catch (e: IOException) {
            handleDnsResponse(parsedPacket!!, outPacket.data)
            Logger.warning("DNSProvider: Could not send packet to upstream, forwarding packet directly")
        }
    }

    private fun handleRawDnsResponse(parsedPacket: IpPacket?, dnsSocket: DatagramSocket) {
        try {
            val datagramData = ByteArray(1024)
            val replyPacket = DatagramPacket(datagramData, datagramData.size)
            dnsSocket.receive(replyPacket)
            handleDnsResponse(parsedPacket!!, datagramData)
        } catch (e: Exception) {
        }
    }

    /**
     * Handles a DNS request, by either blocking it or forwarding it to the remote location.
     *
     * @param packetData The packet data to read
     * @throws DaedalusVpnService.VpnNetworkException If some network error occurred
     */
//    @Throws(DaedalusVpnService.VpnNetworkException::class)
//    protected fun handleDnsRequest(packetData: ByteArray) {
//        val parsedPacket: IpPacket
//        parsedPacket = try {
//            IpSelector.newPacket(packetData, 0, packetData.size) as IpPacket
//        } catch (e: Exception) {
//            Log.i(TAG, "handleDnsRequest: Discarding invalid IP packet", e)
//            return
//        }
//        if (parsedPacket.getPayload() !is UdpPacket) {
//            try {
//                Logger.debug("handleDnsRequest: Discarding unknown packet type " + parsedPacket.getPayload())
//            } catch (ignored: Exception) {
//            }
//            return
//        }
//        var destAddr: InetAddress? = parsedPacket.getHeader().getDstAddr()
//        if (destAddr == null) {
//            return
//        }
//        val dnsServer: AbstractDnsServer
//        try {
//            dnsServer = service.dnsServers.get(destAddr.hostAddress)
//            destAddr = InetAddress.getByName(dnsServer.hostAddress)
//        } catch (e: Exception) {
//            Logger.logException(e)
//            Logger.error("handleDnsRequest: DNS server alias query failed for " + destAddr.hostAddress)
//            return
//        }
//        val parsedUdp: UdpPacket = parsedPacket.getPayload() as UdpPacket
//        if (parsedUdp.getPayload() == null) {
//            Log.i(
//                TAG,
//                "handleDnsRequest: Sending UDP packet without payload: $parsedUdp"
//            )
//
//            // Let's be nice to Firefox. Firefox uses an empty UDP packet to
//            // the gateway to reduce the RTT. For further details, please see
//            // https://bugzilla.mozilla.org/show_bug.cgi?id=888268
//            val outPacket = DatagramPacket(ByteArray(0), 0, 0, destAddr, dnsServer.port)
//            forwardPacket(outPacket, null, dnsServer)
//            return
//        }
//        val dnsRawData: ByteArray = parsedUdp.getPayload().getRawData()
//        val dnsMsg: DnsMessage
//        try {
//            dnsMsg = DnsMessage(dnsRawData)
//            if (Daedalus.getPrefs()!!.getBoolean("settings_debug_output", false)) {
//                Logger.debug("DnsRequest: " + dnsMsg.toString())
//            }
//        } catch (e: IOException) {
//            Log.i(TAG, "handleDnsRequest: Discarding non-DNS or invalid packet", e)
//            return
//        }
//        if (dnsMsg.getQuestion() == null) {
//            Logger.debug("handleDnsRequest: Discarding DNS packet with no query $dnsMsg")
//            return
//        }
//        if (!resolve(parsedPacket, dnsMsg)) {
//            val outPacket =
//                DatagramPacket(dnsRawData, 0, dnsRawData.size, destAddr, dnsServer.port)
//            forwardPacket(outPacket, parsedPacket, dnsServer)
//        }
//    }

    /**
     * Helper class holding a socket, the packet we are waiting the answer for, and a time
     */
    private class WaitingOnSocketPacket(
        val socket: DatagramSocket,
        packet: IpPacket?
    ) {
        val packet: IpPacket? = packet
        private val time: Long = System.currentTimeMillis()
        fun ageSeconds(): Long {
            return (System.currentTimeMillis() - time) / 1000
        }

    }

    /**
     * Queue of WaitingOnSocketPacket, bound on time and space.
     */
    private class WospList : Iterable<WaitingOnSocketPacket?> {
        private val list = LinkedList<WaitingOnSocketPacket>()
        fun add(wosp: WaitingOnSocketPacket) {
            if (list.size > 1024) {
                Log.d(TAG, "Dropping socket due to space constraints: " + list.element().socket)
                list.element().socket.close()
                list.remove()
            }
            while (!list.isEmpty() && list.element().ageSeconds() > 10) {
                Log.d(TAG, "Timeout on socket " + list.element().socket)
                list.element().socket.close()
                list.remove()
            }
            list.add(wosp)
        }

        override fun iterator(): MutableIterator<WaitingOnSocketPacket> {
            return list.iterator()
        }

        fun size(): Int {
            return list.size
        }
    }

    companion object {
        private const val TAG = "UdpProvider"
    }
}
