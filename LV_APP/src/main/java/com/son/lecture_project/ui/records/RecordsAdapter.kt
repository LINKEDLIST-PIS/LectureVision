package com.son.lecture_project.ui.records

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class RecordsAdapter(
    private var records: List<Upload> = emptyList(), 
    private var subjectTotalCountMap: Map<String, Int> = emptyMap(),
    private var subjectColorMap: Map<String, String> = emptyMap() // 색상 맵 추가
) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    // 기존 팔레트는 백업으로 유지
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

    fun updateTotalCountMap(newMap: Map<String, Int>) {
        this.subjectTotalCountMap = newMap
        notifyDataSetChanged()
    }
    
    // 색상 맵 업데이트 함수 추가
    fun updateColorMap(newColorMap: Map<String, String>) {
        this.subjectColorMap = newColorMap
        notifyDataSetChanged()
    }

    fun updateData(newRecords: List<Upload>, newSubjectTotalCountMap: Map<String, Int>, newSubjectColorMap: Map<String, String>) {
        this.records = newRecords
        this.subjectTotalCountMap = newSubjectTotalCountMap
        this.subjectColorMap = newSubjectColorMap
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Upload) {

            val formattedDate = try {
                val rawDate = record.uploadedAt
                if (rawDate.isNullOrEmpty()) {
                    "-"
                } else {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                    val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                    outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    
                    val dayOfWeekFormat = SimpleDateFormat("E", Locale.KOREAN)
                    dayOfWeekFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                    val dateString = if (rawDate.contains(".")) {
                        rawDate.substringBefore(".").replace("Z", "")
                    } else {
                         rawDate.replace("Z", "")
                    }

                    val date = try {
                        inputFormat.parse(dateString)
                    } catch (e: Exception) {
                         val fallbackFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                         fallbackFormat.timeZone = TimeZone.getTimeZone("UTC")
                         fallbackFormat.parse(dateString)
                    }

                    if (date != null) {
                        val dateText = outputFormat.format(date)
                        val dayOfWeek = dayOfWeekFormat.format(date)
                        "$dateText ($dayOfWeek)"
                    } else {
                        rawDate
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                record.uploadedAt ?: "-"
            }
            
            binding.tvDate.text = formattedDate
            
            val courseName = record.originalName ?: "측정 기록"
            binding.tvCourseName.text = courseName
            
            // [수정] 시간표 색상 우선 적용, 없으면 기존 해시 기반 색상 사용
            val assignedColor = subjectColorMap[courseName]
            val color = if (assignedColor != null) {
                try {
                     Color.parseColor(assignedColor)
                } catch (e: Exception) {
                     // 색상 파싱 실패 시 팔레트 사용
                     val colorIndex = abs(courseName.hashCode()) % colorPalette.size
                     Color.parseColor(colorPalette[colorIndex])
                }
            } else {
                val colorIndex = abs(courseName.hashCode()) % colorPalette.size
                Color.parseColor(colorPalette[colorIndex])
            }
            binding.cvColor.setCardBackgroundColor(color)

            val totalStudents = subjectTotalCountMap[courseName] ?: 0
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
}
