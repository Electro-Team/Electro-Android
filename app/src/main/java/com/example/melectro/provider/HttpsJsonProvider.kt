package com.example.melectro.provider

import android.os.ParcelFileDescriptor
import com.example.melectro.service.DaedalusVpnService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.minidns.dnsmessage.DnsMessage
import org.minidns.dnsname.DnsName
import org.minidns.record.*
import org.pcap4j.packet.IpPacket
import java.io.IOException
import java.lang.Exception
import org.minidns.record.A
import org.minidns.record.MX
import org.minidns.record.SOA
import org.minidns.record.CNAME
import org.minidns.record.AAAA
import org.minidns.record.Record

class HttpsJsonProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?) :
    HttpsProvider(descriptor, service) {
    private val HTTP_CLIENT: OkHttpClient = getHttpClient("application/dns-json")
    override fun sendRequestToServer(parsedPacket: IpPacket?, message: DnsMessage?, uri: String?) {
        whqList.add(object : WaitingHttpsRequest(parsedPacket!!) {
           override fun doRequest() {
                val rawRequest: ByteArray = message!!.toArray()

                val request: Request = Request.Builder()
                    .url(
                        (HTTPS_SUFFIX+uri).toHttpUrl().newBuilder()
                            .addQueryParameter("name", message.question?.name.toString())
                            .addQueryParameter("type", message.question?.type?.name)
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
                                val jsonObject: JsonObject =
                                    JsonParser().parse(response.body?.string()).asJsonObject
                                val msg: DnsMessage.Builder = message.asBuilder()
                                    .setRecursionDesired(jsonObject.get("RD").asBoolean)
                                    .setRecursionAvailable(jsonObject.get("RA").asBoolean)
                                    .setAuthenticData(jsonObject.get("AD").asBoolean)
                                    .setCheckingDisabled(jsonObject.get("CD").asBoolean)
                                if (jsonObject.has("Answer")) {
                                    val answers: JsonArray =
                                        jsonObject.get("Answer").getAsJsonArray()
                                    for (answer in answers) {
                                        val ans: JsonObject = answer.asJsonObject
                                        val type: Record.TYPE =
                                            Record.TYPE.getType(ans.get("type").asInt)
                                        val data = ans.get("data").asString
                                        var recordData: Data? = null
                                        when (type) {
                                            Record.TYPE.A -> recordData = A(data)
                                            Record.TYPE.AAAA -> recordData = AAAA(data)
                                            Record.TYPE.CNAME -> recordData = CNAME(data)
                                            Record.TYPE.MX -> recordData = MX(5, data)
                                            Record.TYPE.SOA -> {
                                                val sections = data.split(" ").toTypedArray()
                                                if (sections.size == 7) {
                                                    recordData = SOA(
                                                        sections[0],
                                                        sections[1],
                                                        java.lang.Long.valueOf(sections[2]),
                                                        Integer.valueOf(
                                                            sections[3]
                                                        ),
                                                        Integer.valueOf(sections[4]),
                                                        Integer.valueOf(
                                                            sections[5]
                                                        ),
                                                        java.lang.Long.valueOf(sections[6])
                                                    )
                                                }
                                            }
                                            Record.TYPE.DNAME -> recordData = DNAME(data)
                                            Record.TYPE.NS -> recordData = NS(DnsName.from(data))
                                            Record.TYPE.TXT -> recordData = TXT(data.toByteArray())


                                        }
                                        if (recordData != null) {
                                            msg.addAnswer(
                                                Record(
                                                    ans.get("name").getAsString(),
                                                    type, 1,
                                                    ans.get("TTL").getAsLong(),
                                                    recordData
                                                )
                                            )
                                        }
                                    }
                                }
                                result = msg.setQrFlag(true).build().toArray()
                                completed = true
                            } catch (ignored: Exception) { //throw com.google.gson.JsonSyntaxException when response is not correct
                            }
                        }
                    }
                })
            }
        })
    }
}
