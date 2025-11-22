package com.son.lecture_project.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.ClassScheduleRequest
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.launch

class TimetableViewModel : ViewModel() {

    private val _timetable = MutableLiveData<Result<List<ClassSchedule>>>()
    val timetable: LiveData<Result<List<ClassSchedule>>> = _timetable

    private val _actionResult = MutableLiveData<Result<String>>()
    val actionResult: LiveData<Result<String>> = _actionResult

    fun loadTimetable() {
        viewModelScope.launch {
            _timetable.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                val userId = TokenManager.getUserId()

                if (token == null || userId == null) {
                    _timetable.value = Result.Error(IllegalStateException("로그인 정보가 없습니다."))
                    return@launch
                }

                val response = RetrofitClient.timetableApiService.getUserTimetable("Bearer $token", userId)
                if (response.isSuccessful && response.body() != null) {
                    _timetable.value = Result.Success(response.body()!!)
                } else {
                    _timetable.value = Result.Error(Exception("시간표 조회 실패: ${response.code()}"))
                }
            } catch (e: Exception) {
                _timetable.value = Result.Error(e)
            }
        }
    }

    fun addClass(name: String, day: String, startTime: String, endTime: String, classroom: String?, color: String?) {
        viewModelScope.launch {
            _actionResult.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                val userId = TokenManager.getUserId()

                if (token == null || userId == null) {
                    _actionResult.value = Result.Error(IllegalStateException("로그인 정보가 없습니다."))
                    return@launch
                }

                val request = ClassScheduleRequest(
                    name = name,
                    day = day,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = classroom,
                    color = color,
                    userId = userId
                )

                val response = RetrofitClient.timetableApiService.createClass("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _actionResult.value = Result.Success("수업이 추가되었습니다.")
                    loadTimetable() // Refresh list
                } else {
                    _actionResult.value = Result.Error(Exception("추가 실패: ${response.code()}"))
                }
            } catch (e: Exception) {
                _actionResult.value = Result.Error(e)
            }
        }
    }

    fun updateClass(id: Int, name: String, day: String, startTime: String, endTime: String, classroom: String?, color: String?) {
        viewModelScope.launch {
            _actionResult.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                val userId = TokenManager.getUserId() ?: "" // Not used in body but consistent

                if (token == null) {
                    _actionResult.value = Result.Error(IllegalStateException("로그인 필요"))
                    return@launch
                }

                val request = ClassScheduleRequest(
                    name = name,
                    day = day,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = classroom,
                    color = color,
                    userId = userId
                )

                val response = RetrofitClient.timetableApiService.updateClass("Bearer $token", id, request)
                if (response.isSuccessful) {
                    _actionResult.value = Result.Success("수업이 수정되었습니다.")
                    loadTimetable()
                } else {
                    _actionResult.value = Result.Error(Exception("수정 실패: ${response.code()}"))
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
                val token = TokenManager.getToken()
                if (token == null) {
                    _actionResult.value = Result.Error(IllegalStateException("로그인 필요"))
                    return@launch
                }

                val response = RetrofitClient.timetableApiService.deleteClass("Bearer $token", id)
                if (response.isSuccessful) {
                    _actionResult.value = Result.Success("수업이 삭제되었습니다.")
                    loadTimetable()
                } else {
                    _actionResult.value = Result.Error(Exception("삭제 실패: ${response.code()}"))
                }
            } catch (e: Exception) {
                _actionResult.value = Result.Error(e)
            }
        }
    }
}
