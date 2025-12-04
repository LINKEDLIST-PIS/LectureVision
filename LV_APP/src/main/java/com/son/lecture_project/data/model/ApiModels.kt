package com.son.lecture_project.data.model

import com.google.gson.annotations.SerializedName


// --- API-related data models ---


data class SignUpRequest(
    val email: String,
    val password: String

)

data class SignUpResponse(
    val id: Int,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("is_verified")
    val isVerified: Boolean
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

data class User(
    val id: Int,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("is_verified")
    val isVerified: Boolean
)

// Upload
data class Upload(
    val id: Int,
    @SerializedName("original_name")
    val originalName: String?,
    @SerializedName("stored_name")
    val storedName: String?,
    @SerializedName("abs_path")
    val absPath: String?,
    @SerializedName("people_count")
    val peopleCount: Int,
    @SerializedName("uploaded_at")
    val uploadedAt: String?,
    @SerializedName("client_id")
    val clientId: String?
)

// Token for Model Server
data class ModelServerToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

// --- Model Server ---

data class MeasureResponse(
    @SerializedName("people_count")
    val peopleCount: Int,
    @SerializedName("api_response")
    val apiResponse: ApiResponseDetails
)

data class ApiResponseDetails(
    val status: String,
    val id: String
)
