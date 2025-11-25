package com.son.lecture_project.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.User
import com.son.lecture_project.ui.home.Result // Reusing the Result wrapper
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _userResult = MutableLiveData<Result<User>>()
    val userResult: LiveData<Result<User>> = _userResult

    fun fetchUserData() {
        viewModelScope.launch {
            _userResult.value = Result.Loading
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    throw IllegalStateException("Token not found. User is not logged in.")
                }

                val response = ApiClient.mainApiService.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _userResult.value = Result.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch user data"
                    throw IllegalStateException(errorBody)
                }
            } catch (e: Exception) {
                _userResult.value = Result.Error(e)
            }
        }
    }
}
