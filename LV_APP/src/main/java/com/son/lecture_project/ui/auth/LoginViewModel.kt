package com.son.lecture_project.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.LoginRequest
import com.son.lecture_project.data.model.LoginResponse
import com.son.lecture_project.ui.home.Result // Reusing the Result wrapper from Home module
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            try {
                val request = LoginRequest(email, password)
                val response = ApiClient.mainApiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.accessToken
                    // Save the received token using TokenManager
                    TokenManager.saveToken(token)
                    _loginResult.value = Result.Success(response.body()!!)
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
