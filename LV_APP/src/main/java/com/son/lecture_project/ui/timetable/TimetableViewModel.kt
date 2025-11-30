package com.son.lecture_project.ui.timetable

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// AndroidViewModel을 상속받아 Application Context 사용
class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val _timetable = MutableLiveData<Result<List<ClassSchedule>>>()
    val timetable: LiveData<Result<List<ClassSchedule>>> = _timetable

    private val _actionResult = MutableLiveData<Result<String>>()
    val actionResult: LiveData<Result<String>> = _actionResult

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    private val KEY_TIMETABLE = "local_timetable_list"

    init {
        // ViewModel 생성 시 자동으로 데이터 로드 및 검증(잘못된 시간 삭제) 수행
        loadTimetable()
    }

    fun loadTimetable() {
        viewModelScope.launch {
            _timetable.value = Result.Loading
            try {
                // 1. 로컬 시간표 불러오기
                val list = getLocalTimetable().toMutableList()
                
                // 2. 유효하지 않은 시간(09:00 이전)의 수업이 있는지 확인하고 삭제
                val invalidClasses = list.filter { isTimeTooEarly(it.startTime) }
                
                if (invalidClasses.isNotEmpty()) {
                    list.removeAll(invalidClasses)
                    saveLocalTimetable(list) // 삭제된 상태로 다시 저장 (영구 삭제)
                }
                
                _timetable.value = Result.Success(list)
            } catch (e: Exception) {
                _timetable.value = Result.Error(e)
            }
        }
    }

    fun addClass(name: String, day: String, startTime: String, endTime: String, classroom: String?, color: String?, totalStudents: Int) {
        viewModelScope.launch {
            _actionResult.value = Result.Loading
            try {
                // 09:00 이전 시간 체크 (이중 방어)
                if (isTimeTooEarly(startTime)) {
                    _actionResult.value = Result.Error(Exception("수업 시작 시간은 09:00 이후여야 합니다."))
                    return@launch
                }

                val currentList = getLocalTimetable().toMutableList()
                
                // 새 ID 생성 (가장 큰 ID + 1)
                val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
                
                val newClass = ClassSchedule(
                    id = newId,
                    name = name,
                    day = day,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = classroom,
                    color = color ?: "#FF6B6B", // 기본값 빨강
                    totalStudents = totalStudents
                )
                
                currentList.add(newClass)
                saveLocalTimetable(currentList)
                
                _actionResult.value = Result.Success("수업이 추가되었습니다.")
                loadTimetable() // 목록 갱신
            } catch (e: Exception) {
                _actionResult.value = Result.Error(e)
            }
        }
    }

    fun updateClass(id: Int, name: String, day: String, startTime: String, endTime: String, classroom: String?, color: String?, totalStudents: Int) {
        viewModelScope.launch {
            _actionResult.value = Result.Loading
            try {
                // 09:00 이전 시간 체크
                if (isTimeTooEarly(startTime)) {
                    _actionResult.value = Result.Error(Exception("수업 시작 시간은 09:00 이후여야 합니다."))
                    return@launch
                }

                val currentList = getLocalTimetable().toMutableList()
                val index = currentList.indexOfFirst { it.id == id }
                
                if (index != -1) {
                    val updatedClass = currentList[index].copy(
                        name = name,
                        day = day,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom,
                        color = color,
                        totalStudents = totalStudents
                    )
                    currentList[index] = updatedClass
                    saveLocalTimetable(currentList)
                    
                    _actionResult.value = Result.Success("수업이 수정되었습니다.")
                    loadTimetable()
                } else {
                    _actionResult.value = Result.Error(Exception("해당 수업을 찾을 수 없습니다."))
                }
            } catch (e: Exception) {
                _actionResult.value = Result.Error(e)
            }
        }
    }

    fun deleteClass(id: Int) {
        viewModelScope.launch {
            _actionResult.value = Result.Loading
            try {
                val currentList = getLocalTimetable().toMutableList()
                val removed = currentList.removeIf { it.id == id }
                
                if (removed) {
                    saveLocalTimetable(currentList)
                    _actionResult.value = Result.Success("수업이 삭제되었습니다.")
                    loadTimetable()
                } else {
                    _actionResult.value = Result.Error(Exception("삭제할 수업이 없습니다."))
                }
            } catch (e: Exception) {
                _actionResult.value = Result.Error(e)
            }
        }
    }
    
    // 09:00 이전인지 확인하는 헬퍼 함수
    private fun isTimeTooEarly(timeStr: String): Boolean {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            // 9시보다 작으면(0~8시) true 반환
            hour < 9
        } catch (e: Exception) {
            false
        }
    }
    
    // --- Local Storage Helpers (SharedPreferences + Gson) ---
    
    private suspend fun getLocalTimetable(): List<ClassSchedule> {
        return withContext(Dispatchers.IO) {
            val json = prefs.getString(KEY_TIMETABLE, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<ClassSchedule>>() {}.type
                gson.fromJson(json, type)
            }
        }
    }
    
    private suspend fun saveLocalTimetable(list: List<ClassSchedule>) {
        withContext(Dispatchers.IO) {
            val json = gson.toJson(list)
            prefs.edit().putString(KEY_TIMETABLE, json).apply()
        }
    }
}
