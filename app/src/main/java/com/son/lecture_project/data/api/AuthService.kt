package com.son.lecture_project.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class EmailRequest(
    val email: String
)

interface AuthService {

    @Headers("Content-Type: application/json")
    @POST("accounts/login")
    fun loginUser(@Body request: LoginRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("accounts/signup")
    fun signupUser(@Body request: SignupRequest): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("accounts/send_verification_email")
    fun sendVerificationEmail(@Body request: EmailRequest): Call<ApiResponse>
}
