package com.example.melectro.domain.server

import java.lang.Exception


open class AbstractDnsServer(var address: String, var port: Int) : Cloneable{
    var hostAddress: String? = null

    public override fun clone(): Any {
        try {
            return super.clone()
        } catch (ignored: Exception) {
        }
        return AbstractDnsServer("", 0)
    }

    companion object {
        const val DNS_SERVER_DEFAULT_PORT = 53
    }
}