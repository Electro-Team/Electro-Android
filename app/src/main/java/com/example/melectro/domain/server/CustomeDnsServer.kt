package com.example.melectro.domain.server

import com.example.melectro.domain.server.AbstractDnsServer
import com.example.melectro.presentation.App


class CustomDnsServer(name: String, address: String, port: Int) : AbstractDnsServer(address, port) {
    var name: String? = null
    var id: String? = null

    init {
        this.name = name
        this.id = java.lang.String.valueOf(App.configurations?.nextDnsId)
    }

}