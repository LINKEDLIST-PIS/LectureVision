package com.son.lecture_project.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.SignUpRequest
import com.son.lecture_project.data.model.SignUpResponse
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val _signupResult = MutableLiveData<Result<SignUpResponse>>()
    val signupResult: LiveData<Result<SignUpResponse>> = _signupResult

    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch {
            _signupResult.value = Result.Loading
            try {
                // API 요청 시에는 name을 제외하고 email, password만 전송 (예제 준수)
                val request = SignUpRequest(email, password)
                val response = ApiClient.mainApiService.signup(request)

                if (response.isSuccessful && response.body() != null) {
                    // 회원가입 성공 시, 입력받은 이름을 로컬에 저장 (설정 화면 표시용)
                    TokenManager.saveUserName(name)
                    
                    _signupResult.value = Result.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown signup error"
                    throw IllegalStateException(errorBody)
                }
            } catch (e: Exception) {
                _signupResult.value = Result.Error(e)
            }
        }
    }
}
