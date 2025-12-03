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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

    override fun onResume() {
        super.onResume()
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
                // 측정 중단: ViewModel의 stopTimer만 호출하면 모든 로직이 처리됨
                homeViewModel.stopTimer()
            } else {
                // 측정 시작
                val selectedMinutes = binding.pickerMinutes.value
                homeViewModel.startTimer(selectedMinutes)
            }
        }
    }
    
    private fun observeTimer() {
        homeViewModel.isTimerRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.layoutTimePickerGroup.isVisible = !isRunning
            binding.textTimerDisplay.isVisible = isRunning
            binding.buttonStartTimer.text = if (isRunning) getString(R.string.home_timer_stop_btn) else getString(R.string.home_timer_start_btn)
            if(!isRunning) {
                 binding.textTimerDisplay.text = "00:00"
            }
        }
        
        homeViewModel.timerText.observe(viewLifecycleOwner) { timeText ->
            if (!timeText.isNullOrEmpty()) {
                binding.textTimerDisplay.text = timeText
            }
        }
    }

    private fun observeMeasurementResult() {
        homeViewModel.measurementResult.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            
            when (result) {
                is Result.Loading -> binding.progressBar.isVisible = true
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    val currentPeople = result.data
                    binding.textPresentCount.text = "$currentPeople"
                    
                    val totalStudents = getCurrentClassTotalStudents()
                    
                    if (totalStudents > 0) {
                        binding.textTotalCount.text = "$totalStudents"
                        val absent = (totalStudents - currentPeople).coerceAtLeast(0)
                        binding.textAbsentCount.text = "$absent"
                    } else {
                        binding.textTotalCount.text = "-"
                        binding.textAbsentCount.text = "-"
                    }
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    Toast.makeText(context, "측정 오류: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun observeComparisonResult() {
        homeViewModel.comparisonResult.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            showComparisonDialog(result.startCount, result.endCount)
        }
    }
    
    private fun showComparisonDialog(startCount: Int, endCount: Int) {
        val totalStudent = getCurrentClassTotalStudents()
        
        val totalDisplay = if (totalStudent > 0) "$totalStudent" else "-"
        val startAbsentDisplay = if (totalStudent > 0) (totalStudent - startCount).coerceAtLeast(0).toString() else "-"
        val endAbsentDisplay = if (totalStudent > 0) (totalStudent - endCount).coerceAtLeast(0).toString() else "-"
        
        val message = """
            <타이머 시작>
            총인원: $totalDisplay
            인원: $startCount
            결석: $startAbsentDisplay
            
                   ⬇
            
            <타이머 종료>
            총인원: $totalDisplay
            인원: $endCount
            결석: $endAbsentDisplay
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle("출석 측정 결과")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun getCurrentClassTotalStudents(): Int {
        val todayClassesResult = homeViewModel.todayClasses.value
        
        if (todayClassesResult is Result.Success) {
            val schedules = todayClassesResult.data
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            val currentTimeInMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            
            val currentClass = schedules.find { schedule ->
                val start = parseTimeToMinutes(schedule.startTime)
                val end = parseTimeToMinutes(schedule.endTime)
                if (start != -1 && end != -1) currentTimeInMinutes in start..end else false
            }
            return currentClass?.totalStudents ?: 0
        }
        return 0
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
        } catch (e: Exception) {
            -1
        }
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
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val dateFormat = SimpleDateFormat("MM/dd E", Locale.KOREAN)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val dateText = dateFormat.format(calendar.time)

        binding.textTodayScheduleTitle.text = getString(R.string.format_today_schedule, dateText)
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
                    val totalInfo = if (classItem.totalStudents > 0) " (총 ${classItem.totalStudents}명)" else ""
                    text = "${classItem.name} (${classItem.startTime}~${classItem.endTime})$totalInfo"
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    setPadding(0, 8, 0, 8)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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