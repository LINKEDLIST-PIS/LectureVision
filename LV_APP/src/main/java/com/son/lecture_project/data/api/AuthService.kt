package com.son.lecture_project.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

// --- 기존 데이터 클래스 ---
data class LoginRequest(
    val email: String,
    val password: String
)
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class EmailRequest(
    val email: String
)

// --- 인원수 측정을 위해 새로 추가된 데이터 클래스 ---

// 티켓 발급 응답
data class TicketResponse(
    val ticketId: String
)

// 측정 결과 응답
data class MeasureResultResponse(
    val status: String, // "PENDING", "COMPLETED", "FAILED"
    val presentCount: Int,
    val totalCount: Int,
    val absentCount: Int
)

interface AuthService {

    // --- 기존 함수들 ---
    @Headers("Content-Type: application/json")
    @POST("accounts/login")
    fun loginUser(@Body request: LoginRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("accounts/signup")
    fun signupUser(@Body request: SignupRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("accounts/send_verification_email")
    fun sendVerificationEmail(@Body request: EmailRequest): Call<ApiResponse>

    // --- 인원수 측정을 위해 새로 추가된 함수들 ---

    /**
     * API 서버에 인원수 측정을 요청하고 티켓을 발급받습니다.
     */
    @Headers("Content-Type: application/json")
    @POST("measurement/start")
    fun startMeasurement(): Call<TicketResponse>

    /**
     * 발급받은 티켓으로 측정 결과를 조회합니다.
     * @param ticketId startMeasurement()를 통해 받은 티켓 ID
     */
    @GET("measurement/result/{ticketId}")
    fun getMeasurementResult(@Path("ticketId") ticketId: String): Call<MeasureResultResponse>
}
