package com.son.lecture_project.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

// --- 데이터 모델들 (변경 없음) ---
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
data class TicketResponse(
    val ticketId: String
)
data class MeasureResultResponse(
    val status: String,
    val presentCount: Int,
    val totalCount: Int,
    val absentCount: Int
)

/**
 * 서버와 통신하기 위한 API 인터페이스
 * 실제 서버의 엔드포인트에 맞게 모두 수정되었습니다.
 */
interface AuthService {

    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/signup")
    fun signupUser(@Body request: SignupRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/send-verification-email")
    fun sendVerificationEmail(@Body request: EmailRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/measurement/start")
    fun startMeasurement(): Call<TicketResponse>

    @GET("api/v1/measurement/result/{ticketId}")
    fun getMeasurementResult(@Path("ticketId") ticketId: String): Call<MeasureResultResponse>
}
