package com.son.lecture_project

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.ScreenSettingsBinding
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SettingsScreen : Fragment() {

    private var _binding: ScreenSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val homeViewModel: HomeViewModel by activityViewModels()

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
        
        observeTicketStatus()

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
        binding.switchDarkMode.isChecked = TokenManager.isDarkMode()
        binding.switchNotifications.isChecked = TokenManager.areNotificationsEnabled()
        binding.switchTicketIndicator.isChecked = TokenManager.isTicketIndicatorVisible()
        
        val isDebugMode = TokenManager.isDebugMode()
        binding.switchDebugMode.isChecked = isDebugMode
        binding.layoutModelUrl.visibility = if (isDebugMode) View.VISIBLE else View.GONE
        
        val savedUrl = TokenManager.getModelServerUrl()
        if (!savedUrl.isNullOrEmpty()) {
            binding.etModelServerUrl.setText(savedUrl)
        }
        
        updateTokenStatusUI()
        
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val langCode = currentLocale?.language ?: TokenManager.getLanguage()
        binding.tvCurrentLanguage.text = if (langCode == "en") getString(R.string.current_lang_en) else getString(R.string.current_lang_ko)
    }
    
    private fun updateTokenStatusUI() {
        if (!TokenManager.isTokenValid()) {
             binding.tvTokenStatus.text = "토큰 없음 (만료)"
             return
        }
        val token = TokenManager.getToken()
        if (token.isNullOrEmpty()) {
             binding.tvTokenStatus.text = "토큰 없음"
        } else {
             val maskedToken = if (token.length > 15) "${token.substring(0, 10)}...${token.substring(token.length - 5)}" else token
             binding.tvTokenStatus.text = "보유 중 ($maskedToken)"
        }
    }

    private fun setupClickListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setDarkMode(isChecked)
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }
        
        binding.btnPrivacyPolicy.setOnClickListener {
             val intent = Intent(requireContext(), WebViewActivity::class.java)
             intent.putExtra("URL", "https://app-privacy-policy-generator.nisrulz.com/")
             intent.putExtra("TITLE", "개인정보처리방침")
             startActivity(intent)
        }
        
        binding.btnTermsOfService.setOnClickListener {
             val intent = Intent(requireContext(), WebViewActivity::class.java)
             intent.putExtra("URL", "https://termly.io/resources/templates/")
             intent.putExtra("TITLE", "서비스 이용약관")
             startActivity(intent)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setNotificationsEnabled(isChecked)
        }
        
        binding.switchTicketIndicator.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setTicketIndicatorVisible(isChecked)
            (activity as? BottomNavActivity)?.refreshTicketIndicatorVisibility()
        }
        
        binding.switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            TokenManager.setDebugMode(isChecked)
            binding.layoutModelUrl.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.btnSaveModelUrl.setOnClickListener {
            val inputUrl = binding.etModelServerUrl.text.toString().trim()
            if (inputUrl.isNotEmpty()) {
                if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                    Toast.makeText(context, "URL은 http:// 또는 https://로 시작해야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                Toast.makeText(context, "연결 테스트 중...", Toast.LENGTH_SHORT).show()
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val connection = URL(inputUrl).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        val responseCode = connection.responseCode
                        if (responseCode > 0) {
                             withContext(Dispatchers.Main) {
                                TokenManager.setModelServerUrl(inputUrl)
                                Toast.makeText(context, "모델 서버 연결 성공! URL이 저장되었습니다.", Toast.LENGTH_LONG).show()
                             }
                        } else {
                            throw Exception("Connection failed with code $responseCode")
                        }
                        connection.disconnect()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "모델 서버 연결 실패. URL을 확인해주세요.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "URL을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 티켓 수동 발급 버튼
        binding.btnManualTicket.setOnClickListener {
            homeViewModel.testTicketIssuance()
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }
    
    private fun observeTicketStatus() {
        homeViewModel.ticketStatus.observe(viewLifecycleOwner) { result ->
            if (view?.isShown == false) return@observe
            
            when (result) {
                is Result.Success -> Toast.makeText(context, "티켓 상태: ${result.data}", Toast.LENGTH_SHORT).show()
                is Result.Error -> Toast.makeText(context, "티켓 오류: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                is Result.Loading -> {}
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.current_lang_ko), getString(R.string.current_lang_en))
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val currentLang = currentLocale?.language ?: "ko"
        
        val checkedItem = if (currentLang == "en") 1 else 0

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle(getString(R.string.dialog_lang_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val langCode = if (which == 0) "ko" else "en"
                if (currentLang != langCode) {
                    TokenManager.setLanguage(langCode)
                    dialog.dismiss()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                TokenManager.clearAllData()

                val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
