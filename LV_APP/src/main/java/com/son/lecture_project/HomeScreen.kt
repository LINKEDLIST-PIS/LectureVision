package com.son.lecture_project

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.databinding.ScreenHomeBinding
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.ticket.TicketViewModel
import java.util.Calendar
import java.util.Locale

class HomeScreen : Fragment() {

    private var _binding: ScreenHomeBinding? = null
    private val binding get() = _binding!!


    private val homeViewModel: HomeViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimerPicker()
        setupClickListeners()
        
        observeMeasurementResult()
        observeHomeData()
        observeTimer()
        observeComparisonResult() 
        
        Log.d("HomeScreen", "Loading home data...")
        homeViewModel.loadHomeData()
    }

    private fun setupTimerPicker() {
        binding.pickerMinutes.minValue = 1
        binding.pickerMinutes.maxValue = 120
        binding.pickerMinutes.value = 60
        binding.pickerMinutes.wrapSelectorWheel = false
    }

    private fun setupClickListeners() {
        binding.buttonStartTimer.setOnClickListener {
            val isRunning = homeViewModel.isTimerRunning.value ?: false
            if (isRunning) {
                // 측정 정지
                homeViewModel.stopTimer()
                Toast.makeText(context, getString(R.string.msg_timer_finished), Toast.LENGTH_SHORT).show()
            } else {
                // 측정 시작
                val selectedMinutes = binding.pickerMinutes.value
                homeViewModel.startTimer(selectedMinutes)
            }
        }
    }
    
    private fun observeTimer() {
        // 타이머 진행 상태 관찰
        homeViewModel.isTimerRunning.observe(viewLifecycleOwner) { isRunning ->
            if (isRunning) {
                binding.layoutTimePickerGroup.isVisible = false
                binding.textTimerDisplay.isVisible = true
                binding.buttonStartTimer.text = getString(R.string.home_timer_stop_btn)
            } else {
                binding.layoutTimePickerGroup.isVisible = true
                binding.textTimerDisplay.isVisible = false
                binding.buttonStartTimer.text = getString(R.string.home_timer_start_btn)
                binding.textTimerDisplay.text = "00:00"
            }
        }
        
        // 타이머 시간 텍스트 관찰
        homeViewModel.timerText.observe(viewLifecycleOwner) { timeText ->
            if (!timeText.isNullOrEmpty()) {
                binding.textTimerDisplay.text = timeText
            }
        }
    }

    private fun observeMeasurementResult() {
        // Event Wrapper 적용된 데이터 관찰
        homeViewModel.measurementResult.observe(viewLifecycleOwner) { event ->
            // getContentIfNotHandled()를 통해 한 번만 처리
            val result = event.getContentIfNotHandled() ?: return@observe
            
            when (result) {
                is Result.Loading -> binding.progressBar.isVisible = true
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.textPresentCount.text = result.data.toString()
                    
                    // 성공 시에만 토스트 표시 (중복 방지)
                    Toast.makeText(context, getString(R.string.msg_measurement_complete), Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    Log.w("HomeScreen", "Measurement failed: ${result.exception.message}")
                }
            }
        }
    }
    
    private fun observeComparisonResult() {
        homeViewModel.comparisonResult.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            // 비교 결과 팝업 표시
            showComparisonDialog(result.startCount, result.endCount)
        }
    }
    
    private fun showComparisonDialog(startCount: Int, endCount: Int) {
        val totalStudent = 30 
        val startAbsent = totalStudent - startCount
        val endAbsent = totalStudent - endCount
        
        val message = """
            <타이머 시작>
            총인원: $totalStudent
            인원: $startCount
            결석: $startAbsent
            
                   ⬇
            
            <타이머 종료>
            총인원: $totalStudent
            인원: $endCount
            결석: $endAbsent
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle("출석 측정 결과")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun observeHomeData() {
        homeViewModel.todayClasses.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { Log.d("HomeScreen", "Timetable Loading") }
                is Result.Success -> {
                    Log.d("HomeScreen", "Timetable Success: ${result.data.size} items")
                    updateTodayScheduleUI(result.data)
                }
                is Result.Error -> {
                    Log.e("HomeScreen", "Timetable Error", result.exception)
                    updateTodayScheduleUI(emptyList())
                }
            }
        }
    }

    private fun updateTodayScheduleUI(todayClasses: List<ClassSchedule>) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf(
            getString(R.string.day_sunday), 
            getString(R.string.day_monday), 
            getString(R.string.day_tuesday), 
            getString(R.string.day_wednesday), 
            getString(R.string.day_thursday), 
            getString(R.string.day_friday), 
            getString(R.string.day_saturday)
        )
        val todayName = dayNames[dayOfWeek - 1]

        binding.textTodayScheduleTitle.text = getString(R.string.format_today_schedule, todayName)
        binding.textTodayScheduleTitle.isVisible = true
        
        binding.layoutScheduleItems.removeAllViews()

        if (todayClasses.isEmpty()) {
            binding.textNoSchedule.text = getString(R.string.home_no_schedule)
            binding.textNoSchedule.isVisible = true
            binding.layoutScheduleItems.isVisible = false
        } else {
            binding.textNoSchedule.isVisible = false
            binding.layoutScheduleItems.isVisible = true

            todayClasses.forEach { classItem ->
                val textView = TextView(context).apply {
                    text = getString(R.string.format_schedule_item, classItem.name, classItem.startTime, classItem.endTime)
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setPadding(0, 8, 0, 8)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                binding.layoutScheduleItems.addView(textView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
