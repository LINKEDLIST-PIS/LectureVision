package com.son.lecture_project.ui.home

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Notice
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
    
    private val _notices = MutableLiveData<Result<List<Notice>>>()
    val notices: LiveData<Result<List<Notice>>> = _notices

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

    // 티켓 상태 확인 함수 (홈 화면 로딩 시 호출)
    fun checkTicketStatus() {
        viewModelScope.launch {
            _ticketStatus.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                     _ticketStatus.value = Result.Error(Exception("로그인이 필요합니다."))
                    return@launch
                }
                val authToken = "Bearer $token"
                

                if (currentTicketId != null) {
                    validateTicket(authToken, currentTicketId!!)
                } else {
                    autoIssueTicket(authToken)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking ticket status", e)
                _ticketStatus.value = Result.Error(e)
            }
        }
    }
    
    private suspend fun autoIssueTicket(authToken: String) {
        try {
            val createResponse = RetrofitClient.ticketApiService.createTicket(authToken)
            
            if (createResponse.isSuccessful && createResponse.body() != null) {
                val newTicket = createResponse.body()!!
                Log.d("HomeViewModel", "Auto-issued ticket: ${newTicket.ticketId}")
                
                newTicket.userId?.let {
                    TokenManager.saveUserId(it)
                }
                loadHomeData()
                
                // 티켓 ID 저장
                currentTicketId = newTicket.ticketId
                
                // 발급 성공 시 "티켓 보유중" 상태 업데이트
                _ticketStatus.value = Result.Success("티켓 보유중 (자동 발급)")
                
            } else {
                val errorMsg = createResponse.errorBody()?.string() ?: "Unknown error"
                Log.e("HomeViewModel", "Auto-issue failed: $errorMsg")
                _ticketStatus.value = Result.Success("티켓 없음 (자동 발급 실패)")
            }
        } catch (e: Exception) {
             Log.e("HomeViewModel", "Auto-issue exception", e)
            _ticketStatus.value = Result.Success("티켓 없음 (발급 오류)")
        }
    }

    private suspend fun validateTicket(authToken: String, ticketId: String) {
        try {
            Log.d("HomeViewModel", "Validating ticket: $ticketId")
            val validationResponse = RetrofitClient.ticketApiService.validateTicket(
                authToken,
                ticketId
            )

            if (validationResponse.isSuccessful && validationResponse.body() != null) {
                val isValid = validationResponse.body()!!.isValid
                val message = validationResponse.body()!!.message
                Log.d("HomeViewModel", "Validation result: isValid=$isValid, msg=$message")
                
                // isValid가 true면 '검증됨', false면 '사용 대기중'으로 해석하여 표시
                val statusText = if (isValid) "티켓 유효함 (사용됨)" else "티켓 보유중 (사용 가능)"
                _ticketStatus.value = Result.Success(statusText)
            } else {
                // 검증 API 호출 실패 시에도 티켓 자체는 있으므로 기존 상태 유지하거나 경고 로그
                Log.e("HomeViewModel", "Validation API failed")
            }
        } catch (e: Exception) {
             Log.e("HomeViewModel", "Validation exception", e)
        }
    }


    fun startTimer(minutes: Int) {
        stopTimer()
        
        val durationInMillis = minutes * 60 * 1000L
        _isTimerRunning.value = true
        
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
            }
        }.start()
        

        startMeasurement()
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isTimerRunning.value = false
        _timerText.value = null
    }
    
    // 인원 측정 시작 함수
    fun startMeasurement() {
        viewModelScope.launch {
            _measurementResult.value = Result.Loading
            try {
                if (currentTicketId == null) {
                    // 티켓이 없으면 에러 처리
                    _measurementResult.value = Result.Error(IllegalStateException("유효한 티켓이 없습니다. 잠시 후 다시 시도해주세요."))
                    return@launch
                }
                
                // 모델 서버에 측정 요청
                val response = RetrofitClient.modelApiService.measure(currentTicketId!!)
                
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()!!.peopleCount
                    _measurementResult.value = Result.Success(count)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "측정 실패"
                    _measurementResult.value = Result.Error(Exception("인원 측정 오류: ${response.code()}"))
                }
            } catch (e: Exception) {
                _measurementResult.value = Result.Error(e)
            }
        }
    }

    fun loadHomeData() {
        loadNotices()
        loadTodayTimetable()
    }

    private fun loadNotices() {
        viewModelScope.launch {
            _notices.value = Result.Success(emptyList())
        }
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
