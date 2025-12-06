package com.son.lecture_project.ui.records

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class RecordsAdapter : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    private var records: List<Upload> = emptyList()
    private var schedules: List<ClassSchedule> = emptyList()
    private var subjectColorMap: Map<String, String> = emptyMap()

    // 랜덤 색상 팔레트 (매칭되지 않았거나 색상 정보가 없을 때 사용)
    private val colorPalette = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#96CEB4",
        "#FFEEAD", "#D4A5A5", "#9B59B6", "#3498DB", "#E67E22"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun submitList(newRecords: List<Upload>) {
        this.records = newRecords
        notifyDataSetChanged()
    }

    fun updateSchedules(newSchedules: List<ClassSchedule>) {
        this.schedules = newSchedules
        notifyDataSetChanged()
    }

    fun updateColorMap(newColorMap: Map<String, String>) {
        this.subjectColorMap = newColorMap
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Upload) {
            // 날짜 포맷터 준비
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val recordDate = parseDate(record.uploadedAt, parsers)

            // 날짜 및 요일 표시 (KST 기준)
            val formattedDate = if (recordDate != null) {
                val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val dayOfWeekFormat = SimpleDateFormat("E", Locale.KOREAN)
                dayOfWeekFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                
                val dateText = outputFormat.format(recordDate)
                val dayOfWeek = dayOfWeekFormat.format(recordDate)
                "$dateText ($dayOfWeek)"
            } else {
                record.uploadedAt ?: "-"
            }
            binding.tvDate.text = formattedDate

            // [매칭 로직]
            // 1. ViewModel에서 이미 이름을 매핑해온 경우 우선 사용 (originalName이 시간표에 있는 이름인지 확인)
            val isSubjectName = schedules.any { it.name == record.originalName }
            
            var matchingSchedule: ClassSchedule? = null

            if (isSubjectName) {
                // 1-1. 이름으로 매칭 성공
                matchingSchedule = schedules.find { it.name == record.originalName }
            } else if (recordDate != null) {
                // 1-2. 이름 매칭 실패 시, 어댑터에서 날짜/시간 기반으로 재검색 (Fallback Logic)
                // 요일 계산
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply { time = recordDate }
                val dayName = getDayName(cal.get(Calendar.DAY_OF_WEEK))
                
                // 해당 요일 및 시간에 맞는 수업 탐색
                matchingSchedule = findMatchingSchedule(recordDate, dayName, schedules, SimpleDateFormat("HH:mm", Locale.getDefault()))
            }

            // [UI 반영]
            val courseName = matchingSchedule?.name ?: "과목 없음"
            binding.tvCourseName.text = courseName

            // 색상 처리: 시간표에 저장된 색상 우선 사용
            var colorToParse = subjectColorMap[courseName] // 기본 맵에서 조회

            // 매칭된 스케줄 객체에 직접 색상이 있다면 최우선 적용
            if (matchingSchedule?.color != null && matchingSchedule.color!!.isNotEmpty()) {
                colorToParse = matchingSchedule.color
            }

            val color = if (!colorToParse.isNullOrEmpty()) {
                try {
                    Color.parseColor(colorToParse)
                } catch (e: Exception) {
                    Color.parseColor("#9E9E9E") // 파싱 실패 시 회색
                }
            } else {
                if (matchingSchedule == null) {
                    Color.parseColor("#9E9E9E") // 매칭 안됨 -> 회색
                } else {
                    // 스케줄은 있지만 색상 정보가 없는 경우 -> 과목명 해시 기반 랜덤 색상
                    val colorIndex = abs(courseName.hashCode()) % colorPalette.size
                    Color.parseColor(colorPalette[colorIndex])
                }
            }
            binding.cvColor.setCardBackgroundColor(color)


            // 인원 통계 표시
            val totalStudents = matchingSchedule?.totalStudents ?: 0
            val measuredCount = record.peopleCount

            binding.tvTotalCount.text = "출석 : ${measuredCount}명"

            if (totalStudents > 0) {
                // 결석자 수 계산 (음수 방지)
                val absentCount = (totalStudents - measuredCount).coerceAtLeast(0)
                binding.tvAbsentCount.text = "결석 : ${absentCount}명"
            } else {
                binding.tvAbsentCount.text = "결석 : -명"
            }
        }
    }

    private fun parseDate(dateStr: String?, parsers: List<SimpleDateFormat>): Date? {
        if (dateStr.isNullOrEmpty()) return null
        val cleanDateStr = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr
        for (sdf in parsers) {
            try {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(cleanDateStr)
            } catch (e: Exception) { /* ignore */ }
        }
        return null
    }

    private fun getDayName(dayOfWeek: Int): String {
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

    private fun findMatchingSchedule(recordDate: Date, dayName: String, schedules: List<ClassSchedule>, timeFormat: SimpleDateFormat): ClassSchedule? {
        try {
            // 시간 비교는 KST 기준으로 진행
            timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val recordTimeStr = timeFormat.format(recordDate)
            val recordTime = timeFormat.parse(recordTimeStr) ?: return null

            // 해당 요일의 수업 중 시간이 겹치는 수업 필터링
            val matchingSchedules = schedules.filter { schedule ->
                schedule.day.contains(dayName) && try {
                    val startTime = timeFormat.parse(schedule.startTime) ?: return@filter false
                    val endTime = timeFormat.parse(schedule.endTime) ?: return@filter false
                    
                    // 시작시간 <= 기록시간 < 종료시간
                    !recordTime.before(startTime) && recordTime.before(endTime)
                } catch (e: Exception) {
                    false
                }
            }

            // 여러 개가 매칭될 경우(드물겠지만), 가장 짧은 수업(구체적인 수업)을 반환하거나 첫 번째 것 반환
            // 여기서는 첫 번째 매칭된 수업 반환
            return matchingSchedules.firstOrNull()
        } catch (e: Exception) {
            return null
        }
    }
}
