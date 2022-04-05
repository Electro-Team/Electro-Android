package com.example.melectro.data

import android.content.Context
import android.content.SharedPreferences
import com.example.melectro.data.model.LoggedInUser
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
//            currentUser(username,"${username}@gmail.com")
            val fakeUser = LoggedInUser(java.util.UUID.randomUUID().toString(), "${username}")
            return Result.Success(fakeUser)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("pref",Context.MODE_PRIVATE)
        val editor=sharedPreferences.edit()
        editor.putBoolean("isLoggedIn",false)
        editor.apply()
    }
}