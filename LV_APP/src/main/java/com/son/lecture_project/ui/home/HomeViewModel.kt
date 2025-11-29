package com.son.lecture_project.ui.home

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale


sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class HomeViewModel : ViewModel() {

    private val _measurementResult = MutableLiveData<Result<Int>>()
    val measurementResult: LiveData<Result<Int>> = _measurementResult

    private val _ticketStatus = MutableLiveData<Result<String>>()
    val ticketStatus: LiveData<Result<String>> = _ticketStatus
    
    private val _todayClasses = MutableLiveData<Result<List<ClassSchedule>>>()
    val todayClasses: LiveData<Result<List<ClassSchedule>>> = _todayClasses

    // Timer state
    private val _timerText = MutableLiveData<String?>()
    val timerText: LiveData<String?> = _timerText

    private val _isTimerRunning = MutableLiveData<Boolean>(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    private var countDownTimer: CountDownTimer? = null

    // 현재 유효한 티켓 ID 저장용
    private var currentTicketId: String? = null


    fun startTimer(minutes: Int) {
        stopTimer()
        
        val durationInMillis = minutes * 60 * 1000L
        _isTimerRunning.value = true
        _ticketStatus.value = Result.Success("측정 예약됨 (타이머 동작 중)")
        
        countDownTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val min = totalSeconds / 60
                val sec = totalSeconds % 60
                _timerText.postValue(String.format(Locale.getDefault(), "%02d:%02d", min, sec))
            }

            override fun onFinish() {
                _timerText.postValue("00:00")
                stopTimer()
                
                // 타이머 종료 시 티켓 발급 및 측정 시작
                issueTicketAndMeasure()
            }
        }.start()
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isTimerRunning.value = false
        _timerText.value = null
    }
    
    // 타이머 종료 후 티켓 발급 및 측정 수행 함수
    // public으로 변경하여 외부(SettingsScreen 등)에서 호출 가능하도록 함
    fun issueTicketAndMeasure() {
        viewModelScope.launch {
            _measurementResult.value = Result.Loading
            _ticketStatus.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    _measurementResult.value = Result.Error(IllegalStateException("로그인이 필요합니다."))
                    _ticketStatus.value = Result.Error(Exception("로그인 필요"))
                    return@launch
                }
                
                // 1. 티켓 발급 요청
                Log.d("HomeViewModel", "Requesting ticket creation...")
                val createResponse = RetrofitClient.ticketApiService.createTicket("Bearer $token")
                
                Log.d("HomeViewModel", "Create Ticket Code: ${createResponse.code()}")
                
                if (createResponse.isSuccessful && createResponse.body() != null) {
                    val ticket = createResponse.body()!!
                    
                    // [DEBUG] 티켓 응답 전체 내용 출력
                    Log.d("HomeViewModel", "Ticket Response Body: $ticket")
                    Log.d("HomeViewModel", "Ticket JSON: ${Gson().toJson(ticket)}")
                    
                    val tId = ticket.ticketId
                    Log.d("HomeViewModel", "Extracted Ticket ID: $tId")
                    
                    // ticketId가 null인지 확인
                    if (tId.isNullOrEmpty()) {
                         _measurementResult.value = Result.Error(Exception("발급된 티켓 ID가 유효하지 않습니다."))
                         _ticketStatus.value = Result.Error(Exception("티켓 ID 오류 (Data: $ticket)"))
                         return@launch
                    }
                    
                    currentTicketId = tId
                    Log.d("HomeViewModel", "Ticket issued successfully: $currentTicketId")
                    
                    // 티켓 발급은 성공했음을 알림 (일단 이 시점에 성공 상태로 업데이트)
                    _ticketStatus.value = Result.Success("티켓 발급됨 (측정 서버 연결 시도 중...)")
                    
                    // 2. 발급된 티켓으로 인원 측정 요청 (별도 try-catch로 감싸서 티켓 발급 성공을 유지)
                    try {
                        val measureResponse = RetrofitClient.modelApiService.measure(tId)
                        
                        if (measureResponse.isSuccessful && measureResponse.body() != null) {
                            val count = measureResponse.body()!!.peopleCount
                            _measurementResult.value = Result.Success(count)
                            _ticketStatus.value = Result.Success("측정 완료 (인원: $count)")
                        } else {
                            val errorMsg = measureResponse.errorBody()?.string() ?: "측정 실패"
                            Log.e("HomeViewModel", "Measure Error: $errorMsg")
                            // 측정 실패여도 티켓은 발급되었으므로 '티켓 발급됨' 상태는 유지하되 메시지만 변경
                            _measurementResult.value = Result.Error(Exception("인원 측정 오류: ${measureResponse.code()}"))
                            _ticketStatus.value = Result.Success("티켓 발급됨 (측정 실패)")
                        }
                    } catch (e: Exception) {
                        // 모델 서버 연결 실패 (서버 미운영 등)
                        Log.e("HomeViewModel", "Measure Server Error: ${e.localizedMessage}")
                        _measurementResult.value = Result.Error(e)
                        // ★ 핵심 수정: 측정 서버 오류가 나도 티켓 발급은 성공했으므로 Success 상태 유지
                        _ticketStatus.value = Result.Success("티켓 발급됨 (측정 서버 미운영)")
                    }
                    
                } else {
                    val errorMsg = createResponse.errorBody()?.string() ?: "티켓 발급 실패"
                    Log.e("HomeViewModel", "Ticket Create Error Body: $errorMsg")
                    _measurementResult.value = Result.Error(Exception("티켓 발급 오류: $errorMsg"))
                    _ticketStatus.value = Result.Error(Exception("티켓 발급 실패: ${createResponse.code()}"))
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception during issueTicketAndMeasure", e)
                _measurementResult.value = Result.Error(e)
                _ticketStatus.value = Result.Error(e)
            }
        }
    }

    fun loadHomeData() {
        loadTodayTimetable()
    }

    private fun loadTodayTimetable() {
        viewModelScope.launch {
            _todayClasses.value = Result.Success(emptyList())
        }
    }
    
    private fun getTodayDayName(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
