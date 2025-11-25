package com.son.lecture_project.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.LoginRequest
import com.son.lecture_project.data.model.LoginResponse
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            try {
                val request = LoginRequest(email, password)
                // Use RetrofitClient.mainApiService for consistency
                val response = RetrofitClient.mainApiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!
                    val token = loginData.accessToken
                    
                    // 1. Save Token
                    TokenManager.saveToken(token)
                    TokenManager.saveUserEmail(email)
                    
                    // 2. Fetch User ID (getMe) and Save
                    try {
                        val userResponse = RetrofitClient.mainApiService.getMe("Bearer $token")
                        if (userResponse.isSuccessful && userResponse.body() != null) {
                            val user = userResponse.body()!!
                            // User.id is Int, convert to String and save
                            TokenManager.saveUserId(user.id.toString())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    _loginResult.value = Result.Success(loginData)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown login error"
                    throw IllegalStateException(errorBody)
                }
            } catch (e: Exception) {
                _loginResult.value = Result.Error(e)
            }
        }
    }
}
