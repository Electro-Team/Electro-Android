package com.example.melectro.domain.server


import android.net.Uri
import com.example.melectro.domain.provider.HttpsProvider
import com.example.melectro.domain.provider.ProviderPicker
import com.example.melectro.utill.Logger
import com.example.melectro.presentation.App
import java.lang.Exception
import java.net.InetAddress
import java.util.*


object DnsServerHelper {
    var domainCache = HashMap<String?, List<InetAddress>>()
    fun clearCache() {
        domainCache = HashMap()
    }

    fun buildCache() {
        domainCache = HashMap()
        if (ProviderPicker.dnsQueryMethod >= ProviderPicker.DNS_QUERY_METHOD_HTTPS_IETF &&
            !(App.getPrefs()?.getBoolean("settings_dont_build_cache", false))!!
        ) {
            buildDomainCache(getServerById(primary).address)
            buildDomainCache(getServerById(secondary).address)
        }
    }

    private fun buildDomainCache(addr: String) {
        var addr = addr
        addr = HttpsProvider.HTTPS_SUFFIX.toString() + addr
        val host = Uri.parse(addr).host
        try {
            domainCache[host] = Arrays.asList(*InetAddress.getAllByName(host))
        } catch (e: Exception) {
            Logger.logException(e)
        }
    }



    val primary: String
        get() = "0"

    val secondary: String
        get() ="1"

    private fun checkServerId(id: Int): Int {
        if (id < App.DNS_SERVERS.size) {
            return id
        }
        for (server in App.configurations?.customDNSServers!!) {
            if (server.id.equals(id.toString())) {
                return id
            }
        }
        return 0
    }

    fun getServerById(id: String?): AbstractDnsServer {
        for (server in App.DNS_SERVERS) {
            if (server.id == id) {
                return server
            }
        }
        for (customDNSServer in App.configurations?.customDNSServers!!) {
            if (customDNSServer.id.equals(id)) {
                return customDNSServer
            }
        }
        return App.DNS_SERVERS.get(0)
    }

}
