package com.son.lecture_project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.NotificationItem
import com.son.lecture_project.data.model.NotificationType
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotifications() {
        _isLoading.value = true
        viewModelScope.launch {
            val token = TokenManager.getToken()
            if (token == null) {
                _isLoading.value = false
                return@launch
            }
            val authHeader = "Bearer $token"
            val userId = TokenManager.getUserId()

            val list = mutableListOf<NotificationItem>()
            var idCounter = 1

            // 1. 공지사항 가져오기
            try {
                val noticeResponse = RetrofitClient.noticeApiService.getNotices(authHeader)
                if (noticeResponse.isSuccessful && noticeResponse.body() != null) {
                    noticeResponse.body()!!.take(3).forEach { notice ->
                        list.add(NotificationItem(
                            id = idCounter++,
                            type = NotificationType.NOTICE,
                            title = notice.title,
                            content = notice.content,
                            timestamp = notice.createdAt ?: getCurrentDate()
                        ))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. 오늘 시간표 / 공강 알림
            if (userId != null) {
                try {
                    val timetableResponse = RetrofitClient.timetableApiService.getUserTimetable(authHeader, userId)
                    if (timetableResponse.isSuccessful && timetableResponse.body() != null) {
                        val todayName = getTodayDayName()
                        val todayClasses = timetableResponse.body()!!.filter { it.day.contains(todayName) }

                        if (todayClasses.isEmpty()) {
                            // 공강 알림
                            if (todayName.isNotEmpty() && todayName != "일" && todayName != "토") {
                                list.add(NotificationItem(
                                    id = idCounter++,
                                    type = NotificationType.INFO,
                                    title = "오늘은 공강입니다!",
                                    content = "오늘은 수업이 없는 날입니다. 편안한 하루 보내세요.",
                                    timestamp = getCurrentDate()
                                ))
                            }
                        } else {
                            // 수업 알림
                            todayClasses.forEach { schedule ->
                                list.add(NotificationItem(
                                    id = idCounter++,
                                    type = NotificationType.TIMETABLE,
                                    title = "오늘 수업 알림",
                                    content = "${schedule.name} (${schedule.startTime} ~ ${schedule.endTime}) - ${schedule.classroom ?: "강의실 미정"}",
                                    timestamp = getCurrentDate()
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. 티켓 상태 알림
            if (userId != null) {
                try {
                    val ticketResponse = RetrofitClient.ticketApiService.getUserTicket(authHeader, userId)
                    if (ticketResponse.isSuccessful && ticketResponse.body() != null) {
                        val ticket = ticketResponse.body()!!
                        val isValid = ticket.isValidated ?: false
                        val statusMsg = if (isValid) "검증 완료(사용됨)" else "사용 가능"
                        
                        list.add(NotificationItem(
                            id = idCounter++,
                            type = NotificationType.TICKET,
                            title = "티켓 상태 알림",
                            content = "현재 보유하신 티켓 상태: $statusMsg",
                            timestamp = getCurrentDate()
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. (임시) 최근 측정 결과 알림 시뮬레이션 (API가 없어서 생략하거나 임시 데이터)
            // 실제로는 서버에서 측정 완료 시 푸시를 주거나 조회 기록 API가 있어야 함.
            
            // 최신순 정렬 (timestamp 파싱이 복잡하므로 역순 추가된 순서대로 보여주거나 별도 정렬 로직 필요)
            // 여기서는 단순히 리스트 뒤집기 (최근 것이 위로 오게 하려면)
            // 하지만 위 로직은 카테고리별로 추가했으므로 섞여있음.
            
            _notifications.value = list
            _isLoading.value = false
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTodayDayName(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }
}
