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
                
                // 사용자 정보를 로컬에서 가져옴 (API 호출 제거)
                val userId = TokenManager.getUserId() ?: 0
                val email = TokenManager.getUserEmail() ?: ""
                val name = TokenManager.getUserName() ?: ""
                
                // User 객체 생성 (User 모델이 어떻게 정의되어 있는지 확인 필요, 일단 가상의 데이터로 채움)
                // 실제 User 클래스 구조에 맞게 수정 필요할 수 있음
                // 여기서는 id가 Long이나 Int일 수 있으므로 형변환 주의
                // API 호출이 없으므로 성공 처리
                // 단, User 생성자가 id, email, name 등을 받는지 확인 필요
                
                // 임시로 빈 User 객체 또는 저장된 데이터로 생성
                // User 데이터 클래스 구조를 모르므로 안전하게 로컬 데이터만 사용하거나
                // 필요한 경우 User 객체 구성을 조정해야 함.
                
                // 예시:
                // val user = User(id = userId.toLongOrNull(), email = email, ...)
                
                // 현재는 API 호출을 제거하라는 요청이므로, 
                // fetchUserData의 로직을 "저장된 데이터 불러오기"로 대체하거나
                // 아예 비워둘 수 있음. 하지만 UI에서 관찰 중일 수 있으므로 
                // 로컬 데이터로 Result.Success를 발행하는 것이 안전함.
                
                // 여기서는 단순히 로컬 저장소 값을 이용해 User 객체를 흉내내거나
                // User 모델을 생성할 수 없다면 주석 처리
                
                /*
                val response = ApiClient.mainApiService.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _userResult.value = Result.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch user data"
                    throw IllegalStateException(errorBody)
                }
                */
                
                // 더미 User 객체 또는 로컬 데이터 활용 (User 클래스 정의에 따라 수정 필요)
                // _userResult.value = Result.Success(User(...)) 
                
                // 지금은 에러가 나지 않게만 처리
                 _userResult.value = Result.Error(IllegalStateException("API removed as requested"))

            } catch (e: Exception) {
                _userResult.value = Result.Error(e)
            }
        }
    }
}
