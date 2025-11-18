package com.son.lecture_project.ui.records

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.ui.home.Result // Reusing the Result wrapper
import kotlinx.coroutines.launch

class RecordsViewModel : ViewModel() {

    private val _recordsResult = MutableLiveData<Result<List<Upload>>>()
    val recordsResult: LiveData<Result<List<Upload>>> = _recordsResult

    // TODO: The API currently doesn't support filtering. These parameters are placeholders.
    // The endpoint in MainApiService needs to be updated to accept these.
    fun fetchRecords(classId: String? = null, dateFilter: String? = null) {
        viewModelScope.launch {
            _recordsResult.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    throw IllegalStateException("Token not found. User is not logged in.")
                }

                // The getUploads method needs to be updated to accept filter parameters.
                val response = ApiClient.mainApiService.getUploads("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _recordsResult.value = Result.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch records"
                    throw IllegalStateException(errorBody)
                }
            } catch (e: Exception) {
                _recordsResult.value = Result.Error(e)
            }
        }
    }
}
