package com.example.melectro.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.melectro.Daedalus
import com.example.melectro.activity.MainActivity
import com.example.melectro.utill.Logger
import java.lang.Exception

class StatusBarBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION) {
            Daedalus.deactivateService(context)
        }
        if (intent.action == STATUS_BAR_BTN_SETTINGS_CLICK_ACTION) {
            val settingsIntent: Intent = Intent(context, MainActivity::class.java)

            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(settingsIntent)
            try {
                @SuppressLint("WrongConstant") val statusBarManager =
                    context.getSystemService("statusbar")
                val collapse = statusBarManager.javaClass.getMethod("collapsePanels")
                collapse.invoke(statusBarManager)
            } catch (e: Exception) {
                Logger.logException(e)
            }
        }
    }

    companion object {
        var STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION =
            "org.itxtech.daedalus.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION"
        var STATUS_BAR_BTN_SETTINGS_CLICK_ACTION =
            "org.itxtech.daedalus.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION"
    }
}
