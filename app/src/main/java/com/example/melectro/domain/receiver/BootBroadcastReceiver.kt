package com.example.melectro.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.melectro.utill.Logger
import com.example.melectro.presentation.App


class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (App.getPrefs()!!.getBoolean("settings_boot", false)) {
            App.activateService(context, true)
            Logger.info("Triggered boot receiver")
        }
    }
}
