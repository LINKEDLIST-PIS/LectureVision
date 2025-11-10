package com.son.lecture_project.data.api

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)
