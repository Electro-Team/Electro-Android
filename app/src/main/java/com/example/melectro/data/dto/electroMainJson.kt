package com.example.melectro.data.dto

data class ElectroJson(
    val adBoxPhotoURL: String,
    val adBoxWebURL: String,
    val dns: Dns,
    val mainpagemessage: String,
    val popupmessage: String,
    val updateDownloadURL: String,
    val updatemessage: String,
    val forcepopup: Boolean,
    val version: String
)

data class Dns(
    val dns1: String,
    val dns2: String
)