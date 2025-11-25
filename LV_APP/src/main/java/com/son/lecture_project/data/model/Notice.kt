package com.son.lecture_project.data.model

import com.google.gson.annotations.SerializedName

data class Notice(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("created_at")
    val createdAt: String
)
