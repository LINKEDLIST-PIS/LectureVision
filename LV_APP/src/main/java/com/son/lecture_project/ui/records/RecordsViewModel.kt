package com.son.lecture_project.ui.records

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.launch

class RecordsViewModel : ViewModel() {

    private val _records = MutableLiveData<Result<List<Upload>>>()
    val records: LiveData<Result<List<Upload>>> = _records

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    _records.value = Result.Error(IllegalStateException("로그인이 필요합니다."))
                    return@launch
                }

                // skip, limit는 기본값 사용 (0, 50)
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
}
