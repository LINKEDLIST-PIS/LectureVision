package com.son.lecture_project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.son.lecture_project.data.NotificationStorage
import com.son.lecture_project.data.model.NotificationItem

class NotificationViewModel : ViewModel() {

    // NotificationStorage의 LiveData를 그대로 사용
    val notifications: LiveData<List<NotificationItem>> = NotificationStorage.notifications

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotifications() {
        // 실제 데이터는 NotificationStorage에서 실시간으로 관리되므로
        // 별도의 로딩 로직이 필요 없습니다.
        _isLoading.value = false
    }
}
