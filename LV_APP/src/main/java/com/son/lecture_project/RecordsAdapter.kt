package com.son.lecture_project

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

class RecordsAdapter(private var records: List<Upload>) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    // 과목별 색상을 지정하기 위한 팔레트
    private val colorPalette = listOf(
        "#FF6B6B", // Red
        "#4ECDC4", // Teal
        "#45B7D1", // Blue
        "#FFA07A", // Light Salmon
        "#96CEB4", // Pale Green
        "#FFEEAD", // Pale Yellow
        "#D4A5A5", // Pinkish
        "#9B59B6", // Purple
        "#3498DB", // Dodger Blue
        "#E67E22"  // Carrot
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<Upload>) {
        this.records = newRecords
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Upload) {

            val formattedDate = try {
                // 1. 서버 시간(UTC) 파싱 설정
                // Z가 있거나 없을 수 있는 포맷 대응 (보통 ISO 8601)
                // 여기서는 단순화를 위해 Z를 제거하고 파싱
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC") // 입력은 UTC 기준

                // 2. 출력 시간(KST) 및 요일 설정
                val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul") // 출력은 한국 시간 기준
                
                val dayOfWeekFormat = SimpleDateFormat("E", Locale.KOREAN) // 요일 (월, 화, 수...)
                dayOfWeekFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                val dateString = if (record.uploadedAt.contains(".")) {
                     record.uploadedAt.substring(0, record.uploadedAt.indexOf(".")) // 소수점 이하 제거
                } else {
                     record.uploadedAt
                }
                val date = inputFormat.parse(dateString.replace("Z", ""))

                if (date != null) {
                    val dateText = outputFormat.format(date)
                    val dayOfWeek = dayOfWeekFormat.format(date)
                    "$dateText ($dayOfWeek)"
                } else {
                    record.uploadedAt
                }
            } catch (e: Exception) {
                e.printStackTrace()
                record.uploadedAt
            }
            
            binding.tvDate.text = formattedDate
            
            val courseName = if (record.originalName.isNullOrEmpty()) "측정 기록" else record.originalName
            binding.tvCourseName.text = courseName
            
            // 1. 과목 색상 설정 (이름 기반 해시)
            val colorIndex = abs(courseName.hashCode()) % colorPalette.size
            val color = Color.parseColor(colorPalette[colorIndex])
            binding.cvColor.setCardBackgroundColor(color)

            // 2. 인원 정보 설정
            binding.tvTotalCount.text = "총인원 : ${record.peopleCount}명"
            binding.tvAbsentCount.text = "결석 : 0명"
        }
    }
}
