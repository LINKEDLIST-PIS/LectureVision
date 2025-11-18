package com.son.lecture_project.data.model

/**
 * 서버에서 받아온 출석 기록 한 개의 정보를 담는 데이터 클래스
 */
data class AttendanceRecord(
    val id: Int, // id 필드 추가
    val className: String,
    val date: String,
    val presentCount: Int,
    val totalCount: Int,
    val absentCount: Int
)
