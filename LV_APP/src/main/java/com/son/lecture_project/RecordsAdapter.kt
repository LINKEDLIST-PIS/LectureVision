package com.son.lecture_project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class RecordsAdapter(private var subjectTotalCountMap: Map<String, Int> = emptyMap()) 
    : ListAdapter<Upload, RecordsAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private val colorPalette = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#96CEB4", 
        "#FFEEAD", "#D4A5A5", "#9B59B6", "#3498DB", "#E67E22"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateTotalCountMap(newMap: Map<String, Int>) {
        subjectTotalCountMap = newMap
        // ListAdapter는 submitList가 호출될 때 아이템을 다시 그리므로, 전체를 다시 그릴 필요가 없음
        // 만약 카운트만 바뀌고 아이템 리스트는 그대로라면 notifyDataSetChanged()를 호출해야 할 수도 있음
        // 하지만 보통 데이터 로드 시 함께 갱신되므로 submitList로 충분함
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm (E)", Locale.KOREAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        fun bind(record: Upload) {
            val formattedDate = try {
                val dateString = record.uploadedAt?.substringBefore(".")
                if (dateString != null) {
                    val date = inputFormat.parse(dateString.replace("Z", ""))
                    date?.let { outputFormat.format(it) } ?: (record.uploadedAt ?: "")
                } else {
                    ""
                }
            } catch (e: Exception) {
                record.uploadedAt ?: ""
            }
            
            val courseName = record.originalName?.takeIf { it.isNotEmpty() } ?: "측정 기록"

            binding.tvDate.text = formattedDate
            binding.tvCourseName.text = courseName

            val colorIndex = abs(courseName.hashCode()) % colorPalette.size
            binding.cvColor.setCardBackgroundColor(Color.parseColor(colorPalette[colorIndex]))

            val totalStudents = subjectTotalCountMap[courseName] ?: 0
            val measuredCount = record.peopleCount
            
            binding.tvTotalCount.text = itemView.context.getString(R.string.record_item_present, measuredCount)

            if (totalStudents > 0) {
                val absentCount = (totalStudents - measuredCount).coerceAtLeast(0)
                binding.tvAbsentCount.text = itemView.context.getString(R.string.record_item_absent, absentCount.toString())
            } else {
                binding.tvAbsentCount.text = itemView.context.getString(R.string.record_item_absent, "-")
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<Upload>() {
    override fun areItemsTheSame(oldItem: Upload, newItem: Upload): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Upload, newItem: Upload): Boolean {
        return oldItem == newItem
    }
}
