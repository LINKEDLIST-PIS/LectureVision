package com.son.lecture_project

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

    fun updateData(newRecords: List<Upload>) {
        this.records = newRecords
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Upload) {
            // The date format might be long, so we can split it for better readability.
            val formattedDate = record.uploadedAt.split("T").firstOrNull() ?: record.uploadedAt
            binding.tvDate.text = formattedDate
            binding.tvClassName.text = record.originalName // Using tvClassName to show the original name
            binding.tvPresent.text = record.peopleCount.toString()

            // TODO: The following fields are in the layout but no data is available from the API yet.
            // binding.tvTotal.text = "?"
            // binding.tvAbsent.text = "?"
            // binding.tvDuration.text = "?"
            // binding.tvRateBadge.text = "?%"
        }
    }
}
