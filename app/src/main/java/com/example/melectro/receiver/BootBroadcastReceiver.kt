package com.example.melectro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.melectro.Daedalus
import com.example.melectro.utill.Logger


class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Daedalus.getPrefs()!!.getBoolean("settings_boot", false)) {
            Daedalus.activateService(context, true)
            Logger.info("Triggered boot receiver")
        }
    }
}
