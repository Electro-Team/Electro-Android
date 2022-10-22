package com.example.melectro.presentation.mainActivity

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.melectro.domain.server.DnsServer
import com.example.melectro.domain.service.DaedalusVpnService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.melectro.utill.DnsServersDetector
import com.example.melectro.*
import com.example.melectro.databinding.ActivityMainBinding
import com.example.melectro.presentation.App


class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
            private set

    }

    var activated = false
    var forcePopUp = false
    var adWebAddres: String ="?"
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkForIpv6()
        val preferences: SharedPreferences = getSharedPreferences("appData", Context.MODE_PRIVATE)
         adWebAddres = preferences.getString("adWebAddres", "?").toString()
        val popUpMessage: String = preferences.getString("popUpmessage", "?").toString()
         forcePopUp= preferences.getBoolean("forcePopUp", false)
        Log.d("fkers",forcePopUp.toString())
        val dns1 = preferences.getString("electroDNS1", "78.152.42.100").toString()
        val dns2 = preferences.getString("electroDNS2", "78.152.42.101").toString()
        App.DNS_SERVERS.add(DnsServer(dns1, R.string.server_twnic_primary))
        App.DNS_SERVERS.add(DnsServer(dns2, R.string.server_twnic_secondary))
        val adWebPhotoAddres: String = preferences.getString("adWebPhotoAddres", "?").toString()
        if (popUpMessage != "?") {
            val alertDialog = AlertDialog.Builder(this@MainActivity)
            alertDialog.setIcon(R.drawable.ic_baseline_signal_cellular_connected_no_internet_4_bar_24)
            alertDialog.setMessage(popUpMessage)
            alertDialog.setCancelable(true)
            alertDialog.setPositiveButton(
                R.string.okButton
            ) { dialog, id ->
                dialog.cancel()
            }
            val alert = alertDialog.create()
            alert.setTitle(R.string.popUpAnouncmentTitle)
            alert.show()
        }
        if (adWebAddres != "?") {
            binding.adBox.visibility = View.VISIBLE
            Glide.with(this).load(adWebPhotoAddres).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            }).into(binding.adBox)
        }
        val ourMessage: String = preferences.getString("mainPageMessage", "?").toString()
        if (ourMessage != "?") {
            binding.messageTextView.visibility = View.VISIBLE
            binding.messageTextView.text = ourMessage
        }
        binding.connectionButtonBg.setOnClickListener {

            if (DaedalusVpnService.isActivated || activated) {
                deAcvtivateService()
            } else {
                activateService()
            }
        }
        binding.supportUs.setOnClickListener {
            App.openUri("https://www.instagram.com/irelectro")
        }
        binding.websiteIcon.setOnClickListener {
            App.openUri("https://electrotm.org")
        }
        binding.aboutUs.setOnClickListener {
            App.openUri("https://discord.io/elteam")
        }
        binding.adBox.setOnClickListener {
            App.openUri(adWebAddres)
        }
        App.isActivatedLiveData.observe(this) {
            if (it) {
                activationUiAnimation()
            } else {
                deAvtivationUiAnimation()
            }
        }
    }
    fun checkForIpv6()
    {
        val dns = DnsServersDetector.getServers(this)
        if (dns != null) {
            for (i in dns) {
                if (i!!.contains(":")) {
                    Thread(Runnable {
                        this@MainActivity.runOnUiThread {
                            val alertDialog = AlertDialog.Builder(this@MainActivity)
                            alertDialog.setIcon(R.drawable.ic_baseline_signal_cellular_connected_no_internet_4_bar_24)
                            alertDialog.setMessage("برای استفاده بهتر باید IPv6 خود را غیر فعال کنید  \n  مراحل زیر را دنبال کنید :  \n 1_ بر روی دکمه آموزش بفشارید  \n 2_آموزش را دنبال کنید ")
                            alertDialog.setCancelable(true)
                            alertDialog.setPositiveButton(
                                R.string.amuzesh
                            ) { dialog, id ->
                                App.openUri("https://www.instagram.com/p/CacDgIYt6yx/?utm_medium=copy_link")
                            }
                            alertDialog.setNegativeButton(
                                R.string.TrollCancelText
                            ) { dialog, id ->
                                dialog.cancel()
                            }
                            val alert = alertDialog.create()
                            alert.setTitle(R.string.Ipv6AlertDialogTitle)
                            alert.show()
                        }
                    }).start()
                }
            }
        }
    }

    fun activationUiAnimation() {
        binding.electroLogo.setBackgroundResource(R.drawable.ic_disconnect)
        binding.lottieBgAnimation.setMinAndMaxProgress(0.405f, 0.731f)
        binding.lottieBgAnimation.playAnimation()
        binding.lottieLightAnimation.setMinAndMaxProgress(0.1818f, 0.5681f)
        binding.lottieLightAnimation.playAnimation()
        activated = true
    }

    fun deAvtivationUiAnimation() {
        binding.electroLogo.setBackgroundResource(R.drawable.ic_finger_print)
        binding.lottieBgAnimation.setMinAndMaxProgress(0.731f, 1f)
        binding.lottieBgAnimation.playAnimation()
        binding.lottieLightAnimation.setMinAndMaxProgress(0.5681f, 1f)
        binding.lottieLightAnimation.playAnimation()
    }

    override fun onResume() {
        super.onResume()
        if (DaedalusVpnService.isActivated) {
            activationUiAnimation()
        } else {
            deAvtivationUiAnimation()
        }
    }

    fun activateService() {
        val intent = VpnService.prepare(App.instance)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
        var activateCounter: Long = App.configurations!!.activateCounter
        if (activateCounter == -1L) {
            return
        }
        activateCounter++
        App.configurations?.activateCounter = activateCounter

    }

    fun deAcvtivateService() {
        App.deactivateService(this.applicationContext)
        activated = false
    }

    public override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        if (result == RESULT_OK) {
            App.activateService(App.instance)
            App.updateShortcut(applicationContext)
            if (forcePopUp && adWebAddres != "?"){
                App.openUri(adWebAddres)
            }
        } else {
            Toast.makeText(this, "اووف یه مشکلی هست مثل اینکه", Toast.LENGTH_LONG).show()
        }
        super.onActivityResult(request, result, data)
    }

}


