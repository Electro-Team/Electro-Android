package com.example.melectro.activity.login


data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Int? = null
)