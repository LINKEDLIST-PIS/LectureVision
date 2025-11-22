package com.son.lecture_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RecordsAdapter(private var records: List<Upload>) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

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
            // Format the date string (e.g., "2025-10-25T16:51:00Z" -> "2025-10-25 16:51")
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) 
                val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                val date = inputFormat.parse(record.uploadedAt.replace("Z", ""))
                date?.let { outputFormat.format(it) } ?: record.uploadedAt
            } catch (e: Exception) {
                record.uploadedAt
            }
            
            binding.tvDate.text = formattedDate
            
            // 과목명 처리: originalName이 있으면 사용, 없으면 기본값
            val courseName = if (record.originalName.isNullOrEmpty()) "측정 기록" else record.originalName
            binding.tvCourseName.text = courseName
            
            binding.tvPresent.text = "${record.peopleCount}명"
            
            // 상태 텍스트 설정 (추후 서버 데이터에 따라 분기 가능)
            binding.tvStatus.text = "완료"
        }
    }
}
