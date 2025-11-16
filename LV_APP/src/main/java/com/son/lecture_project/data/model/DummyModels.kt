package com.son.lecture_project.data.model

/**
 * 시간표 정보를 담는 데이터 클래스
 */
data class ClassSchedule(
    val id: Int,
    val name: String,
    val day: String,
    val startTime: String,
    val endTime: String,
    val color: String
)

/**
 * 공지사항 정보를 담는 데이터 클래스
 */
data class Notice(
    val id: Int,
    val title: String,
    val date: String
)
