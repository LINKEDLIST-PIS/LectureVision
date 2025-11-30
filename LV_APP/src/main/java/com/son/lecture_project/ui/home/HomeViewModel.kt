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

// 데이터 변경 이벤트를 한 번만 처리하기 위한 Wrapper
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// 비교 결과를 전달하기 위한 데이터 클래스
data class ComparisonResult(
    val startCount: Int,
    val endCount: Int
)

class HomeViewModel : ViewModel() {

    // Event Wrapper 적용
    private val _measurementResult = MutableLiveData<Event<Result<Int>>>()
    val measurementResult: LiveData<Event<Result<Int>>> = _measurementResult

    private val _ticketStatus = MutableLiveData<Result<String>>()
    val ticketStatus: LiveData<Result<String>> = _ticketStatus
    
    private val _todayClasses = MutableLiveData<Result<List<ClassSchedule>>>()
    val todayClasses: LiveData<Result<List<ClassSchedule>>> = _todayClasses

    // Timer state
    private val _timerText = MutableLiveData<String?>()
    val timerText: LiveData<String?> = _timerText

    private val _isTimerRunning = MutableLiveData<Boolean>(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    // 비교 결과 알림 (여기도 Event 적용 고려 가능하나, 비교 결과는 다이얼로그라 괜찮을 수도 있음. 
    // 하지만 화면 회전 시 다이얼로그가 또 뜨는걸 막으려면 적용하는 게 좋음. 여기선 일단 유지하거나 적용)
    private val _comparisonResult = MutableLiveData<Event<ComparisonResult>>()
    val comparisonResult: LiveData<Event<ComparisonResult>> = _comparisonResult

    private var countDownTimer: CountDownTimer? = null

    // 현재 유효한 티켓 ID 저장용
    private var currentTicketId: String? = null
    
    // 시작 시 측정값 저장용
    private var startCount: Int? = null


    fun startTimer(minutes: Int) {
        stopTimer()
        
        startCount = null // 초기화
        
        val durationInMillis = minutes * 60 * 1000L
        _isTimerRunning.value = true
        _ticketStatus.value = Result.Success("측정 예약됨 (타이머 동작 중)")
        
        // [시작] 타이머 시작 즉시 측정 (isStart = true)
        issueTicketAndMeasure(isStart = true)
        
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
                
                // [종료] 타이머 종료 시 측정 (isStart = false)
                issueTicketAndMeasure(isStart = false)
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
    // isStart: true면 시작 측정, false면 종료 측정
    fun issueTicketAndMeasure(isStart: Boolean = false) {
        viewModelScope.launch {
            // 로딩 상태는 이벤트로 보낼 필요 없음 (UI 상태 표시용)
            // _measurementResult.value = Event(Result.Loading) 
            _ticketStatus.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    _measurementResult.value = Event(Result.Error(IllegalStateException("로그인이 필요합니다.")))
                    _ticketStatus.value = Result.Error(Exception("로그인 필요"))
                    return@launch
                }
                
                // 1. 티켓 발급 요청
                Log.d("HomeViewModel", "Requesting ticket creation...")
                val createResponse = RetrofitClient.ticketApiService.createTicket("Bearer $token")
                
                if (createResponse.isSuccessful && createResponse.body() != null) {
                    val ticket = createResponse.body()!!
                    val tId = ticket.ticketId
                    
                    if (tId.isNullOrEmpty()) {
                         _measurementResult.value = Event(Result.Error(Exception("발급된 티켓 ID가 유효하지 않습니다.")))
                         return@launch
                    }
                    
                    currentTicketId = tId
                    _ticketStatus.value = Result.Success("티켓 발급됨")
                    
                    // 2. 발급된 티켓으로 인원 측정 요청
                    try {
                        val measureResponse = RetrofitClient.modelApiService.measure(tId)
                        
                        if (measureResponse.isSuccessful && measureResponse.body() != null) {
                            val count = measureResponse.body()!!.peopleCount
                            
                            // 측정 성공 이벤트 발생
                            _measurementResult.value = Event(Result.Success(count))
                            
                            _ticketStatus.value = Result.Success("측정 완료 (인원: $count)")
                            
                            if (isStart) {
                                startCount = count // 시작 측정값 저장
                            } else {
                                // 종료 측정값일 경우, 시작값과 함께 결과 전송
                                val start = startCount ?: 0 
                                _comparisonResult.value = Event(ComparisonResult(start, count))
                            }
                            
                        } else {
                            _measurementResult.value = Event(Result.Error(Exception("인원 측정 오류: ${measureResponse.code()}")))
                        }
                    } catch (e: Exception) {
                        _measurementResult.value = Event(Result.Error(e))
                    }
                    
                } else {
                    _measurementResult.value = Event(Result.Error(Exception("티켓 발급 오류")))
                }
            } catch (e: Exception) {
                _measurementResult.value = Event(Result.Error(e))
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
