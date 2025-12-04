package com.son.lecture_project.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.User
import com.son.lecture_project.ui.home.Result
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

                // 사용되지 않는 변수 제거
                // TokenManager.getUserId() ?: 0
                // TokenManager.getUserEmail() ?: ""
                // TokenManager.getUserName() ?: ""
                
                _userResult.value = Result.Error(IllegalStateException("User data fetch logic is currently unavailable."))

            } catch (e: Exception) {
                _userResult.value = Result.Error(e)
            }
        }
    }
}
