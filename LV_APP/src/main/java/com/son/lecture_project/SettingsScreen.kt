package com.son.lecture_project

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.ScreenSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsScreen : Fragment() {

    private var _binding: ScreenSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        loadUserInfo()
        setupClickListeners()

        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            binding.tvVersion.text = packageInfo.versionName
        } catch (_: Exception) {
            binding.tvVersion.text = "1.0.0"
        }
    }

    private fun loadUserInfo() {
        val email = TokenManager.getUserEmail() ?: "No Email"
        var name = TokenManager.getUserName()
        
        if (name.isNullOrEmpty()) {
            name = email.split("@").firstOrNull() ?: "User"
        }

        binding.tvUserName.text = name
        binding.tvUserEmail.text = email
    }

    private fun loadSettings() {
        // 다크 모드 설정 로드
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.isChecked = isDarkMode

        // 알림 설정 로드
        val areNotificationsEnabled = TokenManager.areNotificationsEnabled()
        binding.switchNotifications.isChecked = areNotificationsEnabled
        
        // 티켓 상태 표시 설정 로드
        val isTicketIndicatorVisible = TokenManager.isTicketIndicatorVisible()
        binding.switchTicketIndicator.isChecked = isTicketIndicatorVisible
        
        // 디버그 모드 설정 로드
        val isDebugMode = TokenManager.isDebugMode()
        binding.switchDebugMode.isChecked = isDebugMode
        binding.layoutModelUrl.visibility = if (isDebugMode) View.VISIBLE else View.GONE
        
        // 모델 서버 URL 로드
        val savedUrl = TokenManager.getModelServerUrl()
        if (!savedUrl.isNullOrEmpty()) {
            binding.etModelServerUrl.setText(savedUrl)
        }
        
        // 현재 언어 설정에 따라 텍스트 업데이트
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val langCode = currentLocale?.language ?: TokenManager.getLanguage()
        
        binding.tvCurrentLanguage.text = if (langCode == "en") getString(R.string.current_lang_en) else getString(R.string.current_lang_ko)
    }

    private fun setupClickListeners() {
        // 다크 모드 설정 스위치
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(context, getString(R.string.msg_dark_mode_on), Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(context, getString(R.string.msg_dark_mode_off), Toast.LENGTH_SHORT).show()
            }
            // 필요 시 SharedPreferences 등에 상태 저장 (앱 재실행 시 유지하려면)
        }

        // 언어 변경
        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }
        
        // 비밀번호 변경
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
        
        // 이메일 변경
        binding.btnChangeEmail.setOnClickListener {
            showChangeEmailDialog()
        }
        
        // 이용약관 & 개인정보처리방침
        binding.btnPrivacyPolicy.setOnClickListener {
             Toast.makeText(context, getString(R.string.msg_privacy_preparing), Toast.LENGTH_SHORT).show()
        }
        
        binding.btnTermsOfService.setOnClickListener {
             Toast.makeText(context, getString(R.string.msg_terms_preparing), Toast.LENGTH_SHORT).show()
        }

        // 알림 설정 스위치
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setNotificationsEnabled(isChecked)
            val message = if (isChecked) getString(R.string.msg_noti_on) else getString(R.string.msg_noti_off)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        
        // 티켓 상태 표시 스위치
        binding.switchTicketIndicator.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setTicketIndicatorVisible(isChecked)
            (activity as? BottomNavActivity)?.refreshTicketIndicatorVisibility()
        }
        
        // 디버그 모드 스위치
        binding.switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setDebugMode(isChecked)
            binding.layoutModelUrl.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // 모델 서버 URL 저장 및 연결 테스트 버튼
        binding.btnSaveModelUrl.setOnClickListener {
            val url = binding.etModelServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Toast.makeText(context, "URL은 http:// 또는 https://로 시작해야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // 1. URL 저장 (RetrofitClient가 이 값을 사용하게 됨)
                TokenManager.setModelServerUrl(url)
                Toast.makeText(context, "연결 시도 중...", Toast.LENGTH_SHORT).show()
                
                // 2. 실제 연결 테스트 (Health Check)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // checkHealth 호출 (GET /)
                        val response = RetrofitClient.modelApiService.checkHealth()
                        withContext(Dispatchers.Main) {
                            // 응답이 성공적이거나, 404라도 서버가 응답했다면 연결은 된 것으로 간주
                            if (response.isSuccessful || response.code() != 0) {
                                Toast.makeText(context, "모델 서버에 성공적으로 연결되었습니다!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "서버 응답 오류: ${response.code()}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                             Toast.makeText(context, "연결 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "URL을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.current_lang_ko), getString(R.string.current_lang_en))
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val currentLang = currentLocale?.language ?: "ko"
        
        val checkedItem = if (currentLang == "en") 1 else 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_lang_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val langCode = if (which == 0) "ko" else "en"
                
                // 언어 코드만 변경하고 다크모드 상태는 유지하도록 주의
                if (currentLang != langCode) {
                    TokenManager.setLanguage(langCode)
                    dialog.dismiss()

                    val localeList = LocaleListCompat.forLanguageTags(langCode)
                    AppCompatDelegate.setApplicationLocales(localeList)

                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val currentPassInput = EditText(requireContext())
        currentPassInput.hint = getString(R.string.hint_current_pw)
        currentPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(currentPassInput)

        val newPassInput = EditText(requireContext())
        newPassInput.hint = getString(R.string.hint_new_pw)
        newPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(newPassInput)
        
        val confirmPassInput = EditText(requireContext())
        confirmPassInput.hint = getString(R.string.hint_confirm_pw)
        confirmPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(confirmPassInput)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_pw_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_change)) { _, _ ->
                val currentPass = currentPassInput.text.toString()
                val newPass = newPassInput.text.toString()
                val confirmPass = confirmPassInput.text.toString()

                if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(context, getString(R.string.msg_fill_all), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPass != confirmPass) {
                    Toast.makeText(context, getString(R.string.msg_pw_mismatch), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                Toast.makeText(context, getString(R.string.msg_pw_changed), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    
    private fun showChangeEmailDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val emailInput = EditText(requireContext())
        emailInput.hint = getString(R.string.hint_new_email)
        layout.addView(emailInput)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_email_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_change)) { _, _ ->
                val newEmail = emailInput.text.toString()

                if (newEmail.isEmpty() || !newEmail.contains("@")) {
                    Toast.makeText(context, getString(R.string.msg_invalid_email), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                TokenManager.saveUserEmail(newEmail)
                binding.tvUserEmail.text = newEmail
                Toast.makeText(context, getString(R.string.msg_email_changed), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                TokenManager.clearToken()

                val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                Toast.makeText(context, getString(R.string.action_logout) + "...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
