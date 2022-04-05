package com.example.melectro.activity

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.airbnb.lottie.LottieAnimationView
import com.example.melectro.Daedalus.Companion.DNS_SERVERS
import com.example.melectro.server.DnsServer
import com.example.melectro.service.DaedalusVpnService
import com.google.android.material.navigation.NavigationView
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.melectro.*
import com.example.melectro.LocaleHelper
import com.example.melectro.LocaleHelper.setLocale
import com.example.melectro.activity.login.LoginActivity
import com.example.melectro.activity.login.LoginViewModel
import com.example.melectro.activity.login.LoginViewModelFactory
import com.example.melectro.data.model.LoggedInUser
import com.example.melectro.utill.DnsServersDetector
import com.example.melectro.utill.Logger
import org.acra.ACRA.log


class MainActivity : AppCompat() {
    companion object {
        var UI_STATE = 0
        var instance: MainActivity? = null
            private set


        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    lateinit var bottomLinear: LinearLayout
    lateinit var logoImageView: ImageView
    lateinit var connectionBg: ImageView
    lateinit var blueShadow: ImageView
    lateinit var violetShadow: ImageView
    lateinit var lottielight: LottieAnimationView
    lateinit var goProBTN: Button
    lateinit var drawer: DrawerLayout
    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var navigationView: NavigationView
    lateinit var protocolEditText: TextView
    lateinit var lottieConnection: LottieAnimationView
    lateinit var openGateBTN: Button
    lateinit var proBusiness: TextView
    lateinit var webSiteIccn: ImageView
    lateinit var UsIccn: ImageView
    lateinit var SupportIccn: ImageView
    lateinit var connectionShadow: ImageView
    lateinit var versionNumber: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dns = DnsServersDetector.getServers(this)

        if (dns != null) {
            for (i in dns) {
                Logger.info("it start somethingg ${i}_______")
                if (i!!.contains(":")) {
                    Thread(Runnable {
                        this@MainActivity.runOnUiThread {
                            val alertDialog = AlertDialog.Builder(this@MainActivity)
                            alertDialog.setIcon(R.drawable.ic_baseline_signal_cellular_connected_no_internet_4_bar_24)
                            alertDialog.setMessage("برای استفاده بهتر باید IPv6 خود را غیر فعال کنید  \n  مراحل زیر را دنبال کنید :  \n 1_ بر روی دکمه آموزش بفشارید  \n 2_آموزش را دنبال کنید ")
                            alertDialog.setCancelable(true)
                            alertDialog.setPositiveButton(
                                "آموزش"
                            ) { dialog, id ->

                                Daedalus.openUri("https://www.instagram.com/p/CacDgIYt6yx/?utm_medium=copy_link")

                            }
                            alertDialog.setNegativeButton(
                                "نخوندم"
                            ) { dialog, id ->
                                dialog.cancel()
                            }
//                        alertDialog.show()
                            val alert = alertDialog.create()
                            // set title for alert dialog box
                            alert.setTitle("توجه")
                            // show alert dialog
                            alert.show()
                        }
                    }).start()


                }
            }
        } else {
            Logger.error("fuck MMMMMMEEEEE")
        }
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("pref", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
//        if (!isLoggedIn) {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
        initUI()
        versionSynkCheck()
        getDnsFromWeb()


        if (DaedalusVpnService.isActivated) {
            UI_STATE = 1
            updateUserUI(UI_STATE)
            UI_STATE = 2
            updateUserUI(UI_STATE)
        }
        connectionBg.setOnClickListener { v: View? ->

            if (UI_STATE == 0) {
                UI_STATE = 1
                updateUserUI(UI_STATE)
            } else if (UI_STATE == 1) {
                UI_STATE = 2
                updateUserUI(UI_STATE)
            } else if (UI_STATE == 2) {
                UI_STATE = 3
                updateUserUI(UI_STATE)
            } else if (UI_STATE == 3) {
                UI_STATE = 2
                updateUserUI(UI_STATE)
            }
        }
        webSiteIccn.setOnClickListener {
            Daedalus.openUri("http://elteam.ir")
        }
        UsIccn.setOnClickListener {
            Daedalus.openUri("https://discord.io/elteam")
        }
        SupportIccn.setOnClickListener {
            Daedalus.openUri("https://www.instagram.com/irelectro")

        }
//        navigationView.setNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.englishLanguage -> {
//                    updateViews("en")
//                    val intent = Intent(this, MainActivity::class.java)
//                    finish()
//                    startActivity(intent)
//                }
//                R.id.persianLanguage -> {
//                    updateViews("ira")
//                    val intent = Intent(this, MainActivity::class.java)
//                    finish()
//                    startActivity(intent)
//                }
//                R.id.logOut -> {
//                    lateinit var loginViewModel: LoginViewModel
//                    loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
//                        .get(LoginViewModel::class.java)
//                    loginViewModel.logout(this)
//                    val intent = Intent(this, LoginActivity::class.java)
//                    finish()
//                    startActivity(intent)
//                }
//                R.id.releaseNote -> {
//                    Daedalus.openUri("http://elcdn.ir/app/mobile/notes/release/rn.txt")
//                }
//            }
//            UI_STATE = 0
//
//            true
//        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.drawer_menu, menu)
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("pref", Context.MODE_PRIVATE)
        val userName: String? = sharedPreferences.getString("UserName", "AmirHossin Taghipour")
        val userEmail: String? = sharedPreferences.getString("UserEmail", "A.H.T.P2015@Gmail.com")
        val UserNameEditText = findViewById<TextView>(R.id.userNameEditText)
        val UserEmailEditText = findViewById<TextView>(R.id.emailEditText)
//        UserEmailEditText.text = userEmail!!
//        UserNameEditText.text = userName!!
        return super.onCreateOptionsMenu(menu)
    }

    private fun updateViews(languageCode: String) {
        setLocale(this, languageCode)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }


    private fun initUI() {
        instance = this
        lottieConnection = findViewById(R.id.lottieBgAnimation)
        bottomLinear = findViewById(R.id.linearLayout)
        logoImageView = findViewById(R.id.electroLogo)
        connectionBg = findViewById(R.id.connectionButtonBg)
        goProBTN = findViewById(R.id.goProBTN)
        blueShadow = findViewById(R.id.blueShadow)
        violetShadow = findViewById(R.id.violetShadow)
        lottielight = findViewById(R.id.lottieLightAnimation)
        protocolEditText = findViewById(R.id.protocolTextView)
        openGateBTN = findViewById(R.id.openGateBTN)
        proBusiness = findViewById(R.id.pro_business)
        bottomLinear.visibility = View.GONE
        logoImageView.setBackgroundResource(R.drawable.ic_electro_logo)
        webSiteIccn = findViewById(R.id.websiteIcon)
        UsIccn = findViewById(R.id.aboutUs)
        SupportIccn = findViewById(R.id.supportUs)
        connectionShadow = findViewById(R.id.connectionShadow)
        drawer = findViewById(R.id.main_drawer_layout)
        toolbar = findViewById(R.id.toolbar)
//        navigationView = findViewById(R.id.nav_view)



        setSupportActionBar(toolbar)
//        navigationView.bringToFront()
//        val toggle = ActionBarDrawerToggle(
//            this,
//            drawer,
//            toolbar,
//            R.string.navigation_drawer_open,
//            R.string.navigation_drawer_close
//        )
//        drawer.addDrawerListener(toggle)
//        toggle.syncState()
    }

    override fun onResume() {
        versionSynkCheck()

        super.onResume()
    }

    private fun getDnsFromWeb() {
        object : Thread() {
            override fun run() {
                val path = "http://elcdn.ir/app/mobile/etc/dns.txt"
                var ur: URL? = null
                val sharedPreferences = getSharedPreferences("pref", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                try {
                    ur = URL(path)
                    val c: HttpURLConnection = ur.openConnection() as HttpURLConnection
                    c.requestMethod = "GET"
                    c.connect()
                    val `in` = c.inputStream
                    val br = BufferedReader(InputStreamReader(`in`))
                    var strLine: String?
                    var name: Int = R.string.server_twnic_primary
                    br.readLine().also { strLine = it }
                    DNS_SERVERS.add(DnsServer(strLine, name))
                    editor.putString("Dns1", strLine)
                    name = R.string.server_twnic_secondary
                    br.readLine().also { strLine = it }
                    editor.putString("Dns2", strLine)
                    DNS_SERVERS.add(DnsServer(strLine, name))
                    editor.apply()
                    br.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: ProtocolException) {

                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun versionSynkCheck() {
        object : Thread() {
            override fun run() {
                versionNumber = BuildConfig.VERSION_NAME
                val path = "http://elcdn.ir/app/mobile/version/version.txt"
                var u: URL? = null
                try {
                    u = URL(path)
                    val c: HttpURLConnection = u.openConnection() as HttpURLConnection
                    c.requestMethod = "GET"
                    c.connect()
                    val `in` = c.inputStream
                    val br = BufferedReader(InputStreamReader(`in`))
                    var strLine: String?
                    br.readLine().also { strLine = it }
                    val updateVersion1: String = strLine!!
                    if (updateVersion1 != versionNumber) {
                        runOnUiThread {
                            val alertDialog = AlertDialog.Builder(this@MainActivity)
                            alertDialog.setIcon(R.drawable.ic_baseline_signal_cellular_connected_no_internet_4_bar_24)
                            alertDialog.setMessage("لطفا آخرین ورژن نرم افزار را از صفحه باز شده دانلود و نصب نمایید.")
                            alertDialog.setCancelable(false)
                            alertDialog.setPositiveButton(
                                "OK"
                            ) { dialog, id ->
//                                br.readLine().also { strLine = it }
                                Daedalus.openUri("http://lalejinwcp.com/elcdn/app/android/Electro.apk")
                            }
//                        alertDialog.show()
                            val alert = alertDialog.create()
                            // set title for alert dialog box
                            alert.setTitle("Need Update")
                            // show alert dialog
                            alert.show()
                        }


                    }
                    br.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: ProtocolException) {
                    e.printStackTrace()
                } catch (e: IOException) {

                    e.printStackTrace()
                }
            }
        }.start()
    }

    override fun onRestart() {
        super.onRestart()
        if (DaedalusVpnService.isActivated) {
            UI_STATE = 1
            updateUserUI(UI_STATE)
            UI_STATE = 2
            updateUserUI(UI_STATE)
        } else {
            UI_STATE = 1
            updateUserUI(UI_STATE)
        }

    }

     fun updateUserUI(uiState: Int) {
        val fadeIn: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeIn2: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOut: Animation =
            AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
        val move: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.move)
        if (uiState == 0) {
            bottomLinear.visibility = View.GONE
            logoImageView.setBackgroundResource(R.drawable.ic_electro_logo)

        } else if (uiState == 1) {

            bottomLinear.startAnimation(fadeIn)
            logoImageView.startAnimation(fadeIn2)
            logoImageView.setBackgroundResource(R.drawable.ic_finger_print)
            bottomLinear.visibility = View.VISIBLE
            connectionBg.startAnimation(move)
            logoImageView.startAnimation(move)
            connectionShadow.startAnimation(move)
            goProBTN.startAnimation(fadeIn)
            goProBTN.visibility = View.VISIBLE
            blueShadow.startAnimation(move)
            violetShadow.startAnimation(move)
            lottieConnection.startAnimation(move)
            lottieConnection.setMinAndMaxProgress(0.0f, 0.405f)
            lottieConnection.playAnimation()
            lottielight.setMinAndMaxProgress(0.0f, 0.1818f)
            lottielight.playAnimation()

        } else if (uiState == 2) {
            logoImageView.setBackgroundResource(R.drawable.ic_disconnect)
            activateService()
            lottieConnection.setMinAndMaxProgress(0.405f, 0.731f)
            lottieConnection.playAnimation()
            lottielight.setMinAndMaxProgress(0.1818f, 0.5681f)
            lottielight.playAnimation()
        } else if (uiState == 3) {
            logoImageView.setBackgroundResource(R.drawable.ic_finger_print)
            if (DaedalusVpnService.isActivated) {
                Daedalus.deactivateService(this.applicationContext)
            }
            lottieConnection.setMinAndMaxProgress(0.731f, 1f)
            lottieConnection.playAnimation()
            lottielight.setMinAndMaxProgress(0.5681f, 1f)
            lottielight.playAnimation()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }


    fun activateService() {
        val intent = VpnService.prepare(Daedalus.instance)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
        var activateCounter: Long = Daedalus.configurations!!.activateCounter
        if (activateCounter == -1L) {
            return
        }
        activateCounter++
        Daedalus.configurations?.activateCounter = activateCounter
    }

    public override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        if (result == RESULT_OK) {
            Daedalus.activateService(Daedalus.instance)
            Daedalus.updateShortcut(applicationContext)
        }
        super.onActivityResult(request, result, data)
    }


}


