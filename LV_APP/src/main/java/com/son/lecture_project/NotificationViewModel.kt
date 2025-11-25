package com.son.lecture_project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.model.NotificationItem
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotifications() {
        _isLoading.value = true
        viewModelScope.launch {
            // 보안상 티켓 발급 내역은 알림에 표시하지 않으며, 더미 데이터도 제거함.
            // 현재는 알림 데이터 소스가 없으므로 빈 리스트를 반환하여 기능 구조만 유지.
            val list = emptyList<NotificationItem>()
            
            _notifications.value = list
            _isLoading.value = false
        }
    }
}
