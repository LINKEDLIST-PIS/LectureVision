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
                    
                    // 참고: /accounts/me 엔드포인트가 없으므로 추가 정보를 가져오지 않음.
                    // 회원가입 시 저장된 로컬 데이터(이름 등)를 사용하거나,
                    // 새로운 기기 로그인 시에는 이메일을 기반으로 표시합니다.

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
