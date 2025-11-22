package com.son.lecture_project.ui.ticket

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.ui.home.Result
import kotlinx.coroutines.launch

class TicketViewModel : ViewModel() {

    private val _ticketResult = MutableLiveData<Result<Ticket>>()
    val ticketResult: LiveData<Result<Ticket>> = _ticketResult

    fun issueTicket() {
        _ticketResult.value = Result.Loading
        
        val token = TokenManager.getToken()
        if (token.isNullOrEmpty()) {
            _ticketResult.value = Result.Error(IllegalStateException("로그인이 필요합니다."))
            return
        }

        viewModelScope.launch {
            try {
                val authHeader = "Bearer $token"
                
                // 변경된 createTicket: Body 없이 Header(token)만 전달
                val response = RetrofitClient.ticketApiService.createTicket(authHeader)

                if (response.isSuccessful && response.body() != null) {
                    val ticket = response.body()!!
                    Log.d("TicketViewModel", "Ticket issued: $ticket") // 로그 추가
                    
                    // Ticket fields are now nullable, so check for null
                    ticket.userId?.let {
                        TokenManager.saveUserId(it)
                    }
                    _ticketResult.value = Result.Success(ticket)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "내용 없음"
                    val errorMsg = "실패(${response.code()}): $errorBody"
                    Log.e("TicketViewModel", errorMsg) // 로그 추가
                    _ticketResult.value = Result.Error(IllegalStateException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("TicketViewModel", "Exception during ticket issue", e) // 로그 추가
                _ticketResult.value = Result.Error(e)
            }
        }
    }
}
