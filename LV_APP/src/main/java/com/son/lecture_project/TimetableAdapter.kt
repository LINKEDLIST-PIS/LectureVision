package com.son.lecture_project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.databinding.ItemClassBinding


class TimetableAdapter(

    private val items: List<ClassSchedule>,
    private val onLongClick: (ClassSchedule) -> Unit
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {


    inner class ViewHolder(private val binding: ItemClassBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClassSchedule) {
            binding.tvClassName.text = item.name
            binding.tvTime.text = "${item.startTime} - ${item.endTime}"

            binding.cardView.setCardBackgroundColor(Color.parseColor(item.color))

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
