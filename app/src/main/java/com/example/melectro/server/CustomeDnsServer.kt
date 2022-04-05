package com.example.melectro.server

import com.example.melectro.Daedalus


class CustomDnsServer(name: String, address: String, port: Int) : AbstractDnsServer(address, port) {
    var name: String? = null
    var id: String? = null

    init {
        this.name = name
        this.id = java.lang.String.valueOf(Daedalus.configurations?.nextDnsId)
    }

}