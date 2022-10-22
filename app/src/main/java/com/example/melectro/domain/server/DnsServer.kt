package com.example.melectro.domain.server

import android.content.Context
import com.example.melectro.presentation.App


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
        get() = getStringDescription(App())

    companion object {
        private var totalId = 0
    }

}
