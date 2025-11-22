package com.son.lecture_project.ui.records

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ItemRecordBinding

class RecordsAdapter(private var records: List<Upload>) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    // 메서드 이름 변경: updateData -> submitList (확실한 갱신을 위해)
    fun submitList(newRecords: List<Upload>) {
        this.records = newRecords
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Upload) {
            val formattedDate = record.uploadedAt.split("T").firstOrNull() ?: record.uploadedAt
            
            binding.tvDate.text = formattedDate
            binding.tvCourseName.text = record.originalName
            
            try {
                binding.tvStatus.text = "출석: ${record.peopleCount}명"
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
