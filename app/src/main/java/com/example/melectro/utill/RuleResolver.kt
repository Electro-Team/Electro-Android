package com.example.melectro.utill

import org.minidns.record.Record
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.util.HashMap

class RuleResolver : Runnable {
    private fun load() {
        try {
            status = STATUS_LOADING
            rulesA = HashMap()
            rulesAAAA = HashMap()
            if (mode == MODE_HOSTS) {
                for (hostsFile in hostsFiles) {
                    val file = File(hostsFile)
                    if (file.canRead()) {
                        Logger.info("Loading hosts from $file")
                        val stream = FileInputStream(file)
                        val dataIO = BufferedReader(InputStreamReader(stream))
                        var strLine: String
                        var data: Array<String>
                        var count = 0
                        while (dataIO.readLine().also { strLine = it } != null) {
                            if (strLine != "" && !strLine.startsWith("#")) {
                                data = strLine.split("\\s+").toTypedArray()
                                if (strLine.contains(":")) { //IPv6
                                    rulesAAAA[data[1]] = data[0]
                                } else if (strLine.contains(".")) { //IPv4
                                    rulesA[data[1]] = data[0]
                                }
                                count++
                            }
                        }
                        dataIO.close()
                        stream.close()
                        Logger.info("Loaded $count rules")
                    }
                }
            } else if (mode == MODE_DNSMASQ) {
                for (dnsmasqFile in dnsmasqFiles) {
                    val file = File(dnsmasqFile)
                    if (file.canRead()) {
                        Logger.info("Loading DNSMasq configuration from $file")
                        val stream = FileInputStream(file)
                        val dataIO = BufferedReader(InputStreamReader(stream))
                        var strLine: String
                        var data: Array<String>
                        var count = 0
                        while (dataIO.readLine().also { strLine = it } != null) {
                            if (strLine != "" && !strLine.startsWith("#")) {
                                data = strLine.split("/").toTypedArray()
                                if (data.size == 3 && data[0] == "address=") {
                                    if (data[1].startsWith(".")) {
                                        data[1] = data[1].substring(1, data[1].length)
                                    }
                                    if (strLine.contains(":")) { //IPv6
                                        rulesAAAA[data[1]] = data[2]
                                    } else if (strLine.contains(".")) { //IPv4
                                        rulesA[data[1]] = data[2]
                                    }
                                    count++
                                }
                            }
                        }
                        dataIO.close()
                        stream.close()
                        Logger.info("Loaded $count rules")
                    }
                }
            }
            status = STATUS_LOADED
        } catch (e: Exception) {
            Logger.logException(e)
            status = STATUS_NOT_LOADED
        }
    }

    override fun run() {
        try {
            while (!shutdown) {
                if (status == STATUS_PENDING_LOAD) {
                    load()
                }
                Thread.sleep(100)
            }
        } catch (e: Exception) {
            Logger.logException(e)
        }
    }

    companion object {
        const val STATUS_LOADED = 0
        const val STATUS_LOADING = 1
        const val STATUS_NOT_LOADED = 2
        const val STATUS_PENDING_LOAD = 3
        const val MODE_HOSTS = 0
        const val MODE_DNSMASQ = 1
        private var status = STATUS_NOT_LOADED
        private var mode = MODE_HOSTS
        private var hostsFiles: Array<String?> = arrayOfNulls(0)
        private var dnsmasqFiles: Array<String?> = arrayOfNulls(0)
        private var rulesA = HashMap<String, String>()
        private var rulesAAAA = HashMap<String, String>()
        private var shutdown = false
        fun shutdown() {
            shutdown = true
        }


        fun clear() {
            rulesA = HashMap()
            rulesAAAA = HashMap()
        }

        fun resolve(hostname: String, type: Record.TYPE): String? {
            val rules: HashMap<String, String> = if (type === Record.TYPE.A) {
                rulesA
            } else if (type === Record.TYPE.AAAA) {
                rulesAAAA
            } else {
                return null
            }
            if (rules.size == 0) {
                return null
            }
            if (rules.containsKey(hostname)) {
                return rules[hostname]
            }
            if (mode == MODE_DNSMASQ) {
                val pieces = hostname.split("\\.").toTypedArray()
                var builder: StringBuilder
                for (i in 1 until  pieces.size) {
                    builder = StringBuilder()
                    for (j in i until pieces.size) {
                        builder.append(pieces[j])
                        if (j < pieces.size - 1) {
                            builder.append(".")
                        }
                    }
                    if (rules.containsKey(builder.toString())) {
                        return rules[builder.toString()]
                    }
                }
            }
            return null
        }
    }

    init {
        status = STATUS_NOT_LOADED
        hostsFiles = arrayOfNulls(0)
        dnsmasqFiles = arrayOfNulls(0)
        shutdown = false
    }
}