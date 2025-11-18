package com.son.lecture_project.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.ApiClient
import com.son.lecture_project.data.local.TokenManager
import kotlinx.coroutines.launch

/**
 * A wrapper class for representing UI states (loading, success, error).
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class HomeViewModel : ViewModel() {

    private val _measurementResult = MutableLiveData<Result<Int>>()
    val measurementResult: LiveData<Result<Int>> = _measurementResult

    fun startMeasurement() {
        viewModelScope.launch {
            _measurementResult.value = Result.Loading
            try {
                // Retrieve the saved token from TokenManager
                val token = TokenManager.getToken()
                if (token == null) {
                    throw IllegalStateException("Authentication token not found. Please log in again.")
                }
                val authToken = "Bearer $token"

                // 1. Issue a ticket from the main API server
                val ticketResponse = ApiClient.mainApiService.issueTicket(authToken)
                if (!ticketResponse.isSuccessful || ticketResponse.body() == null) {
                    throw IllegalStateException("Failed to issue a ticket: ${ticketResponse.errorBody()?.string()}")
                }
                val ticket = ticketResponse.body()!!.ticket

                // 2. Request measurement from the model server using the ticket
                val measureResponse = ApiClient.modelApiService.measure(ticket)
                if (!measureResponse.isSuccessful || measureResponse.body() == null) {
                    throw IllegalStateException("Failed to get measurement: ${measureResponse.errorBody()?.string()}")
                }

                // 3. Post the successful result
                val peopleCount = measureResponse.body()!!.peopleCount
                _measurementResult.value = Result.Success(peopleCount)

            } catch (e: Exception) {
                _measurementResult.value = Result.Error(e)
            }
        }
    }
}
