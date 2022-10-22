package com.example.melectro.domain.provider

import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.melectro.domain.service.DaedalusVpnService
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.minidns.dnsmessage.DnsMessage
import org.pcap4j.packet.IpPacket
import java.io.IOException
import java.lang.Exception

class HttpsIetfProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    HttpsProvider(descriptor, service) {
    private val HTTP_CLIENT: OkHttpClient = getHttpClient("application/dns-message")
    override fun sendRequestToServer(parsedPacket: IpPacket?, message: DnsMessage?, uri: String?) {
        whqList.add(object : WaitingHttpsRequest(parsedPacket!!) {
            override fun doRequest() {
                val id: Int = message!!.id
                val rawRequest: ByteArray = message.toArray()
                val request: Request = Request.Builder()
                    .url(
                        (HTTPS_SUFFIX + uri).toHttpUrl().newBuilder()
                            .addQueryParameter(
                                "dns", Base64.encodeToString(
                                    message.asBuilder().setId(0).build().toArray(),
                                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                                )
                            )
                            .build()
                    )
                    .get()
                    .build()
                HTTP_CLIENT.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        result = rawRequest
                        completed = true
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            try {
                                result = DnsMessage(response.body?.bytes()).asBuilder()
                                    .setId(id).build().toArray()
                                completed = true
                            } catch (ignored: Exception) { //throw IllegalArgumentException when response is not correct
                            }
                        }
                    }
                })
            }
        })
    }
}
