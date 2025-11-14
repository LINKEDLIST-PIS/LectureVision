package com.son.lecture_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.timetable.databinding.ItemClassBinding

class TimetableAdapter(
    private val items: List<ClassItem>,
    private val onLongClick: (ClassItem) -> Unit
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemClassBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ClassItem) {
            binding.tvClassName.text = item.name
            binding.tvTime.text = "${item.startTime} - ${item.endTime}"
            binding.cardView.setCardBackgroundColor(item.color)

            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
