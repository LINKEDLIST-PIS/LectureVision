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

    // 화면이 다시 보일 때마다 데이터 갱신 (시간표 변경 반영)
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
                    val currentPeople = result.data
                    binding.textPresentCount.text = "$currentPeople"
                    
                    // 현재 시간에 맞는 수업의 총 인원 가져오기
                    val totalStudents = getCurrentClassTotalStudents()
                    
                    if (totalStudents > 0) {
                        binding.textTotalCount.text = "$totalStudents"
                        
                        // 결석 계산 로직 수정
                        val absent = totalStudents - currentPeople
                        if (absent < 0) {
                            // 현재원이 총원보다 많은 경우
                            binding.textAbsentCount.text = "-"
                            Toast.makeText(context, "측정된 인원이 총 인원보다 많습니다.", Toast.LENGTH_LONG).show()
                        } else {
                            binding.textAbsentCount.text = "$absent"
                        }
                    } else {
                        binding.textTotalCount.text = "-"
                        binding.textAbsentCount.text = "-"
                    }
                    
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
        // 현재 시간에 맞는 수업의 총 인원 가져오기
        val totalStudent = getCurrentClassTotalStudents()
        
        // 결석 계산 로직 수정 (음수 처리)
        val startAbsent = if (totalStudent > 0) totalStudent - startCount else 0
        val endAbsent = if (totalStudent > 0) totalStudent - endCount else 0
        
        // 표시 텍스트 설정
        val totalDisplay = if (totalStudent > 0) "$totalStudent" else "-"
        val startAbsentDisplay = if (totalStudent > 0 && startAbsent >= 0) "$startAbsent" else "-"
        val endAbsentDisplay = if (totalStudent > 0 && endAbsent >= 0) "$endAbsent" else "-"
        
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
        
        // 음수 발생 시 경고 메시지 추가
        if ((totalStudent > 0) && (startAbsent < 0 || endAbsent < 0)) {
            Toast.makeText(context, "측정된 인원이 총 인원보다 많습니다.", Toast.LENGTH_LONG).show()
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle("출석 측정 결과")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }
    
    // 현재 시간(KST) 기준 진행 중인 수업의 총 인원수 반환
    private fun getCurrentClassTotalStudents(): Int {
        val todayClassesResult = homeViewModel.todayClasses.value
        
        if (todayClassesResult is Result.Success) {
            val schedules = todayClassesResult.data
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            
            // 현재 시간이 수업 시간 범위 내에 있는 수업 찾기
            val currentClass = schedules.find { schedule ->
                val start = parseTimeToMinutes(schedule.startTime)
                val end = parseTimeToMinutes(schedule.endTime)
                
                if (start != -1 && end != -1) {
                    currentTimeInMinutes in start..end
                } else {
                    false
                }
            }
            
            return currentClass?.totalStudents ?: 0
        }
        
        return 0
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            h * 60 + m
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
        // 1. 날짜 및 요일 표시 (한국 시간 기준)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        
        // 날짜 포맷팅 (예: 11/25 월)
        val dateFormat = SimpleDateFormat("MM/dd E", Locale.KOREAN)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val dateText = dateFormat.format(calendar.time)

        binding.textTodayScheduleTitle.text = getString(R.string.format_today_schedule, dateText)
        binding.textTodayScheduleTitle.isVisible = true
        
        // 2. 시간표 목록 업데이트
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
                    // 시간표 항목에 총 인원 정보도 표시 (선택 사항)
                    val totalInfo = if (classItem.totalStudents > 0) " (총 ${classItem.totalStudents}명)" else ""
                    text = "${classItem.name} (${classItem.startTime}~${classItem.endTime})$totalInfo"
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
