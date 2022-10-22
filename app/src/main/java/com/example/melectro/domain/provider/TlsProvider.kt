package com.example.melectro.domain.provider

import android.os.ParcelFileDescriptor
import com.example.melectro.domain.server.AbstractDnsServer
import com.example.melectro.domain.service.DaedalusVpnService
import org.pcap4j.packet.IpPacket
import java.io.DataOutputStream
import java.lang.Exception
import java.net.DatagramPacket
import java.net.Socket
import javax.net.ssl.SSLContext

class TlsProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    TcpProvider(descriptor, service) {
    override fun forwardPacket(
        outPacket: DatagramPacket,
        parsedPacket: IpPacket?,
        dnsServer: AbstractDnsServer?
    ) {
        val dnsSocket: Socket
        try {
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(null, null, null)
            dnsSocket = context.socketFactory.createSocket(outPacket.address, dnsServer!!.port)
            //Create TLS v1.2 socket
            //TODO: SNI
            service.protect(dnsSocket)
            val dos = DataOutputStream(dnsSocket.getOutputStream())
            val packet: ByteArray = processUdpPacket(outPacket, parsedPacket)
            dos.writeShort(packet.size)
            dos.write(packet)
            dos.flush()
            if (parsedPacket != null) {
                dnsIn.add(WaitingOnSocketPacket(dnsSocket, parsedPacket))
            } else {
                dnsSocket.close()
            }
        } catch (e: Exception) {
        }
    }
}