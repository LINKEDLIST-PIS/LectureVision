package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.FragmentSettingsBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.settings.SettingsViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeUserResult()

        // Request user data when the view is created
        settingsViewModel.fetchUserData()
        
        // Display app version
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            binding.tvVersion.text = packageInfo.versionName
        } catch (e: Exception) {
            binding.tvVersion.text = "1.0.0"
        }
    }

    private fun observeUserResult() {
        settingsViewModel.userResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.tvUserName.text = "로딩 중..."
                    binding.tvUserEmail.text = "..."
                    // Optionally, show a progress bar
                }
                is Result.Success -> {
                    val user = result.data
                    // The API provides email, but not a name field.
                    // Using the email prefix as a temporary name.
                    binding.tvUserName.text = user.email.split("@").firstOrNull() ?: "사용자"
                    binding.tvUserEmail.text = user.email
                }
                is Result.Error -> {
                    binding.tvUserName.text = "사용자 정보 로드 실패"
                    binding.tvUserEmail.text = "-"
                    Toast.makeText(context, "오류: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SettingsFragment", "Error fetching user data", result.exception)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(context, "회원정보 편집 기능 구현 필요", Toast.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "알림이 켜졌습니다." else "알림이 꺼졌습니다."
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("정말로 로그아웃 하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                TokenManager.clearToken()

                val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
