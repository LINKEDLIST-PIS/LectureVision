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
import com.son.lecture_project.service.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

data class ComparisonResult(
    val startCount: Int,
    val endCount: Int
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _measurementResult = MutableLiveData<Event<Result<Int>>>()
    val measurementResult: LiveData<Event<Result<Int>>> = _measurementResult

    private val _ticketStatus = MutableLiveData<Result<String>>()
    val ticketStatus: LiveData<Result<String>> = _ticketStatus
    
    private val _todayClasses = MutableLiveData<Result<List<ClassSchedule>>>()
    val todayClasses: LiveData<Result<List<ClassSchedule>>> = _todayClasses

    private val _timerText = MutableLiveData<String?>()
    val timerText: LiveData<String?> = _timerText

    private val _isTimerRunning = MutableLiveData<Boolean>(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    private val _comparisonResult = MutableLiveData<Event<ComparisonResult>>()
    val comparisonResult: LiveData<Event<ComparisonResult>> = _comparisonResult

    private var countDownTimer: CountDownTimer? = null
    private var startCount: Int? = null

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    private val KEY_TIMETABLE = "local_timetable_list"

    fun startTimer(minutes: Int) {
        if (_isTimerRunning.value == true) return

        _isTimerRunning.value = true
        startCount = null
        _ticketStatus.value = Result.Success("초기 측정 시작...")

        issueTicketAndMeasure(isStart = true)

        val durationInMillis = minutes * 60 * 1000L
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
    }

    fun stopTimer() {
        val wasRunning = _isTimerRunning.value == true
        
        countDownTimer?.cancel()
        countDownTimer = null
        _isTimerRunning.value = false
        _timerText.value = null

        if (wasRunning && startCount != null) {
            issueTicketAndMeasure(isStart = false)
        }
    }
    
    fun issueTicketAndMeasure(isStart: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _ticketStatus.value = if (isStart) Result.Loading else Result.Success("종료 측정 중...")
            }
            
            try {
                if (!TokenManager.isTokenValid()) {
                    throw IllegalStateException("로그인이 필요하거나 토큰이 만료되었습니다.")
                }
                val token = TokenManager.getToken()!!
                
                Log.d("HomeViewModel", "Requesting ticket...")
                val createResponse = RetrofitClient.ticketApiService.createTicket("Bearer $token")
                
                if (!createResponse.isSuccessful || createResponse.body() == null) {
                    throw Exception("티켓 발급 오류: ${createResponse.code()}")
                }

                val ticket = createResponse.body()!!
                val tId = ticket.ticketId
                
                if (tId.isNullOrEmpty()) {
                    throw Exception("발급된 티켓 ID가 유효하지 않습니다.")
                }
                
                TokenManager.saveTicket(tId)
                Log.d("HomeViewModel", "Ticket issued. Measuring count...")

                val measureResponse = RetrofitClient.modelApiService.measure(tId)
                TokenManager.useTicket()

                if (!measureResponse.isSuccessful || measureResponse.body() == null) {
                    throw Exception("인원 측정 오류: ${measureResponse.code()}")
                }

                val count = measureResponse.body()!!.peopleCount
                
                withContext(Dispatchers.Main) {
                    _measurementResult.value = Event(Result.Success(count))
                    
                    if (isStart) {
                        startCount = count
                        _ticketStatus.value = Result.Success("초기 측정 완료 (인원: $count)")
                        if (_isTimerRunning.value == false) {
                           return@withContext
                        }
                    } else {
                        val start = startCount ?: 0
                        _comparisonResult.value = Event(ComparisonResult(start, count))
                        _ticketStatus.value = Result.Success("최종 측정 완료 (인원: $count)")
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in issueTicketAndMeasure", e)
                withContext(Dispatchers.Main) {
                     if(isStart) {
                        stopTimer()
                     }
                     _ticketStatus.value = Result.Error(e)
                    _measurementResult.value = Event(Result.Error(e))
                }
            }
        }
    }

    // New function for testing ticket issuance ONLY
    fun testTicketIssuance() {
        viewModelScope.launch {
            _ticketStatus.value = Result.Loading
            try {
                if (!TokenManager.isTokenValid()) {
                    throw IllegalStateException("로그인이 필요하거나 토큰이 만료되었습니다.")
                }
                val token = TokenManager.getToken()!!

                Log.d("HomeViewModel", "Requesting ticket for testing...")
                val createResponse = RetrofitClient.ticketApiService.createTicket("Bearer $token")

                if (createResponse.isSuccessful && createResponse.body() != null) {
                    val ticket = createResponse.body()!!
                    val tId = ticket.ticketId

                    if (tId.isNullOrEmpty()) {
                        throw Exception("발급된 테스트 티켓 ID가 유효하지 않습니다.")
                    }

                    TokenManager.saveTicket(tId)
                    
                    withContext(Dispatchers.Main) {
                        _ticketStatus.value = Result.Success("테스트 티켓 발급 성공 (5분 유효)")
                    }
                    Log.d("HomeViewModel", "Test ticket issued successfully: $tId")

                } else {
                    throw Exception("테스트 티켓 발급 오류: ${createResponse.code()}")
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in testTicketIssuance", e)
                withContext(Dispatchers.Main) {
                    _ticketStatus.value = Result.Error(e)
                }
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
                
                // 시간표 로드 시 알람 스케줄링도 함께 수행
                if (allSchedules.isNotEmpty()) {
                    AlarmScheduler.scheduleClassAlarms(getApplication(), allSchedules)
                }
                
                val todayName = getTodayDayName()
                val todaySchedules = allSchedules.filter { it.day.contains(todayName) }.sortedBy { it.startTime } 
                
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
        countDownTimer?.cancel()
    }
}
