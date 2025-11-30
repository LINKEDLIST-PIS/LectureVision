package com.son.lecture_project.data.model

import com.google.gson.annotations.SerializedName

data class ClassSchedule(
    @SerializedName("id")
    val id: Int,
    @SerializedName("course_name")
    val name: String,
    @SerializedName("day_of_week")
    val day: String, // e.g., "MON", "TUE" or "Monday"
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("classroom")
    val classroom: String?,
    @SerializedName("color_hex")
    val color: String?,
    @SerializedName("total_students")
    val totalStudents: Int = 0 // 총 인원수 필드 추가 (기본값 0)
)

data class ClassScheduleRequest(
    @SerializedName("course_name")
    val name: String,
    @SerializedName("day_of_week")
    val day: String,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("classroom")
    val classroom: String?,
    @SerializedName("color_hex")
    val color: String?,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("total_students")
    val totalStudents: Int = 0
)
