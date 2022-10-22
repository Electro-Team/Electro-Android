package com.example.melectro.presentation.splash

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.melectro.BuildConfig
import com.example.melectro.data.dto.ElectroJson

import com.example.melectro.databinding.ActivitySplashScreenBinding
import com.example.melectro.presentation.mainActivity.MainActivity
import com.example.melectro.presentation.updateScreen.UpdateActivity
import com.google.gson.Gson

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isFullscreen = true
        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent
        fullscreenContent.setOnClickListener { toggle() }
        //Body

            Handler().postDelayed({
                getDataFromUrl()
            }, 700)


    }

    private fun getDataFromUrl() {
        val queue = Volley.newRequestQueue(this)
        val request =
            StringRequest(Request.Method.GET, "https://elcdn.ir/beta/electro.json", { response ->
                val responseJson: ElectroJson =
                    Gson().fromJson(response.toString(), ElectroJson::class.java)
                val appVersion: Boolean = getAppVersion(responseJson.version)
                if (appVersion) {

                    val intent = Intent(this, UpdateActivity::class.java)
                    intent.putExtra("updateDownloadURL", responseJson.updateDownloadURL)
                    intent.putExtra("updatemessage", responseJson.updatemessage)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("adWebAddres", responseJson.adBoxWebURL)
                    intent.putExtra("adWebPhotoAddres", responseJson.adBoxPhotoURL)
                    intent.putExtra("mainPageMessage", responseJson.mainpagemessage)
                    intent.putExtra("popUpmessage", responseJson.popupmessage)
                    intent.putExtra("forcePopUp", responseJson.forcepopup)
                    intent.putExtra("electroDNS1", responseJson.dns.dns1)
                    intent.putExtra("electroDNS2", responseJson.dns.dns2)
                    putDnsInSharedprefrences(responseJson)
                    startActivity(intent)
                    finish()
                }


            }, {
                val intent = Intent(this, MainActivity::class.java)
                Log.d("elecRes", "error___$it")
                intent.putExtra("electroDNS1", "78.152.42.100")
                intent.putExtra("electroDNS2", "78.152.42.101")
                startActivity(intent)
                finish()

            })
        queue.add(request)
    }

    private fun getAppVersion(version: String): Boolean {
        val appVersion = BuildConfig.VERSION_NAME.split('.')
        val lastVersion = version.split('.')
        Log.d("elecRes", "error___$appVersion")
        Log.d("elecRes", "error___$lastVersion")
        val firstIndex = lastVersion[0].toInt() > appVersion[0].toInt()
        val secondIndex = lastVersion[1].toInt() > appVersion[1].toInt()
        val thirthIndex = lastVersion[2].toInt() > appVersion[2].toInt()
        val fourthIndex = lastVersion[3].toInt() > appVersion[3].toInt()
        return firstIndex || secondIndex || thirthIndex || fourthIndex
    }

    fun putDnsInSharedprefrences(responseJson: ElectroJson) {
        val dns1 = responseJson.dns.dns1
        val dns2 = responseJson.dns.dns2
        val sharedPreferences = getSharedPreferences("appData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("electroDNS1", dns1)
        editor.putString("electroDNS2", dns2)
        editor.putString("mainPageMessage", responseJson.mainpagemessage)
        editor.putString("adWebPhotoAddres", responseJson.adBoxPhotoURL)
        editor.putString("adWebAddres", responseJson.adBoxWebURL)
        editor.putString("popUpmessage", responseJson.popupmessage)
        editor.putBoolean("forcePopUp", responseJson.forcepopup)
        editor.apply()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
//        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }
}