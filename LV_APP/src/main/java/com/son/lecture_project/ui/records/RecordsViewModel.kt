package com.son.lecture_project.ui.records

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RecordsViewModel(application: Application) : AndroidViewModel(application) {

    private val _records = MutableLiveData<Result<List<Upload>>>()
    val records: LiveData<Result<List<Upload>>> = _records

    private val _subjectList = MutableLiveData<List<String>>()
    val subjectList: LiveData<List<String>> = _subjectList

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    private val KEY_TIMETABLE = "local_timetable_list"
    
    private var cachedSchedules: List<ClassSchedule> = emptyList()

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = Result.Loading
            try {
                // [Fix] TokenManager는 object이므로 init 후 정적 메서드로 접근
                TokenManager.init(getApplication())
                val token = TokenManager.getToken()
                
                if (token == null) {
                    _records.value = Result.Error(Exception("Token is null"))
                    return@launch
                }

                // 실제 API 호출
                val response = RetrofitClient.instance.getUploads(token)
                
                if (response.isSuccessful && response.body() != null) {
                    val uploads = response.body()!!
                    
                    // 시간표 데이터 로드 (매칭을 위해 필요)
                    if (cachedSchedules.isEmpty()) {
                        cachedSchedules = getLocalSchedules()
                    }

                    // 서버 데이터 보정 (originalName이 없거나 불명확할 경우 시간표 기반 매칭)
                    val processedRecords = uploads.map { record ->
                        val matchedSubject = findSubjectByTime(record.uploadedAt, cachedSchedules)
                        // originalName이 비어있거나 기본 파일명인 경우 매칭된 과목명으로 대체
                        if (matchedSubject != null && (record.originalName.isNullOrEmpty() || record.originalName!!.startsWith("photo"))) {
                            record.copy(originalName = matchedSubject)
                        } else {
                            record
                        }
                    }
                    _records.value = Result.Success(processedRecords)
                } else {
                    _records.value = Result.Error(Exception("Failed to load records: ${response.code()}"))
                }
            } catch (e: Exception) {
                _records.value = Result.Error(e)
            }
        }
    }
    
    // [Helper] 타임스탬프를 기반으로 시간표에서 과목 찾기
    private fun findSubjectByTime(uploadedAt: String?, schedules: List<ClassSchedule>): String? {
        if (uploadedAt == null) return null
        
        try {
            // 1. 날짜 파싱 (UTC -> Date)
            val cleanDate = uploadedAt.replace("Z", "")
            val format = if (cleanDate.contains("T")) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
            
            val parser = SimpleDateFormat(format, Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC") 
            val date = parser.parse(cleanDate) ?: return null
            
            // 2. KST 기준으로 요일 및 시간 확인
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            cal.time = date
            
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            
            val dayString = when (dayOfWeek) {
                Calendar.MONDAY -> "월"
                Calendar.TUESDAY -> "화"
                Calendar.WEDNESDAY -> "수"
                Calendar.THURSDAY -> "목"
                Calendar.FRIDAY -> "금"
                Calendar.SATURDAY -> "토"
                Calendar.SUNDAY -> "일"
                else -> ""
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val recordTimeStr = timeFormat.format(date)
            
            // 3. 매칭 (시간 충돌 해결: Start <= Time < End)
            val matchedSchedule = schedules.find { schedule ->
                if (!schedule.day.contains(dayString)) return@find false
                
                val start = schedule.startTime ?: "00:00"
                val end = schedule.endTime ?: "23:59"
                
                recordTimeStr >= start && recordTimeStr < end
            }
            
            return matchedSchedule?.name
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    fun loadSubjectList() {
        viewModelScope.launch {
            val schedules = getLocalSchedules()
            cachedSchedules = schedules
            _subjectList.value = cachedSchedules.map { it.name }.distinct().sorted()
        }
    }
    
    fun getSchedulesForSubject(subjectName: String): List<ClassSchedule> {
        return cachedSchedules.filter { it.name == subjectName }
    }
    
    fun getAllSchedules(): List<ClassSchedule> {
        return cachedSchedules
    }

    fun getSubjectTotalCountMap(): Map<String, Int> {
        return cachedSchedules.groupBy { it.name }
            .mapValues { (_, list) -> list.maxOfOrNull { it.totalStudents } ?: 0 }
    }
    
    fun getSubjectColorMap(): Map<String, String> {
        return cachedSchedules.associate { it.name to (it.color ?: "#CCCCCC") }
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
}
