
package com.son.lecture_project.data.api

import com.son.lecture_project.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Account
    @POST("/accounts/signup")
    suspend fun signup(@Body request: SignUpRequest): Response<SignUpResponse>

    @GET("/accounts/verify")
    suspend fun verifyEmail(@Query("token") token: String): Response<Unit>

    @POST("/accounts/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/accounts/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<User>

    // Ticket
    @POST("/tickets/issue")
    suspend fun issueTicket(@Header("Authorization") token: String): Response<Ticket>

    @POST("/tickets/validate")
    suspend fun validateTicket(@Query("ticket") ticket: String): Response<Unit>

    // Model Server
    @POST("/token")
    suspend fun getModelServerToken(@Query("model_server_id") serverId: String): Response<ModelServerToken>

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
