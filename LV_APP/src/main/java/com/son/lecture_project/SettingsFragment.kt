package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.semantics.text
import androidx.fragment.app.Fragment
import com.son.lecture_project.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        // TODO: 실제로는 SharedPreferences나 데이터베이스에서 사용자 정보를 불러와야 합니다.
        binding.tvUserName.text = "홍길동"
        binding.tvUserEmail.text = "gildong@gnu.ac.kr"

        // 앱 버전 정보 표시
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            binding.tvVersion.text = packageInfo.versionName
        } catch (e: Exception) {
            binding.tvVersion.text = "1.0.0"
        }
    }

    private fun setupClickListeners() {

        binding.btnEditProfile.setOnClickListener {
            // TODO: React 코드의 Dialog처럼, 회원정보 변경을 위한 다이얼로그나 새 화면을 띄워야 합니다.
            Toast.makeText(context, "회원정보 편집 기능 구현 필요", Toast.LENGTH_SHORT).show()
        }


        binding.switchDarkMode.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
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
                // TODO: 저장된 로그인 토큰 삭제 등 실제 로그아웃 로직 구현


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
