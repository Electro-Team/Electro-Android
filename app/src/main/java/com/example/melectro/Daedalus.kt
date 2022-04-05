package com.example.melectro

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.VpnService
import android.os.Build
import androidx.preference.PreferenceManager
import com.example.melectro.activity.MainActivity
import com.example.melectro.server.AbstractDnsServer
import com.example.melectro.server.DnsServer
import com.example.melectro.server.DnsServerHelper
import com.example.melectro.service.DaedalusVpnService
import com.example.melectro.utill.Configurations
import com.example.melectro.utill.RuleResolver
import java.io.File
import com.example.melectro.utill.Logger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.acra.ACRA.log
import kotlin.collections.ArrayList


class Daedalus : Application() {
    private var prefs: SharedPreferences? = null
    private var mResolver: Thread? = null
    override fun onCreate() {
        super.onCreate()
        instance = this
        Logger.init()
        mResolver = Thread(RuleResolver())
        mResolver!!.start()
        initData()
    }

    private fun initDirectory(dir: String?) {
        val directory = File(dir)
        if (!directory.isDirectory) {
            Logger.warning(dir + " is not a directory. Delete result: " + directory.delete())
        }
        if (!directory.exists()) {
            Logger.debug(dir + " does not exist. Create result: " + directory.mkdirs())
        }
    }

    private fun initData() {
        PreferenceManager.setDefaultValues(this, R.xml.perf_setting, false)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (getExternalFilesDir(null) != null) {
            rulePath = getExternalFilesDir(null)!!.path + "/rules/"
            logPath = getExternalFilesDir(null)!!.path + "/logs/"
            configPath = getExternalFilesDir(null)!!.path + "/config.json"
            initDirectory(rulePath)
            initDirectory(logPath)
        }
        if (configPath != null) {
            configurations = Configurations.load(File(configPath))
        } else {
            configurations = Configurations()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
        prefs = null
        RuleResolver.shutdown()
        mResolver!!.interrupt()
        RuleResolver.clear()
        mResolver = null
        Logger.shutdown()
    }

    companion object {
        private const val SHORTCUT_ID_ACTIVATE = "shortcut_activate"
        val DNS_SERVERS: MutableList<DnsServer> = ArrayList<DnsServer>()
        var configurations: Configurations? = null
        var rulePath: String? = null
        var logPath: String? = null
        private var configPath: String? = null
        var instance: Daedalus? = null
            private set

        @Throws(JsonParseException::class)
        fun <T> parseJson(beanClass: Class<T>?, reader: com.google.gson.stream.JsonReader): T {
            val builder = GsonBuilder()
            val gson: Gson = builder.create()
            return gson.fromJson(reader, beanClass)
        }


        fun getPrefs(): SharedPreferences? {
            return instance!!.prefs
        }


        fun getServiceIntent(context: Context?): Intent {
            return Intent(context, DaedalusVpnService::class.java)
        }

        fun switchService(): Boolean {
            return if (DaedalusVpnService.isActivated) {
                deactivateService(instance)
                false
            } else {
                prepareAndActivateService(instance)
                true
            }
        }

        fun prepareAndActivateService(context: Context?): Boolean {
            val intent = VpnService.prepare(context)
            return if (intent != null) {
                false
            } else {
                activateService(context)
                true
            }
        }

        @JvmOverloads
        fun activateService(context: Context?, forceForeground: Boolean = false) {
            if (DNS_SERVERS.size != 2) {
                val sharedPreferences = context?.getSharedPreferences("pref", Context.MODE_PRIVATE)
                val dns1 = sharedPreferences?.getString("Dns1", "185.231.182.126")
                val dns2 = sharedPreferences?.getString("Dns2", "37.152.182.112")
                log.d("verssion", "$dns1")
                DNS_SERVERS.add(DnsServer(dns1, R.string.server_twnic_primary))
                DNS_SERVERS.add(DnsServer(dns2, R.string.server_twnic_secondary))
            }
            DaedalusVpnService.primaryServer =
                DnsServerHelper.getServerById(DnsServerHelper.primary)
                    .clone() as AbstractDnsServer
            DaedalusVpnService.secondaryServer =
                DnsServerHelper.getServerById(DnsServerHelper.secondary)
                    .clone() as AbstractDnsServer
            if ((instance!!.prefs!!.getBoolean("settings_foreground", false) || forceForeground)
                && Build.VERSION.SDK_INT > Build.VERSION_CODES.O
            ) {
                Logger.info("Starting foreground service")
                context!!.startForegroundService(
                    getServiceIntent(context).setAction(
                        DaedalusVpnService.ACTION_ACTIVATE
                    )
                )
            } else {
                Logger.info("Starting background service")
                context!!.startService(getServiceIntent(context).setAction(DaedalusVpnService.ACTION_ACTIVATE))
            }
        }

        fun deactivateService(context: Context?) {
            context!!.startService(getServiceIntent(context).setAction(DaedalusVpnService.ACTION_DEACTIVATE))
            context.stopService(getServiceIntent(context))
        }

        fun updateShortcut(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Logger.info("Updating shortcut")
                val activate: Boolean = DaedalusVpnService.isActivated
                val notice =
                    if (activate) {
                        context.getString(R.string.button_text_deactivate)
                    } else {
                        context.getString(
                            R.string.button_text_activate
                        )
                    }
                val info = ShortcutInfo.Builder(context, SHORTCUT_ID_ACTIVATE)
                    .setLongLabel(notice)
                    .setShortLabel(notice)
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_electro_logo))
                    .setIntent(
                        Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW)

                    ).build()
                val shortcutManager = context.getSystemService(SHORTCUT_SERVICE) as ShortcutManager
                shortcutManager.addDynamicShortcuts(listOf(info))
            }
        }

        fun openUri(uri: String?) {
            try {
                instance!!.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Logger.logException(e)
            }
        }
    }
}