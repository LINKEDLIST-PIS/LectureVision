package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.son.lecture_project.databinding.ActivityLoginBinding
import com.son.lecture_project.ui.auth.LoginViewModel
import com.son.lecture_project.ui.home.Result

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        setupClickListeners()
        observeLoginResult()
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editLoginEmail.text.toString().trim()
            val password = binding.editLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.lowercase().endsWith("@gnu.ac.kr")) {
                Toast.makeText(this, "GNU 이메일만 사용 가능합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.login(email, password)
        }

        binding.textSignupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun observeLoginResult() {
        loginViewModel.loginResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.buttonLogin.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.buttonLogin.isEnabled = true
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    // TODO: The received token (result.data.accessToken) should be saved securely.
                    startActivity(Intent(this, BottomNavActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.buttonLogin.isEnabled = true
                    Toast.makeText(this, "로그인 실패: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Login Error", result.exception)
                }
            }
        }
    }
}
