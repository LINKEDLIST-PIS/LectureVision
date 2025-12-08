package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        // 약관 보기 버튼
        binding.btnShowTerm.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("URL", "https://termly.io/resources/templates/") // 임시 URL
            intent.putExtra("TITLE", "이용약관")
            startActivity(intent)
        }

        // 개인정보처리방침 보기 버튼
        binding.btnShowPrivacy.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("URL", "https://app-privacy-policy-generator.nisrulz.com/") // 임시 URL
            intent.putExtra("TITLE", "개인정보처리방침")
            startActivity(intent)
        }

        binding.buttonSignup.setOnClickListener {
            val email = binding.editSignupEmail.text.toString().trim()
            val password = binding.editSignupPassword.text.toString().trim()
            val passwordConfirm = binding.editSignupPasswordConfirm.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일, 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
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

            // 약관 동의 체크 확인
            if (!binding.checkTerm.isChecked) {
                Toast.makeText(this, "이용약관에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.checkPrivacy.isChecked) {
                Toast.makeText(this, "개인정보처리방침에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call signup
            signupViewModel.signup(email, password)
        }

        // 로그인 화면으로 이동 (Link)
        binding.textLoginLink.setOnClickListener {
            finish()
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
                    

                    MaterialAlertDialogBuilder(this, R.style.Theme_Lecture_project_AlertDialog)
                        .setTitle("회원가입 요청 완료")
                        .setMessage("회원가입 요청이 성공했습니다.\n이메일에서 인증을 완료해주세요.")
                        .setPositiveButton("확인") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
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
