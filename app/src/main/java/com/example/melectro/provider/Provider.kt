package com.example.melectro.provider

import android.os.ParcelFileDescriptor
import android.system.Os
import com.example.melectro.service.DaedalusVpnService
import com.example.melectro.utill.Logger
import com.example.melectro.utill.RuleResolver
import org.minidns.dnsmessage.DnsMessage
import org.minidns.record.A
import org.minidns.record.AAAA
import org.minidns.record.Record
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.*
import org.pcap4j.packet.*

abstract class Provider internal constructor(
    protected var descriptor: ParcelFileDescriptor?,
    protected val service: DaedalusVpnService
) {
    protected var running = false
    protected var mBlockFd: FileDescriptor? = null
    protected var mInterruptFd: FileDescriptor? = null
    protected val deviceWrites: Queue<ByteArray> = LinkedList()
    val dnsQueryTimes: Long
        get() = Companion.dnsQueryTimes

    abstract fun process()
    fun start() {
        running = true
    }

    fun shutdown() {
        running = false
    }

    fun stop() {
        try {
            if (mInterruptFd != null) {
                Os.close(mInterruptFd)
            }
            if (mBlockFd != null) {
                Os.close(mBlockFd)
            }
            if (descriptor != null) {
                descriptor!!.close()
                descriptor = null
            }
        } catch (ignored: Exception) {
        }
    }

    private fun queueDeviceWrite(ipOutPacket: IpPacket) {
        Companion.dnsQueryTimes++
        deviceWrites.add(ipOutPacket.rawData)
    }

    fun resolve(parsedPacket: IpPacket, dnsMsg: DnsMessage): Boolean {
        val dnsQueryName: String = dnsMsg.question.name.toString()
        try {
            val response = RuleResolver.resolve(dnsQueryName, dnsMsg.question.type)
            if (response != null && dnsMsg.question.type === Record.TYPE.A) {
                Logger.info("Provider: Resolved $dnsQueryName  Local resolver response: $response")
                val builder: DnsMessage.Builder = dnsMsg.asBuilder()
                    .setQrFlag(true)
                    .addAnswer(
                        Record(
                            dnsQueryName, Record.TYPE.A, 1, 64,
                            A(Inet4Address.getByName(response).address)
                        )
                    )
                handleDnsResponse(parsedPacket, builder.build().toArray())
                return true
            } else if (response != null && dnsMsg.question.type === Record.TYPE.AAAA) {
                Logger.info("Provider: Resolved $dnsQueryName  Local resolver response: $response")
                val builder: DnsMessage.Builder = dnsMsg.asBuilder()
                    .setQrFlag(true)
                    .addAnswer(
                        Record(
                            dnsQueryName, Record.TYPE.AAAA, 1, 64,
                            AAAA(Inet6Address.getByName(response).address)
                        )
                    )
                handleDnsResponse(parsedPacket, builder.build().toArray())
                return true
            }
        } catch (e: Exception) {
            Logger.logException(e)
        }
        return false
    }

    /**
     * Handles a responsePayload from an upstream DNS server
     *
     * @param requestPacket   The original request packet
     * @param responsePayload The payload of the response
     */
    fun handleDnsResponse(requestPacket: IpPacket, responsePayload: ByteArray?) {
        try {
            Logger.debug("DnsResponse: " + DnsMessage(responsePayload).toString())
        } catch (e: IOException) {
            Logger.logException(e)
        }
        val udpOutPacket: UdpPacket = requestPacket.payload as UdpPacket
        val payLoadBuilder: UdpPacket.Builder = UdpPacket.Builder(udpOutPacket)
            .srcPort(udpOutPacket.header.dstPort)
            .dstPort(udpOutPacket.header.srcPort)
            .srcAddr(requestPacket.header.dstAddr)
            .dstAddr(requestPacket.header.srcAddr)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)
            .payloadBuilder(
                UnknownPacket.Builder()
                    .rawData(responsePayload)
            )
        val ipOutPacket: IpPacket = if (requestPacket is IpV4Packet) {
            IpV4Packet.Builder(requestPacket as IpV4Packet)
                .srcAddr(requestPacket.header.dstAddr as Inet4Address)
                .dstAddr(requestPacket.header.srcAddr as Inet4Address)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(payLoadBuilder)
                .build()
        } else {
            IpV6Packet.Builder(requestPacket as IpV6Packet)
                .srcAddr(requestPacket.header.dstAddr as Inet6Address)
                .dstAddr(requestPacket.header.srcAddr as Inet6Address)
                .correctLengthAtBuild(true)
                .payloadBuilder(payLoadBuilder)
                .build()
        }
        queueDeviceWrite(ipOutPacket)
    }

    @Throws(DaedalusVpnService.VpnNetworkException::class)
    protected fun writeToDevice(outFd: FileOutputStream) {
        try {
            outFd.write(deviceWrites.poll())
        } catch (e: IOException) {
            throw DaedalusVpnService.VpnNetworkException("Outgoing VPN output stream closed")
        }
    }

    @Throws(DaedalusVpnService.VpnNetworkException::class)
    protected fun readPacketFromDevice(inputStream: FileInputStream, packet: ByteArray?) {
        // Read the outgoing packet from the input stream.
        val length: Int = try {
            inputStream.read(packet)
        } catch (e: IOException) {
            throw DaedalusVpnService.VpnNetworkException("Cannot read from device", e)
        }
        if (length == 0) {
            return
        }
        val readPacket = (packet!!).copyOfRange(0, length)
        handleDnsRequest(readPacket)
    }

    @Throws(DaedalusVpnService.VpnNetworkException::class)
    protected abstract fun handleDnsRequest(packetData: ByteArray?)

    companion object {
        protected var dnsQueryTimes: Long = 0
    }

    init {
        Companion.dnsQueryTimes = 0
    }
}
