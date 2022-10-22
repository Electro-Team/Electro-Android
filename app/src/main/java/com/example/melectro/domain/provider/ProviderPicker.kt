package com.example.melectro.domain.provider

import android.os.ParcelFileDescriptor
import com.example.melectro.presentation.App
import com.example.melectro.domain.service.DaedalusVpnService

object ProviderPicker {
    const val DNS_QUERY_METHOD_UDP = 0
    const val DNS_QUERY_METHOD_TCP = 1
    const val DNS_QUERY_METHOD_TLS = 2
    const val DNS_QUERY_METHOD_HTTPS_IETF = 3
    const val DNS_QUERY_METHOD_HTTPS_JSON = 4

    //This section mush be the same as the one in arrays.xml
    fun getProvider(descriptor: ParcelFileDescriptor?, service: DaedalusVpnService?): Provider {
        when (dnsQueryMethod) {
            DNS_QUERY_METHOD_UDP -> return UdpProvider(descriptor, service)
            DNS_QUERY_METHOD_TCP -> return TcpProvider(descriptor, service)
            DNS_QUERY_METHOD_HTTPS_IETF -> return HttpsIetfProvider(descriptor, service)
            DNS_QUERY_METHOD_HTTPS_JSON -> return HttpsJsonProvider(descriptor, service)
            DNS_QUERY_METHOD_TLS -> return TlsProvider(descriptor, service)
        }
        return UdpProvider(descriptor, service)
    }

    val dnsQueryMethod: Int
        get() = App.getPrefs()?.getString("settings_dns_query_method", "0")!!.toInt()
}
