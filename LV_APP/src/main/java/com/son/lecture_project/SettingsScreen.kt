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
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.ScreenSettingsBinding

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
        // 알림 설정 로드
        val areNotificationsEnabled = TokenManager.areNotificationsEnabled()
        binding.switchNotifications.isChecked = areNotificationsEnabled
        
        // 현재 언어 설정에 따라 텍스트 업데이트
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val langCode = currentLocale?.language ?: TokenManager.getLanguage()
        
        binding.tvCurrentLanguage.text = if (langCode == "en") getString(R.string.current_lang_en) else getString(R.string.current_lang_ko)
    }

    private fun setupClickListeners() {
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
