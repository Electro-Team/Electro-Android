package com.example.melectro.server

import android.content.Context
import com.example.melectro.Daedalus


class DnsServer @JvmOverloads constructor(
    address: String?,
    description: Int = 0,
    port: Int = DNS_SERVER_DEFAULT_PORT
) :
    AbstractDnsServer(address!!, port) {
    val id: String = totalId++.toString()
    private val description: Int = description
    private fun getStringDescription(context: Context): String {
        return context.resources.getString(description)
    }

     val name: String
        get() = getStringDescription(Daedalus())

    companion object {
        private var totalId = 0
    }

}
