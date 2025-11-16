package com.son.lecture_project

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.api.AuthService
import com.son.lecture_project.data.api.MeasureResultResponse
import com.son.lecture_project.data.api.TicketResponse
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Notice
import com.son.lecture_project.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private val authService by lazy { ApiClient.instance.create(AuthService::class.java) }


    private val pollingHandler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null


    private val timerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            startMeasurementProcess()
        } else {

            Toast.makeText(context, "출석 체크가 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupClickListeners()
        loadHomePageData()
    }

    /**
     * NumberPicker 같은 UI 요소들을 초기 설정합니다.
     */
    private fun initializeViews() {
        binding.pickerMinutes.minValue = 1
        binding.pickerMinutes.maxValue = 180
        binding.pickerMinutes.value = 90 // 기본값 90분
    }

    /**
     * 버튼 클릭 이벤트를 설정합니다.
     */
    private fun setupClickListeners() {

        binding.buttonStartTimer.setOnClickListener {
            val intent = Intent(requireActivity(), TimerActivity::class.java).apply {

                putExtra("TIMER_MINUTES", binding.pickerMinutes.value.toLong())
            }
            timerActivityResultLauncher.launch(intent)
        }
    }



    private fun loadHomePageData() {
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

            // 동적으로 수업 목록 TextView를 생성하여 추가
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

    /**
     * 공지사항 데이터를 가져와 UI에 표시합니다.
     * TODO: 향후 이 부분은 실제 서버 API나 데이터베이스에서 데이터를 가져오도록 수정해야 합니다.
     */
    private fun updateNotices() {
        val notices = getNoticesFromDummyData()

        binding.layoutNoticeItems.removeAllViews() // 기존 뷰 제거

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

    // --- 아래는 서버와 통신하는 핵심 로직입니다. ---

    private fun startMeasurementProcess() {
        binding.textPresentCount.text = "..."
        binding.textTotalCount.text = "..."
        binding.textAbsentCount.text = "..."

        authService.startMeasurement().enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                if (response.isSuccessful) {
                    val ticketId = response.body()?.ticketId
                    if (ticketId != null) {
                        startPollingForResult(ticketId)
                    } else {
                        updateUiWithError("티켓 발급 실패")
                    }
                } else {
                    updateUiWithError("측정 요청 실패")
                }
            }

            override fun onFailure(call: Call<TicketResponse>, t: Throwable) {
                updateUiWithError("네트워크 오류")
                Log.e("HomeFragment", "Network Error: ${t.message}")
            }
        })
    }

    private fun startPollingForResult(ticketId: String) {
        pollingRunnable = object : Runnable {
            override fun run() {
                authService.getMeasurementResult(ticketId).enqueue(object : Callback<MeasureResultResponse> {
                    override fun onResponse(call: Call<MeasureResultResponse>, response: Response<MeasureResultResponse>) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            when (result?.status) {
                                "COMPLETED" -> {
                                    updateUiWithResult(result)
                                    stopPolling()
                                }
                                "PENDING" -> {
                                    pollingHandler.postDelayed(this, 2000) // 2초 후 다시 시도
                                }
                                else -> {
                                    updateUiWithError("측정 실패: ${result?.status}")
                                    stopPolling()
                                }
                            }
                        } else {
                            updateUiWithError("결과 조회 실패")
                            stopPolling()
                        }
                    }

                    override fun onFailure(call: Call<MeasureResultResponse>, t: Throwable) {
                        updateUiWithError("네트워크 오류")
                        Log.e("HomeFragment", "Polling Network Error: ${t.message}")
                        stopPolling()
                    }
                })
            }
        }
        pollingHandler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        pollingRunnable?.let { pollingHandler.removeCallbacks(it) }
        pollingRunnable = null
    }

    private fun updateUiWithResult(result: MeasureResultResponse) {
        binding.textPresentCount.text = result.presentCount.toString()
        binding.textTotalCount.text = result.totalCount.toString()
        binding.textAbsentCount.text = result.absentCount.toString()
        Toast.makeText(context, "인원수 측정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
    }

    private fun updateUiWithError(message: String) {
        binding.textPresentCount.text = "-"
        binding.textTotalCount.text = "-"
        binding.textAbsentCount.text = "-"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling() // 프래그먼트가 사라질 때 폴링 중단 (메모리 누수 방지)
        _binding = null
    }
}

