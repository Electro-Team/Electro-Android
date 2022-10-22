package com.example.melectro.domain.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.example.melectro.domain.provider.Provider
import com.example.melectro.domain.provider.ProviderPicker
import com.example.melectro.domain.server.AbstractDnsServer
import com.example.melectro.utill.Logger
import com.example.melectro.domain.receiver.StatusBarBroadcastReceiver
import com.example.melectro.domain.server.DnsServerHelper
import com.example.melectro.utill.DnsServersDetector
import com.example.melectro.R
import com.example.melectro.presentation.App
import com.example.melectro.presentation.mainActivity.MainActivity
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.HashMap


class DaedalusVpnService : VpnService(), Runnable {
    private var notification: NotificationCompat.Builder? = null
    private var running = false
    private var lastUpdate: Long = 0
    private var statisticQuery = false
    private var provider: Provider? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var mThread: Thread? = null
    var dnsServers: HashMap<String, AbstractDnsServer?>? = null


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_ACTIVATE -> {
                isActivated = true
                if (true) {
                    val manager =
                        this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    val builder: NotificationCompat.Builder =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(
                                CHANNEL_ID,
                                CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_HIGH
                            )
                            manager.createNotificationChannel(channel)
                            NotificationCompat.Builder(this, CHANNEL_ID)
                        } else {
                            NotificationCompat.Builder(this)
                        }
                    val deactivateIntent =
                        Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION)
                    deactivateIntent.setClass(this, StatusBarBroadcastReceiver::class.java)
                    val settingsIntent =
                        Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION)
                    settingsIntent.setClass(this, StatusBarBroadcastReceiver::class.java)
                    val pIntent = PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setWhen(0)
                        .setContentTitle(resources.getString(R.string.notice_activated))
                        .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                        .setSmallIcon(R.drawable.electro_icon)
                        .setColor(resources.getColor(R.color.ElectrocolorPrimary)) //backward compatibility
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setTicker(resources.getString(R.string.notice_activated))
                        .setContentIntent(pIntent)
                        .addAction(
                            R.drawable.ic_clear,
                            resources.getString(R.string.button_text_deactivate),
                            PendingIntent.getBroadcast(
                                this, 0,
                                deactivateIntent, PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    val notification = builder.build()
                    manager.notify(NOTIFICATION_ACTIVATED, notification)
                    this.notification = builder
                }
                startThread()
                App.updateShortcut(applicationContext)
                if (MainActivity.instance != null) {
                    MainActivity.instance?.startActivity(
                        Intent(
                            applicationContext,
                            MainActivity::class.java
                        )

                    )
                }
                return START_STICKY
            }
            ACTION_DEACTIVATE -> {
                stopThread()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startThread() {
        if (mThread == null) {
            mThread = Thread(this, "DaedalusVpn")
            running = true
            mThread!!.start()
        }
    }

    override fun onDestroy() {
        stopThread()
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }
    }

    private fun stopThread() {
        isActivated = false
        var shouldRefresh = false
        try {
            if (descriptor != null) {
                descriptor!!.close()
                descriptor = null
            }
            if (mThread != null) {
                running = false
                shouldRefresh = true
                if (provider != null) {
                    provider!!.shutdown()
                    mThread!!.interrupt()
                    provider!!.stop()
                } else {
                    mThread!!.interrupt()
                }
                mThread = null
            }
            if (notification != null) {
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ACTIVATED)
                notification = null
            }
            dnsServers = null
        } catch (e: Exception) {
            Logger.logException(e)
        }
        stopSelf()
        if (shouldRefresh) {
            DnsServerHelper.clearCache()
            Logger.info("Daedalus VPN service has stopped")
        }
        if (shouldRefresh && MainActivity.instance != null) {
            MainActivity.instance!!.startActivity(
                Intent(applicationContext, MainActivity::class.java)
            )
        } else if (shouldRefresh) {
            App.updateShortcut(applicationContext)
        }
    }

    override fun onRevoke() {
        stopThread()
    }

    @Throws(UnknownHostException::class)
    private fun addDnsServer(
        builder: Builder,
        format: String?,
        ipv6Template: ByteArray?,
        addr: AbstractDnsServer?
    ): InetAddress? {
        Log.d("uselesses","______________217")
        var size = dnsServers!!.size
        size++
        if (addr?.address!!.contains("/")) { //https uri
            val alias = String.format(format!!, size + 1)
            dnsServers!![alias] = addr
            builder.addRoute(alias, 32)
            Log.d("uselesses","______________224")
            return InetAddress.getByName(alias)
        }
        val address = InetAddress.getByName(addr.address)
        if (address is Inet6Address && ipv6Template == null) {
            Log.i(
                TAG,
                "addDnsServer: Ignoring DNS server $address"
            )
            Log.d("uselesses","______________233")
        } else if (address is Inet4Address) {
            val alias = String.format(format!!, size + 1)
            addr.hostAddress = address.getHostAddress()
            dnsServers!![alias] = addr
            builder.addRoute(alias, 32)
            return InetAddress.getByName(alias)
        } else if (address is Inet6Address) {
            ipv6Template!![ipv6Template.size - 1] = (size + 1).toByte()
            val i6addr = Inet6Address.getByAddress(ipv6Template)
            addr.hostAddress = address.getHostAddress()
            dnsServers!![i6addr.hostAddress!!] = addr
            return i6addr
        }
        return null
    }

    override fun run() {
        Log.d("uselesses","______________251")
        try {
            DnsServerHelper.buildCache()
            val builder: Builder = Builder()
                .setSession("Daedalus")
                .setConfigureIntent(
                    PendingIntent.getActivity(
                        this, 0,
                        Intent(
                            this,
                            MainActivity::class.java
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            if (App.getPrefs()!!.getBoolean("settings_app_filter_switch", false)) {
                Log.d("uselesses","______________267")
                val apps: ArrayList<String> = App.configurations?.appObjects!!
                if (apps.size > 0) {
                    val mode: Boolean =
                        App.getPrefs()!!.getBoolean("settings_app_filter_mode_switch", false)
                    for (app in apps) {
                        try {
                            if (mode) {
                                builder.addDisallowedApplication(app)
                            } else {
                                builder.addAllowedApplication(app)
                            }
                            Logger.debug("Added app to list: $app")
                        } catch (e: PackageManager.NameNotFoundException) {
                            Logger.error("Package Not Found:$app")
                        }
                    }
                }
            }
            var format: String? = null
            for (prefix in arrayOf("10.0.0", "192.0.2", "198.51.100", "203.0.113", "192.168.50")) {
                try {
                    builder.addAddress("$prefix.1", 24)
                } catch (e: IllegalArgumentException) {
                    continue
                }
                format = "$prefix.%d"
                break
            }
            val advanced: Boolean =
                App.getPrefs()!!.getBoolean("settings_advanced_switch", false)
            statisticQuery = App.getPrefs()!!.getBoolean("settings_count_query_times", false)
            Log.d("uselesses","______________299")
            var ipv6Template: ByteArray? = byteArrayOf(
                32, 1, 13,
                (184 and 0xFF).toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            try {
                val addr = Inet6Address.getByAddress(ipv6Template)
                Log.d(
                    TAG,
                    "configure: Adding IPv6 address$addr"
                )
                builder.addAddress(addr, 120)
            } catch (e: Exception) {
                Logger.logException(e)
                ipv6Template = null
            }
            if (advanced) {
                dnsServers = HashMap<String, AbstractDnsServer?>()
                aliasPrimary = addDnsServer(builder, format, ipv6Template, primaryServer)
                aliasSecondary = addDnsServer(builder, format, ipv6Template, secondaryServer)
            } else {
                aliasPrimary = InetAddress.getByName(primaryServer?.address)
                aliasSecondary = InetAddress.getByName(secondaryServer?.address)
            }
            Logger.info(
                "Daedalus VPN service is listening on " + primaryServer?.address
                    .toString() + " as " + aliasPrimary!!.hostAddress
            )
            Logger.info(
                "Daedalus VPN service is listening on " + secondaryServer?.address
                    .toString() + " as " + aliasSecondary!!.hostAddress
            )
            builder.addDnsServer(aliasPrimary!!).addDnsServer(
                aliasSecondary!!
            )
            if (advanced) {
                builder.setBlocking(true)
                builder.allowFamily(OsConstants.AF_INET)
                builder.allowFamily(OsConstants.AF_INET6)
            }
            descriptor = builder.establish()
            Logger.info("Daedalus VPN service is started")
            if (advanced) {
                provider = ProviderPicker.getProvider(descriptor, this)
                provider?.start()
                provider?.process()
            } else {
                while (running) {
                    Thread.sleep(1000)
                }
            }
        } catch (ignored: InterruptedException) {
        } catch (e: Exception) {
            MainActivity.instance?.runOnUiThread {
                AlertDialog.Builder(MainActivity.instance!!)
                    .setTitle(R.string.errorOccurred)
                    .setMessage(Logger.getExceptionMessage(e))
                    .setPositiveButton(
                        R.string.appply,
                        DialogInterface.OnClickListener { d: DialogInterface?, id: Int -> })
                    .show()
            }
            Logger.logException(e)
        } finally {
            stopThread()
        }
    }

    fun providerLoopCallback() {
        if (statisticQuery) {
            updateUserInterface()
        }
    }

    private fun updateUserInterface() {
        Log.d("uselesses","______________376")
        val time = System.currentTimeMillis()
        if (time - lastUpdate >= 1000) {
            lastUpdate = time
            if (notification != null) {
                notification!!.setContentTitle(resources.getString(R.string.notice_queries) + " " + provider?.dnsQueryTimes)
                val manager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ACTIVATED, notification!!.build())
            }
        }
    }

    class VpnNetworkException : Exception {
        constructor(s: String?) : super(s) {}
        constructor(s: String?, t: Throwable?) : super(s, t) {}
    }

    companion object {
        const val ACTION_ACTIVATE =
            "org.itxtech.daedalus.service.DaedalusVpnService.ACTION_ACTIVATE"
        const val ACTION_DEACTIVATE =
            "org.itxtech.daedalus.service.DaedalusVpnService.ACTION_DEACTIVATE"
        private const val NOTIFICATION_ACTIVATED = 0
        private const val TAG = "DaedalusVpnService"
        private const val CHANNEL_ID = "daedalus_channel_1"
        private const val CHANNEL_NAME = "daedalus_channel"
        var primaryServer: AbstractDnsServer? = null
        var secondaryServer: AbstractDnsServer? = null
        private var aliasPrimary: InetAddress? = null
        private var aliasSecondary: InetAddress? = null
        var isActivated = false
            private set
        private var receiver: BroadcastReceiver? = null
        private fun updateUpstreamServers(context: Context) {
            val servers = DnsServersDetector.getServers(context)
            if (servers != null) {
                if (servers.size >= 2 && (aliasPrimary == null || aliasPrimary!!.hostAddress != servers[0]) &&
                    (aliasSecondary == null || aliasSecondary!!.hostAddress != servers[0]) &&
                    (aliasPrimary == null || aliasPrimary!!.hostAddress != servers[1]) &&
                    (aliasSecondary == null || aliasSecondary!!.hostAddress != servers[1])
                ) {
                    primaryServer?.address = servers[0]!!
                    primaryServer?.port = AbstractDnsServer.DNS_SERVER_DEFAULT_PORT
                    secondaryServer?.address = servers[1]!!
                    secondaryServer?.port = AbstractDnsServer.DNS_SERVER_DEFAULT_PORT
                } else if ((aliasPrimary == null || aliasPrimary!!.hostAddress != servers[0]) &&
                    (aliasSecondary == null || aliasSecondary!!.hostAddress != servers[0])
                ) {
                    primaryServer?.address = servers[0]!!
                    primaryServer?.port = AbstractDnsServer.DNS_SERVER_DEFAULT_PORT
                    secondaryServer?.address = servers[0]!!
                    secondaryServer?.port = AbstractDnsServer.DNS_SERVER_DEFAULT_PORT
                } else {
                    val buf = StringBuilder()
                    for (server in servers) {
                        buf.append(server).append(" ")
                    }
                    Logger.error("Invalid upstream DNS $buf")
                }
                Logger.info(
                    "Upstream DNS updated: " + primaryServer?.address
                        .toString() + " " + secondaryServer?.address
                )
            } else {
                Logger.error("Cannot obtain upstream DNS server!")
            }
        }
    }
}
