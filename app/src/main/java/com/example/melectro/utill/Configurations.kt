package com.example.melectro.utill


import com.example.melectro.domain.server.CustomDnsServer
import com.example.melectro.presentation.App
import java.io.File
import java.io.FileReader
import java.util.*


class Configurations {
    var customDNSServers: ArrayList<CustomDnsServer>? = null
        get() {
            if (field == null) {
                field = ArrayList<CustomDnsServer>()
            }
            return field
        }
        private set
    var appObjects: ArrayList<String>? = null
        get() {
            if (field == null) {
                field = ArrayList()
            }
            return field
        }
        private set
    private var totalDnsId = 0
    var activateCounter: Long = 0
    val nextDnsId: Int
        get() {
            if (totalDnsId < CUSTOM_ID_START) {
                totalDnsId = CUSTOM_ID_START
            }
            return totalDnsId++
        }

    companion object {
        private const val CUSTOM_ID_START = 32
        private var file: File? = null
        fun load(file: File): Configurations {
            Companion.file = file
            var config: Configurations? = null
            if (file.exists()) {
                try {
                    config =
                        App.parseJson(Configurations::class.java, com.google.gson.stream.JsonReader(FileReader(file)))
                    Logger.info("Load configuration successfully from $file")
                } catch (e: Exception) {
                    Logger.logException(e)
                }
            }
            if (config == null) {
                Logger.info("Load configuration failed. Generating default configurations.")
                config = Configurations()
            }
            return config
        }
    }
}
