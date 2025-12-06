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
        // Exclude weekends from the list that is actually displayed
        this.records = newRecords.filter { record ->
            val recordDate = parseDate(record.uploadedAt, listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            ))
            if (recordDate != null) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply { time = recordDate }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
            } else {
                true // Keep records with unparseable dates for now, or filter them
            }
        }
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
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val recordDate = parseDate(record.uploadedAt, parsers)

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

            val matchingSchedule = if (recordDate != null) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply { time = recordDate }
                val dayName = getDayName(cal.get(Calendar.DAY_OF_WEEK))
                findMatchingSchedule(recordDate, dayName, schedules, SimpleDateFormat("HH:mm", Locale.getDefault()))
            } else {
                null
            }

            val courseName = matchingSchedule?.name ?: "과목 없음"
            binding.tvCourseName.text = courseName

            val assignedColor = subjectColorMap[courseName]
            val color = if (assignedColor != null) {
                try {
                    Color.parseColor(assignedColor)
                } catch (e: Exception) {
                    val colorIndex = abs(courseName.hashCode()) % colorPalette.size
                    Color.parseColor(colorPalette[colorIndex])
                }
            } else {
                val colorIndex = abs(courseName.hashCode()) % colorPalette.size
                Color.parseColor(colorPalette[colorIndex])
            }
            binding.cvColor.setCardBackgroundColor(color)

            val totalStudents = matchingSchedule?.totalStudents ?: 0
            val measuredCount = record.peopleCount

            binding.tvTotalCount.text = "출석 : ${measuredCount}명"

            if (totalStudents > 0) {
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
            timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val recordTime = timeFormat.parse(timeFormat.format(recordDate)) ?: return null

            val matchingSchedules = schedules.filter { schedule ->
                schedule.day.contains(dayName) && try {
                    val startTime = timeFormat.parse(schedule.startTime) ?: return@filter false
                    val endTime = timeFormat.parse(schedule.endTime) ?: return@filter false
                    !recordTime.before(startTime) && recordTime.before(endTime)
                } catch (e: Exception) {
                    false
                }
            }

            return matchingSchedules.minByOrNull { schedule ->
                try {
                    val startTime = timeFormat.parse(schedule.startTime)?.time ?: -1
                    val endTime = timeFormat.parse(schedule.endTime)?.time ?: -1
                    if (startTime == -1L || endTime == -1L) Long.MAX_VALUE else endTime - startTime
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            }
        } catch (e: Exception) {
            return null
        }
    }
}
