package com.son.lecture_project.data.model

data class AttendanceRecord(
    val id: Int,
    val className: String,
    val date: String,
    val presentCount: Int,
    val totalCount: Int,
    val absentCount: Int
)
    