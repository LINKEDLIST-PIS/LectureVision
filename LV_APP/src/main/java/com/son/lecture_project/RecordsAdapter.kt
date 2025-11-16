package com.son.lecture_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.AttendanceRecord
import com.son.lecture_project.databinding.ItemRecordBinding

class RecordsAdapter(
    private val items: List<AttendanceRecord>
) : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AttendanceRecord) {
            binding.tvClassName.text = item.className
            binding.tvDate.text = item.date
            binding.tvPresentCount.text = item.presentCount.toString()
            binding.tvTotalCount.text = item.totalCount.toString()
            binding.tvAbsentCount.text = item.absentCount.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
    