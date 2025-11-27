package com.son.lecture_project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Locale
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
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) 
                val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                val date = inputFormat.parse(record.uploadedAt.replace("Z", ""))
                date?.let { outputFormat.format(it) } ?: record.uploadedAt
            } catch (e: Exception) {
                record.uploadedAt
            }
            
            binding.tvDate.text = formattedDate
            
            val courseName = if (record.originalName.isNullOrEmpty()) "측정 기록" else record.originalName
            binding.tvCourseName.text = courseName
            
            // 1. 과목 색상 설정 (이름 기반 해시)
            val colorIndex = abs(courseName.hashCode()) % colorPalette.size
            val color = Color.parseColor(colorPalette[colorIndex])
            binding.cvColor.setCardBackgroundColor(color)

            // 2. 인원 정보 설정 (변경된 레이아웃 반영)
            // Upload 객체에는 'peopleCount'만 있으므로 이를 총인원(혹은 출석)으로 표시하고,
            // 결석 정보가 없으므로 임의로 0명 혹은 계산된 값(여기서는 0명)으로 표시
            binding.tvTotalCount.text = "총인원 : ${record.peopleCount}명"
            binding.tvAbsentCount.text = "결석 : 0명"
        }
    }
}
