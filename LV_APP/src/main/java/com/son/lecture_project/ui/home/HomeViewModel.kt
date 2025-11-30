package com.son.lecture_project.ui.home

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

// AndroidViewModel 상속으로 변경 (Context 사용을 위해)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

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

    // 비교 결과 알림
    private val _comparisonResult = MutableLiveData<Event<ComparisonResult>>()
    val comparisonResult: LiveData<Event<ComparisonResult>> = _comparisonResult

    private var countDownTimer: CountDownTimer? = null

    // 현재 유효한 티켓 ID 저장용
    private var currentTicketId: String? = null
    
    // 시작 시 측정값 저장용
    private var startCount: Int? = null

    // 시간표 로컬 데이터 접근용
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    private val KEY_TIMETABLE = "local_timetable_list"

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
    fun issueTicketAndMeasure(isStart: Boolean = false) {
        viewModelScope.launch {
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
                            
                            _measurementResult.value = Event(Result.Success(count))
                            _ticketStatus.value = Result.Success("측정 완료 (인원: $count)")
                            
                            if (isStart) {
                                startCount = count 
                            } else {
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
            _todayClasses.value = Result.Loading
            try {
                val allSchedules = getLocalSchedules()
                val todayName = getTodayDayName()
                
                // 오늘 요일이 포함된 수업만 필터링
                val todaySchedules = allSchedules.filter { it.day.contains(todayName) }
                    .sortedBy { it.startTime } // 시작 시간 순 정렬
                
                _todayClasses.value = Result.Success(todaySchedules)
            } catch (e: Exception) {
                _todayClasses.value = Result.Error(e)
            }
        }
    }
    
    private suspend fun getLocalSchedules(): List<ClassSchedule> {
        return withContext(Dispatchers.IO) {
            val json = prefs.getString(KEY_TIMETABLE, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<ClassSchedule>>() {}.type
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }
    
    private fun getTodayDayName(): String {
        // 한국 시간(KST) 기준으로 요일 계산
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
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
