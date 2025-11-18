package com.son.lecture_project

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.son.lecture_project.databinding.ActivitySignupBinding
import com.son.lecture_project.ui.auth.SignupViewModel
import com.son.lecture_project.ui.home.Result

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val signupViewModel: SignupViewModel by viewModels()
    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeSignupResult()
    }

    private fun setupClickListeners() {
        binding.buttonSignup.setOnClickListener {
            val email = binding.editSignupEmail.text.toString().trim()
            val password = binding.editSignupPassword.text.toString().trim()
            val passwordConfirm = binding.editSignupPasswordConfirm.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.lowercase().endsWith("@gnu.ac.kr")) {
                Toast.makeText(this, "GNU 이메일만 사용 가능합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // According to the API spec, calling signup will trigger the verification email.
            signupViewModel.signup(email, password)
        }
    }

    private fun observeSignupResult() {
        signupViewModel.signupResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.buttonSignup.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.buttonSignup.isEnabled = true
                    Toast.makeText(this, "회원가입 요청이 성공했습니다. 이메일을 확인하여 계정을 활성화해주세요.", Toast.LENGTH_LONG).show()
                    finish() // Close SignupActivity and return to LoginActivity
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.buttonSignup.isEnabled = true
                    Toast.makeText(this, "회원가입 실패: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Signup Error", result.exception)
                }
            }
        }
    }
}
