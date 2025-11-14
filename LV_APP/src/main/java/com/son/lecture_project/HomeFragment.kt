package com.son.lecture_project

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.api.AuthService
import com.son.lecture_project.data.api.MeasureResultResponse
import com.son.lecture_project.data.api.TicketResponse
import com.son.lecture_project.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val authService by lazy { ApiClient.instance.create(AuthService::class.java) }

    // Polling
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    // Timer
    private var countdownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftInMillis: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimerUI()

        binding.buttonStartTimer.setOnClickListener {
            if (isTimerRunning) {
                stopCountdown()
            } else {
                startCountdown()
            }
        }
    }

    private fun setupTimerUI() {
        binding.pickerMinutes.minValue = 1
        binding.pickerMinutes.maxValue = 180
        binding.pickerMinutes.value = 90
        updateTimerButtonUI()
    }

    private fun startCountdown() {
        val minutes = binding.pickerMinutes.value
        timeLeftInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
        isTimerRunning = true

        countdownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplayUI()
            }

            override fun onFinish() {
                isTimerRunning = false
                updateTimerButtonUI()
                startMeasurementProcess()
            }
        }.start()

        updateTimerButtonUI()
    }

    private fun stopCountdown() {
        countdownTimer?.cancel()
        isTimerRunning = false
        updateTimerButtonUI()
    }

    private fun updateTimerDisplayUI() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) - TimeUnit.MINUTES.toSeconds(minutes)
        binding.textTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTimerButtonUI() {
        if (isTimerRunning) {
            binding.layoutTimePickerGroup.visibility = View.GONE
            binding.textTimerDisplay.visibility = View.VISIBLE
            binding.buttonStartTimer.text = "중지"
        } else {
            binding.layoutTimePickerGroup.visibility = View.VISIBLE
            binding.textTimerDisplay.visibility = View.GONE
            binding.buttonStartTimer.text = "출석 체크 시작"
        }
    }



    private fun startMeasurementProcess() {
        binding.textPresentCount.text = "측정 중..."
        binding.textTotalCount.text = "-"
        binding.textAbsentCount.text = "-"

        authService.startMeasurement().enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                if (response.isSuccessful) {
                    response.body()?.ticketId?.let { startPollingForResult(it) }
                        ?: updateUiWithError("티켓 ID가 없습니다.")
                } else {
                    updateUiWithError("측정 시작 요청 실패")
                }
            }

            override fun onFailure(call: Call<TicketResponse>, t: Throwable) {
                updateUiWithError("네트워크 오류: ${t.message}")
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
                                "PENDING" -> pollingHandler.postDelayed(this, 2000)
                                else -> {
                                    updateUiWithError("측정에 실패했습니다.")
                                    stopPolling()
                                }
                            }
                        } else {
                            updateUiWithError("결과 조회 실패")
                            stopPolling()
                        }
                    }

                    override fun onFailure(call: Call<MeasureResultResponse>, t: Throwable) {
                        updateUiWithError("네트워크 오류: ${t.message}")
                        stopPolling()
                    }
                })
            }
        }
        pollingHandler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        pollingRunnable?.let { pollingHandler.removeCallbacks(it) }
    }

    private fun updateUiWithResult(result: MeasureResultResponse) {
        binding.textPresentCount.text = result.presentCount.toString()
        binding.textTotalCount.text = result.totalCount.toString()
        binding.textAbsentCount.text = result.absentCount.toString()
        Toast.makeText(context, "인원수 측정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
    }

    private fun updateUiWithError(message: String) {
        binding.textPresentCount.text = "오류"
        Log.e("HomeFragment", message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        countdownTimer?.cancel()
        _binding = null
    }
}
