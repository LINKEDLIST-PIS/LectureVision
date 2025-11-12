package com.son.lecture_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.api.AuthService
import com.son.lecture_project.data.api.MeasureResultResponse
import com.son.lecture_project.data.api.TicketResponse
import com.son.lecture_project.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val authService by lazy { ApiClient.instance.create(AuthService::class.java) }

    // Polling을 위한 Handler
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    // TimerActivity 결과를 처리하는 ActivityResultLauncher
    private val timerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // TimerActivity가 성공적으로 끝나면, 인원수 측정을 시작한다.
            startMeasurementProcess()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStartTimerActivity.setOnClickListener {
            val intent = Intent(activity, TimerActivity::class.java)
            timerActivityResultLauncher.launch(intent)
        }
    }

    /**
     * API 서버에 티켓 발급을 요청하는 것으로 측정 프로세스를 시작합니다.
     */
    private fun startMeasurementProcess() {
        binding.textPresentCount.text = "측정 중..."
        authService.startMeasurement().enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                if (response.isSuccessful) {
                    val ticketId = response.body()?.ticketId
                    if (ticketId != null) {
                        // 티켓 발급 성공 시, 2초 간격으로 결과 폴링 시작
                        startPollingForResult(ticketId)
                    } else {
                        updateUiWithError("티켓 발급에 실패했습니다.")
                    }
                } else {
                    updateUiWithError("측정 시작 요청 실패")
                }
            }

            override fun onFailure(call: Call<TicketResponse>, t: Throwable) {
                updateUiWithError("네트워크 오류: ${t.message}")
            }
        })
    }

    /**
     * ticketId를 사용하여 2초마다 측정 결과를 반복적으로 요청(Polling)합니다.
     */
    private fun startPollingForResult(ticketId: String) {
        pollingRunnable = object : Runnable {
            override fun run() {
                authService.getMeasurementResult(ticketId).enqueue(object : Callback<MeasureResultResponse> {
                    override fun onResponse(call: Call<MeasureResultResponse>, response: Response<MeasureResultResponse>) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            when (result?.status) {
                                "COMPLETED" -> {
                                    // 측정이 완료되면, UI를 업데이트하고 폴링을 중단한다.
                                    updateUiWithResult(result)
                                    stopPolling()
                                }
                                "PENDING" -> {
                                    // 아직 측정 중이면, 2초 후에 다시 시도한다.
                                    pollingHandler.postDelayed(this, 2000)
                                }
                                else -> {
                                    // FAILED 등 다른 상태 처리
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

    /**
     * 폴링을 중단합니다.
     */
    private fun stopPolling() {
        pollingRunnable?.let { pollingHandler.removeCallbacks(it) }
        pollingRunnable = null
    }

    /**
     * 측정 성공 시 결과를 UI에 업데이트합니다.
     */
    private fun updateUiWithResult(result: MeasureResultResponse) {
        binding.textPresentCount.text = result.presentCount.toString()
        binding.textTotalCount.text = result.totalCount.toString()
        binding.textAbsentCount.text = result.absentCount.toString()
        Toast.makeText(context, "인원수 측정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
    }

    /**
     * 오류 발생 시 UI를 업데이트하고 메시지를 표시합니다.
     */
    private fun updateUiWithError(message: String) {
        binding.textPresentCount.text = "-"
        Log.e("HomeFragment", message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling() // Fragment가 사라질 때 폴링도 반드시 중단
        _binding = null
    }
}
