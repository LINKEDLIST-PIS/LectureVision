package com.son.lecture_project

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Notice
import com.son.lecture_project.databinding.FragmentHomeBinding
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeMeasurementResult()
        loadStaticHomePageData()
    }

    private fun setupClickListeners() {
        binding.buttonStartTimer.setOnClickListener {
            homeViewModel.startMeasurement()
        }
    }

    private fun observeMeasurementResult() {
        homeViewModel.measurementResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.buttonStartTimer.isEnabled = false
                    binding.textPresentCount.text = "..."
                    binding.textTotalCount.text = "..."
                    binding.textAbsentCount.text = "..."
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.buttonStartTimer.isEnabled = true

                    val presentCount = result.data
                    binding.textPresentCount.text = presentCount.toString()

                    // TODO: The total count is not available from the current API.
                    binding.textTotalCount.text = "-"
                    binding.textAbsentCount.text = "-"

                    Toast.makeText(context, "인원수 측정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.buttonStartTimer.isEnabled = true
                    binding.textPresentCount.text = "-"
                    binding.textTotalCount.text = "-"
                    binding.textAbsentCount.text = "-"
                    Toast.makeText(context, "오류 발생: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("HomeFragment", "Measurement Error", result.exception)
                }
            }
        }
    }

    private fun loadStaticHomePageData() {
        updateTodaySchedule()
        updateNotices()
    }

    private fun updateTodaySchedule() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")
        val todayName = dayNames[dayOfWeek - 1]

        binding.textTodayScheduleTitle.text = "오늘의 시간표 ($todayName)"

        val todayClasses = getTodayClassesFromDummyData(todayName)

        if (todayClasses.isEmpty()) {
            binding.textNoSchedule.isVisible = true
            binding.layoutScheduleItems.isVisible = false
        } else {
            binding.textNoSchedule.isVisible = false
            binding.layoutScheduleItems.isVisible = true
            binding.layoutScheduleItems.removeAllViews()

            todayClasses.forEach { classItem ->
                val textView = TextView(context).apply {
                    text = "• ${classItem.name} (${classItem.startTime} - ${classItem.endTime})"
                    textSize = 16f
                    setTextColor(Color.parseColor("#1E293B"))
                    setPadding(0, 8, 0, 8)
                }
                binding.layoutScheduleItems.addView(textView)
            }
        }
    }

    private fun updateNotices() {
        val notices = getNoticesFromDummyData()

        binding.layoutNoticeItems.removeAllViews()

        if (notices.isEmpty()) {
            val textView = TextView(context).apply {
                text = "새로운 공지사항이 없습니다."
                setTextColor(Color.parseColor("#6B7280"))
            }
            binding.layoutNoticeItems.addView(textView)
        } else {
            notices.forEach { notice ->
                val textView = TextView(context).apply {
                    text = "• ${notice.title}"
                    textSize = 16f
                    setTextColor(Color.parseColor("#1E293B"))
                    setPadding(0, 8, 0, 8)
                }
                binding.layoutNoticeItems.addView(textView)
            }
        }
    }

    private fun getTodayClassesFromDummyData(todayName: String): List<ClassSchedule> {
        val allClasses = listOf(
            ClassSchedule(1, "자료구조", "월요일", "09:00", "10:30", "#E0F7FA"),
            ClassSchedule(2, "알고리즘", "월요일", "11:00", "12:30", "#E0F7FA"),
            ClassSchedule(3, "운영체제", "화요일", "10:00", "11:30", "#FCE4EC"),
            ClassSchedule(4, "객체지향프로그래밍", "수요일", "13:00", "15:00", "#F1F8E9"),
            ClassSchedule(5, "데이터베이스", "목요일", "09:00", "11:00", "#FFFDE7"),
            ClassSchedule(6, "컴퓨터네트워크", "금요일", "14:00", "16:00", "#EFEBE9")
        )
        return allClasses.filter { it.day == todayName }
    }

    private fun getNoticesFromDummyData(): List<Notice> {
        return listOf(
            Notice(1, "11월 중간고사 일정 안내", "2025-11-01"),
            Notice(2, "출석 체크 시스템 업데이트", "2025-10-30"),
            Notice(3, "휴강 안내 - 11월 5일", "2025-10-28")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
