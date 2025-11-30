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

class RecordsViewModel(application: Application) : AndroidViewModel(application) {

    private val _records = MutableLiveData<Result<List<Upload>>>()
    val records: LiveData<Result<List<Upload>>> = _records

    // 과목 목록 LiveData (필터용)
    private val _subjectList = MutableLiveData<List<String>>()
    val subjectList: LiveData<List<String>> = _subjectList

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    private val KEY_TIMETABLE = "local_timetable_list"
    
    // 필터링을 위해 로드된 전체 시간표 데이터를 메모리에 캐시
    private var cachedSchedules: List<ClassSchedule> = emptyList()

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    _records.value = Result.Error(IllegalStateException("로그인이 필요합니다."))
                    return@launch
                }

                val response = RetrofitClient.recordApiService.getRecords("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    _records.value = Result.Success(response.body()!!)
                } else {
                    _records.value = Result.Error(Exception("기록 조회 실패: ${response.code()}"))
                }
            } catch (e: Exception) {
                _records.value = Result.Error(e)
            }
        }
    }
    
    // 시간표에서 과목 목록 로드 (필터 목록 갱신 및 데이터 캐싱)
    fun loadSubjectList() {
        viewModelScope.launch {
            val schedules = getLocalSchedules()
            cachedSchedules = schedules
            // 과목명 추출, 중복 제거, 정렬
            _subjectList.value = schedules.map { it.name }.distinct().sorted()
        }
    }
    
    // 특정 과목의 시간표 정보(요일, 시간 등) 반환
    fun getSchedulesForSubject(subjectName: String): List<ClassSchedule> {
        return cachedSchedules.filter { it.name == subjectName }
    }

    // 과목별 총 인원수 맵 반환
    fun getSubjectTotalCountMap(): Map<String, Int> {
        return cachedSchedules.groupBy { it.name }
            .mapValues { (_, list) -> list.maxOfOrNull { it.totalStudents } ?: 0 }
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
