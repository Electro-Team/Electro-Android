package com.example.melectro.utill

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.lang.Exception
import java.net.InetAddress
import java.util.ArrayList
import java.util.HashSet


object DnsServersDetector {
    //https://stackoverflow.com/a/48973823
    private const val METHOD_EXEC_PROP_DELIM = "]: ["
    fun getServers(context: Context): Array<String?>? {
        var result: Array<String?>? = serversMethodSystemProperties
        if (result != null && result.isNotEmpty()) {
            return result
        }
        result = getServersMethodConnectivityManager(context)
        if (result != null && result.isNotEmpty()) {
            return result
        }
        result = serversMethodExec
        return if (result != null && result.isNotEmpty()) {
            result
        } else null
    }

    private fun getServersMethodConnectivityManager(context: Context): Array<String?>? {
        val priorityServersArrayList = ArrayList<String?>()
        val serversArrayList = ArrayList<String?>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (network in connectivityManager.allNetworks) {
            val networkInfo = connectivityManager.getNetworkInfo(network)
            if (networkInfo!!.isConnected) {
                val linkProperties = connectivityManager.getLinkProperties(network)
                val dnsServersList = linkProperties!!.dnsServers
                if (linkPropertiesHasDefaultRoute(linkProperties)) {
                    for (element in dnsServersList) {
                        val dnsHost = element.hostAddress
                        priorityServersArrayList.add(dnsHost)
                    }
                } else {
                    for (element in dnsServersList) {
                        val dnsHost = element.hostAddress
                        serversArrayList.add(dnsHost)
                    }
                }
            }
        }
        if (priorityServersArrayList.isEmpty()) {
            priorityServersArrayList.addAll(serversArrayList)
        }
        return if (priorityServersArrayList.size > 0) {
            priorityServersArrayList.toTypedArray()
        } else null
    }

    private val serversMethodSystemProperties: Array<String?>?
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val re1 = "^\\d+(\\.\\d+){3}$"
                val re2 = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$"
                val serversArrayList = ArrayList<String?>()
                try {
                    val SystemProperties = Class.forName("android.os.SystemProperties")
                    val method = SystemProperties.getMethod(
                        "get", *arrayOf<Class<*>>(
                            String::class.java
                        )
                    )
                    val netdns = arrayOf("net.dns1", "net.dns2", "net.dns3", "net.dns4")
                    for (dns in netdns) {
                        val args = arrayOf<Any>(dns)
                        val v = method.invoke(null, *args) as String
                        if ((v.matches(re1.toRegex()) || v.matches(re2.toRegex())) && !serversArrayList.contains(
                                v
                            )
                        ) {
                            serversArrayList.add(v)
                        }
                    }
                    if (serversArrayList.size > 0) {
                        return serversArrayList.toTypedArray()
                    }
                } catch (ex: Exception) {
                    com.example.melectro.utill.Logger.logException(ex)
                }
            }
            return null
        }
    private val serversMethodExec: Array<String?>?
        get() {
            try {
                val process: Process = Runtime.getRuntime().exec("getprop")
                val inputStream: InputStream = process.inputStream
//
                val lineNumberReader: LineNumberReader =
                    LineNumberReader(InputStreamReader(inputStream))
                val serversSet: Set<String?> = methodExecParseProps(lineNumberReader)
                Logger.error("OMgg***_____ ${inputStream}")
                if (serversSet.isNotEmpty()) {
                    return serversSet.toTypedArray()
                }
            } catch (ex: Exception) {

                com.example.melectro.utill.Logger.logException(ex)
            }
            return null
        }


    private fun methodExecParseProps(lineNumberReader: BufferedReader?): Set<String?> {
        var line: String=""
        val serversSet = HashSet<String?>()
        while (lineNumberReader?.readLine().also {
                if (it != null) {
                    line = it
                }
            } != null) {
            val split = line.indexOf(METHOD_EXEC_PROP_DELIM)
            if (split == -1) {
                continue
            }
            val property = line.substring(1, split)
            val valueStart = split + METHOD_EXEC_PROP_DELIM.length
            val valueEnd = line.length - 1
            if (valueEnd < valueStart) {
                continue
            }
            var value = line.substring(valueStart, valueEnd)
            if (value.isEmpty()) {
                continue
            }
            if (property.endsWith(".dns") || property.endsWith(".dns1") || property.endsWith(".dns2") ||
                property.endsWith(".dns3") || property.endsWith(".dns4")
            ) {
                val ip = InetAddress.getByName(value) ?: continue
                value = ip.hostAddress
                if (value == null || value.length == 0) {
                    continue
                }
                serversSet.add(value)
            }
        }
        return serversSet
    }

    private fun linkPropertiesHasDefaultRoute(linkProperties: LinkProperties?): Boolean {
        for (route in linkProperties!!.routes) {
            if (route.isDefaultRoute) {
                return true
            }
        }
        return false
    }
}
