package com.example.melectro

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.example.melectro.utill.DnsServersDetector

class DisableIPv6 {

    fun checkIPv6(context: Context) {
        val a: Array<String?>? = DnsServersDetector.getServers(context)

        print("**********" + a.toString())
        for (i in a!!)
            Log.d("Errorr22r","${i}")

    }

    fun APNProtocol(context: Context) {
        val intent = Intent(Settings.ACTION_APN_SETTINGS)

        context.startActivity(intent)
    }


}


