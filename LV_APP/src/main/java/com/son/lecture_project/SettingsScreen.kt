package com.son.lecture_project

import android.content.Intent
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
    
    // Ticket 발급 로직 재사용을 위해 HomeViewModel 사용 (activityViewModels로 공유)
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
        
        // 현재 토큰 상태 표시
        updateTokenStatusUI()
        
        // 현재 언어 설정에 따라 텍스트 업데이트
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val langCode = currentLocale?.language ?: TokenManager.getLanguage()
        
        binding.tvCurrentLanguage.text = if (langCode == "en") getString(R.string.current_lang_en) else getString(R.string.current_lang_ko)
    }
    
    private fun updateTokenStatusUI() {
        val token = TokenManager.getToken()
        if (token.isNullOrEmpty()) {
             binding.tvTokenStatus.text = "토큰 없음"
        } else {
             // 토큰의 앞 10자리와 끝 5자리만 보여줌
             val maskedToken = if (token.length > 15) {
                 "${token.substring(0, 10)}...${token.substring(token.length - 5)}"
             } else {
                 token
             }
             binding.tvTokenStatus.text = "보유 중 ($maskedToken)"
        }
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
        
        // 개인정보처리방침 (WebViewActivity로 이동)
        binding.btnPrivacyPolicy.setOnClickListener {
             val intent = Intent(requireContext(), WebViewActivity::class.java)
             intent.putExtra("URL", "https://app-privacy-policy-generator.nisrulz.com/") // 임시 URL
             intent.putExtra("TITLE", "개인정보처리방침")
             startActivity(intent)
        }
        
        // 서비스 이용약관 (WebViewActivity로 이동)
        binding.btnTermsOfService.setOnClickListener {
             val intent = Intent(requireContext(), WebViewActivity::class.java)
             intent.putExtra("URL", "https://termly.io/resources/templates/") // 임시 URL
             intent.putExtra("TITLE", "서비스 이용약관")
             startActivity(intent)
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
            if (isChecked) {
                updateTokenStatusUI()
            }
        }
        
        // 모델 서버 URL 저장 및 연결 테스트 버튼
        binding.btnSaveModelUrl.setOnClickListener {
            val inputUrl = binding.etModelServerUrl.text.toString().trim()
            if (inputUrl.isNotEmpty()) {
                if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                    Toast.makeText(context, "URL은 http:// 또는 https://로 시작해야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                Toast.makeText(context, "연결 테스트 중...", Toast.LENGTH_SHORT).show()
                
                // 2. 실제 연결 테스트 (Ping 방식: HttpURLConnection 사용)
                lifecycleScope.launch(Dispatchers.IO) {
                    var isReachable = false
                    try {
                        val connection = URL(inputUrl).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000 // 3초 타임아웃
                        connection.readTimeout = 3000
                        
                        // 연결 시도 (응답 코드가 200이 아니어도 연결 자체는 성공한 것으로 간주할 수 있음)
                        // 여기서는 응답 코드를 받아오는 것으로 서버 존재 여부 확인
                        val responseCode = connection.responseCode
                        Log.d("SettingsScreen", "Ping response code: $responseCode")
                        
                        if (responseCode > 0) {
                            isReachable = true
                        }
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Ping failed", e)
                        isReachable = false
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (isReachable) {
                            // 연결 성공 시에만 URL 저장
                            TokenManager.setModelServerUrl(inputUrl)
                            Toast.makeText(context, "모델 서버 연결 성공! URL이 저장되었습니다.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "모델 서버 연결 실패. URL을 확인해주세요.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "URL을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 티켓 수동 발급 버튼 (테스트용)
        binding.btnManualTicket.setOnClickListener {
            Toast.makeText(context, "티켓 발급 및 측정을 시도합니다...", Toast.LENGTH_SHORT).show()
            homeViewModel.issueTicketAndMeasure()
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }
    
    private fun observeTicketStatus() {
        homeViewModel.ticketStatus.observe(viewLifecycleOwner) { result ->
            // Settings 화면이 visible일 때만 Toast 등을 띄우거나 UI 업데이트
            // 여기서는 로그만 찍거나 간단한 토스트 처리 (HomeViewModel에서 이미 처리된 상태가 올 수 있음)
            if (binding.layoutModelUrl.visibility == View.VISIBLE) {
                when (result) {
                    is Result.Success -> {
                        // 필요시 여기에 추가적인 UI 피드백
                        Log.d("SettingsScreen", "Ticket Status: ${result.data}")
                    }
                    is Result.Error -> {
                        Log.e("SettingsScreen", "Ticket Error: ${result.exception.message}")
                    }
                    is Result.Loading -> {
                        
                    }
                }
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

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
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
