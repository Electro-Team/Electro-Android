package com.example.melectro.utill


import android.util.Log
import com.example.melectro.Daedalus
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


object Logger {
    private var buffer: StringBuffer? = null
    fun init() {
        if (buffer != null) {
            buffer!!.setLength(0)
        } else {
            buffer = StringBuffer()
        }
    }

    fun shutdown() {
        buffer = null
    }

    val log: String
        get() = buffer.toString()

    fun error(message: String) {
        send("[ERROR] $message")
    }

    fun warning(message: String) {
        send("[WARNING] $message")
    }

    fun info(message: String) {
        send("[INFO] $message")
    }

    fun debug(message: String) {
        send("[DEBUG] $message")
    }

    fun logException(e: Throwable) {
        error(getExceptionMessage(e))
    }

    fun getExceptionMessage(e: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        return stringWriter.toString()
    }

    private val logSizeLimit: Int
        private get() = Daedalus.getPrefs()?.getString("settings_log_size", "10000")!!.toInt()

    private fun checkBufferSize(): Boolean {
        val limit = logSizeLimit
        if (limit == 0) { //DISABLED!
            return false
        }
        if (limit == -1) { //N0 limit
            return true
        }
        if (buffer!!.length > limit) { //LET's clean it up!
            buffer!!.setLength(limit)
        }
        return true
    }

    private fun send(message: String) {
        try {
            if (checkBufferSize()) {
                val fileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(Date())
                buffer!!.insert(0, "\n").insert(0, message).insert(0, fileDateFormat)
            }
            Log.d("Daedalus", message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
