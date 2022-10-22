package com.example.melectro.presentation.updateScreen

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.example.melectro.databinding.ActivityUpdateBinding
import com.example.melectro.presentation.App

class UpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.downloadButton.setOnClickListener {
            App.openUri(intent.getStringExtra("updateDownloadURL").toString())
        }
        binding.explainTextView.text = intent.getStringExtra("updatemessage").toString()
    }
}