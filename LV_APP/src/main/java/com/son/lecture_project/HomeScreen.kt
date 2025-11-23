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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
        
        Log.d("HomeScreen", "Loading home data...")
        // 데이터 로드는 뷰모델이 이미 데이터를 가지고 있지 않을 때만 호출하도록 할 수도 있지만,
        // 여기서는 간단히 매번 로드하되, 타이머 상태는 유지됨.
        homeViewModel.loadHomeData()
        homeViewModel.checkTicketStatus() 
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
        homeViewModel.measurementResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.isVisible = true
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.textPresentCount.text = result.data.toString()
                    binding.textTotalCount.text = "-"
                    binding.textAbsentCount.text = "-"
                    Toast.makeText(context, getString(R.string.msg_measurement_complete), Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.textPresentCount.text = "-"
                    Log.e("HomeFragment", "Measurement Error", result.exception)
                }
            }
        }
    }
    
    private fun observeHomeData() {
        homeViewModel.notices.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                }
                is Result.Success -> {
                    val notices = result.data
                    if (notices.isNotEmpty()) {
                        val latestNotice = notices.first()
                        binding.tvLatestNotice.text = latestNotice.title
                    } else {
                        binding.tvLatestNotice.text = getString(R.string.home_no_notice)
                    }
                }
                is Result.Error -> {
                    binding.tvLatestNotice.text = getString(R.string.home_notice_error)
                }
            }
        }
        
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
                    setTextColor(Color.parseColor("#1E293B"))
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
