package com.son.lecture_project.data.model

data class NotificationItem(
    val id: Int,
    val type: NotificationType,
    val title: String,
    val content: String,
    val timestamp: String
)

enum class NotificationType {
    NOTICE,
    TIMETABLE,
    TICKET,
    MEASUREMENT,
    INFO
}
