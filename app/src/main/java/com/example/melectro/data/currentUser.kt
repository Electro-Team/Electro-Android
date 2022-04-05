package com.example.melectro.data

import java.io.File


class currentUser(val userName : String , val email: String) {
    init {
        val fileName ="currentUser.txt"
        var file = File(fileName)
           file.writeText("${userName} ${email}")
    }




}