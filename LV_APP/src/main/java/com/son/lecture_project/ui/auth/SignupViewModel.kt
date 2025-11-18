package com.son.lecture_project.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.model.SignUpRequest
import com.son.lecture_project.data.model.SignUpResponse
import com.son.lecture_project.ui.home.Result // Reusing the Result wrapper
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val _signupResult = MutableLiveData<Result<SignUpResponse>>()
    val signupResult: LiveData<Result<SignUpResponse>> = _signupResult

    fun signup(email: String, password: String) {
        viewModelScope.launch {
            _signupResult.value = Result.Loading
            try {
                val request = SignUpRequest(email, password)
                val response = ApiClient.mainApiService.signup(request)

                if (response.isSuccessful && response.body() != null) {
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
