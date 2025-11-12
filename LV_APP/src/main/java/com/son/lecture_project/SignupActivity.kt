package com.son.lecture_project

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.son.lecture_project.data.api.*
import com.son.lecture_project.databinding.ActivitySignupBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authService by lazy { ApiClient.instance.create(AuthService::class.java) }
    private var isCodeSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이메일 인증 버튼
        binding.buttonSendCode.setOnClickListener {
            val email = binding.editSignupEmail.text.toString().trim()
            if (email.isNotBlank()) {
                sendVerificationEmail(email)
            } else {
                Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 버튼
        binding.buttonSignup.setOnClickListener {
            val name = binding.editSignupName.text.toString().trim()
            val email = binding.editSignupEmail.text.toString().trim()
            val password = binding.editSignupPassword.text.toString().trim()

            if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && isCodeSent) {
                signupUser(name, email, password)
            } else if (!isCodeSent) {
                Toast.makeText(this, "이메일 인증을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationEmail(email: String) {
        val request = EmailRequest(email)
        authService.sendVerificationEmail(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@SignupActivity, "인증메일 전송됨", Toast.LENGTH_SHORT).show()
                    isCodeSent = true
                    binding.buttonSendCode.isEnabled = false
                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "전송 실패: ${response.body()?.message ?: "알 수 없음"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@SignupActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun signupUser(name: String, email: String, password: String) {
        val request = SignupRequest(name, email, password)
        authService.signupUser(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@SignupActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                    finish() // 로그인 화면으로 돌아가기
                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "회원가입 실패: ${response.body()?.message ?: "알 수 없음"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@SignupActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }
}