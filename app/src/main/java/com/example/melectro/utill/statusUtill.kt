package com.example.melectro.utill

import android.content.Context

object StatusUtils {
    // 1
    fun storeTutorialStatus(context: Context, show: Boolean) {
        val preferences = context.getSharedPreferences("showTutorial", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean("show", show)
        editor.apply()
    }

    // 2
    fun getTutorialStatus(context: Context): Boolean {
        val preferences = context.getSharedPreferences("showTutorial", Context.MODE_PRIVATE)
        return preferences.getBoolean("show", true)
    }
}
