package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.LoginRequest
import com.son.lecture_project.data.model.LoginResponse
import com.son.lecture_project.data.model.SignUpRequest
import com.son.lecture_project.data.model.SignUpResponse
import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.data.model.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MainApiService {

    // Account
    @POST("/accounts/signup")
    suspend fun signup(@Body request: SignUpRequest): Response<SignUpResponse>

    @POST("/accounts/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>


    // Ticket
    @POST("/tickets/issue")
    suspend fun issueTicket(@Header("Authorization") token: String): Response<Ticket>

    // Upload
    @Multipart
    @POST("/upload")
    suspend fun upload(
        @Header("Authorization") token: String,
        @Header("X-Timestamp") timestamp: String,
        @Header("X-Signature") signature: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Part file: MultipartBody.Part,
        @Part("people_count") peopleCount: Int,
        @Part("client_id") clientId: String
    ): Response<Upload>

    @GET("/uploads")
    suspend fun getUploads(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<Upload>>
}
