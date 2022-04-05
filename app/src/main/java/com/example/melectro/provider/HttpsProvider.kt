package com.example.melectro.provider

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import com.example.melectro.service.DaedalusVpnService
import com.example.melectro.utill.Logger
import okhttp3.OkHttpClient
import okhttp3.internal.and
import org.minidns.dnsmessage.DnsMessage
import org.pcap4j.packet.IpPacket
import org.pcap4j.packet.IpSelector
import org.pcap4j.packet.UdpPacket
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.or

abstract class HttpsProvider internal constructor(
    descriptor: ParcelFileDescriptor?,
    service: DaedalusVpnService?
) :
    Provider(descriptor, service!!) {
    val whqList = WhqList()
    fun getHttpClient(accept: String?): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", accept!!)
                        .build()) }

            .build()

    }

    override fun process() {
        try {
            val pipes = Os.pipe()
            mInterruptFd = pipes[0]
            mBlockFd = pipes[1]
            val inputStream = FileInputStream(descriptor?.fileDescriptor)
            val outputStream = FileOutputStream(descriptor?.fileDescriptor)
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
                val polls = arrayOfNulls<StructPollfd>(2)
                polls[0] = deviceFd
                polls[1] = blockFd
                Os.poll(polls, 100)
                if (blockFd.revents.toInt() != 0) {
                    Log.i(TAG, "Told to stop VPN")
                    running = false
                    return
                }
                val iterator = whqList.iterator()
                while (iterator.hasNext()) {
                    val request = iterator.next()
                    if (request.completed) {
                        handleDnsResponse(request.packet, request.result)
                        iterator.remove()
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
            Logger.logException(e)
        }
    }

    override fun handleDnsRequest(packetData: ByteArray?) {
        val parsedPacket: IpPacket = try {
            IpSelector.newPacket(packetData, 0, packetData!!.size) as IpPacket
        } catch (e: Exception) {
            return
        }
        if (parsedPacket.payload !is UdpPacket
        ) {
            return
        }
        val destAddr: InetAddress = parsedPacket.header.dstAddr ?: return
        val uri: String = try {
            service.dnsServers?.get(destAddr.hostAddress!!)!!.address //https uri
        } catch (e: Exception) {
            Logger.logException(e)
            return
        }
        val parsedUdp: UdpPacket = parsedPacket.payload as UdpPacket
        if (parsedUdp.payload == null) {
            return
        }
        val dnsRawData: ByteArray = parsedUdp.payload.rawData
        val dnsMsg: DnsMessage
        try {
            dnsMsg = DnsMessage(dnsRawData)
            Logger.debug("DnsRequest: $dnsMsg")
        } catch (e: IOException) {
            return
        }
        if (dnsMsg.question == null) {
            Logger.debug("handleDnsRequest: Discarding DNS packet with no query $dnsMsg")
            return
        }
        if (!resolve(parsedPacket, dnsMsg)) {
            sendRequestToServer(parsedPacket, dnsMsg, uri)
            //SHOULD use a DNS ID of 0 in every DNS request (according to draft-ietf-doh-dns-over-https-11)
        }
    }

    protected abstract fun sendRequestToServer(
        parsedPacket: IpPacket?,
        message: DnsMessage?,
        uri: String?
    )

    //uri example: 1.1.1.1:1234/dnsQuery. The specified provider will add https:// and parameters
    abstract class WaitingHttpsRequest(packet: IpPacket) {
        var completed = false
        lateinit var result: ByteArray
        val packet: IpPacket = packet
        abstract fun doRequest()

    }

    class WhqList : Iterable<WaitingHttpsRequest?> {
        private val list = LinkedList<WaitingHttpsRequest>()
        fun add(request: WaitingHttpsRequest) {
            list.add(request)
            request.doRequest()
        }

        override fun iterator(): MutableIterator<WaitingHttpsRequest> {
            return list.iterator()
        }
    }

    companion object {
        const val HTTPS_SUFFIX = "https://"
        private const val TAG = "HttpsProvider"
    }
}
