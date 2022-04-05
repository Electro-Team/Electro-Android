package com.example.melectro.activity.signUp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.melectro.activity.MainActivity
import com.example.melectro.activity.login.LoginActivity
import com.example.melectro.data.currentUser
import com.example.melectro.databinding.ActivitySignUpBinding
import com.example.melectro.utill.Logger.log


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val email = binding.signupEmailTextinput
        val passWord = binding.signupPasswordTextinput
        val userName = binding.signupUsernameTextinput
        val createBTN = binding.signUpCreateAccountBTN
        val goToLogin = binding.signUpLoginBTN

        goToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        createBTN.setOnClickListener {
            currentUser(userName.editText?.text.toString(), email.editText?.text.toString())
            Log.d("deb", "pressing")

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userEmail", email.editText?.text.toString())
            intent.putExtra("userName", userName.editText?.text.toString())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

    }

}
