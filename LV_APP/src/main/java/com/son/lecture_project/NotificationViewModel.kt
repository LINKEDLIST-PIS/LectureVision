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

            // 4. 최근 측정 결과 알림 (API 조회)
            try {
                // 최근 5개 정도만 조회
                val recordResponse = RetrofitClient.recordApiService.getRecords(authHeader, skip = 0, limit = 5)
                if (recordResponse.isSuccessful && recordResponse.body() != null) {
                    val records = recordResponse.body()!!
                    records.forEach { record ->
                        list.add(NotificationItem(
                            id = idCounter++,
                            type = NotificationType.MEASUREMENT,
                            title = "측정 완료 알림",
                            content = "최근 측정된 인원: ${record.peopleCount}명",
                            timestamp = record.uploadedAt ?: getCurrentDate()
                        ))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 최신순 정렬 (timestamp 파싱이 가능하면 좋지만, 일단 간단히 리스트 역순은 아님)
            // timestamp 문자열 기준으로 내림차순 정렬 시도 (ISO8601 형식이면 문자열 정렬 가능)
            list.sortByDescending { it.timestamp }
            
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
