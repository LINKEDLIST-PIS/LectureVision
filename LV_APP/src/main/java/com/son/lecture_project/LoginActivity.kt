package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.ActivityLoginBinding
import com.son.lecture_project.ui.auth.LoginViewModel
import com.son.lecture_project.ui.home.Result

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeLoginResult()
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editLoginEmail.text.toString().trim() // Changed ID: editEmail -> editLoginEmail
            val password = binding.editLoginPassword.text.toString().trim() // Changed ID: editPassword -> editLoginPassword

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.login(email, password)
        }

        binding.textSignupLink.setOnClickListener { // Changed ID: textSignup -> textSignupLink
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
                    
                    // Save the token
                    TokenManager.saveToken(result.data.accessToken)
                    
                    // 로그인 성공 시 이메일과 비밀번호 저장 (자동 로그인용)
                    // 주의: 비밀번호를 평문으로 저장하는 것은 보안에 취약합니다.
                    val email = binding.editLoginEmail.text.toString().trim()
                    val password = binding.editLoginPassword.text.toString().trim()
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        TokenManager.saveUserEmail(email)
                        TokenManager.saveUserPassword(password)
                    }
                    
                    navigateToMain()
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

    private fun navigateToMain() {
        val intent = Intent(this, BottomNavActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
