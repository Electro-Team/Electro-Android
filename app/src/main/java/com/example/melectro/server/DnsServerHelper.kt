package com.example.melectro.server


import android.net.Uri
import com.example.melectro.Daedalus
import com.example.melectro.utill.Logger
import com.example.melectro.provider.HttpsProvider
import com.example.melectro.provider.ProviderPicker
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
            !(Daedalus.getPrefs()?.getBoolean("settings_dont_build_cache", false))!!
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
        get() = checkServerId(
            Daedalus.getPrefs()?.getString("primary_server", "0")!!.toInt()
        ).toString()
    val secondary: String
        get() = checkServerId(
            Daedalus.getPrefs()?.getString("secondary_server", "1")!!.toInt()
        ).toString()

    private fun checkServerId(id: Int): Int {
        if (id < Daedalus.DNS_SERVERS.size) {
            return id
        }
        for (server in Daedalus.configurations?.customDNSServers!!) {
            if (server.id.equals(id.toString())) {
                return id
            }
        }
        return 0
    }

    fun getServerById(id: String?): AbstractDnsServer {
        for (server in Daedalus.DNS_SERVERS) {
            if (server.id.equals(id)) {
                return server
            }
        }
        for (customDNSServer in Daedalus.configurations?.customDNSServers!!) {
            if (customDNSServer.id.equals(id)) {
                return customDNSServer
            }
        }
        return Daedalus.DNS_SERVERS.get(0)
    }

}
