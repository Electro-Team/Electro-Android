package com.example.melectro

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class AppCompat: AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!, "en"));
    }
}