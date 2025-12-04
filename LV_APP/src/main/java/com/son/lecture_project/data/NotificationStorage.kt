package com.son.lecture_project.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.son.lecture_project.data.model.NotificationItem
import com.son.lecture_project.data.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationStorage {
    private val _notifications = MutableLiveData<List<NotificationItem>>(emptyList())
    val notifications: LiveData<List<NotificationItem>> = _notifications

    // 알림을 보낸 수업의 키(수업명+시작시간)를 저장하여 중복 알림 방지
    private val sentNotificationKeys = mutableSetOf<String>()

    fun addNotification(title: String, content: String) {
        val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN).format(Date())
        
        // ID 생성 (기존 리스트가 비어있으면 1, 아니면 최대값 + 1)
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        
        val newItem = NotificationItem(
            id = newId,
            title = title,
            content = content,
            timestamp = currentTime,
            type = NotificationType.TIMETABLE
        )
        
        // 최신 알림이 위로 오도록 0번 인덱스에 추가
        currentList.add(0, newItem)
        _notifications.postValue(currentList)
    }
    
    // 이미 알림을 보냈는지 확인하는 함수
    fun isNotificationSent(key: String): Boolean {
        return sentNotificationKeys.contains(key)
    }

    // 알림 보냄 처리
    fun setNotificationSent(key: String) {
        sentNotificationKeys.add(key)
    }

    fun clearNotifications() {
        _notifications.postValue(emptyList())
        sentNotificationKeys.clear()
    }
}
