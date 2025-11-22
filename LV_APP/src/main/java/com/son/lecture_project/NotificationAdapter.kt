package com.son.lecture_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.NotificationItem
import com.son.lecture_project.data.model.NotificationType
import com.son.lecture_project.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val notifications: List<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: NotificationItem) {
            binding.tvTitle.text = item.title
            binding.tvContent.text = item.content
            binding.tvTime.text = item.timestamp
            
            // 타입별 라벨 및 색상 설정
            when (item.type) {
                NotificationType.NOTICE -> {
                    binding.tvType.text = "공지사항"
                    binding.tvType.backgroundTintList = binding.root.context.getColorStateList(R.color.btn_primary) // #0ea5e9
                }
                NotificationType.TIMETABLE -> {
                    binding.tvType.text = "시간표"
                    binding.tvType.backgroundTintList = binding.root.context.getColorStateList(R.color.purple_500)
                }
                NotificationType.TICKET -> {
                    binding.tvType.text = "티켓"
                    binding.tvType.backgroundTintList = binding.root.context.getColorStateList(R.color.teal_700)
                }
                NotificationType.MEASUREMENT -> {
                    binding.tvType.text = "측정결과"
                    binding.tvType.backgroundTintList = binding.root.context.getColorStateList(R.color.red_500)
                }
                NotificationType.INFO -> {
                    binding.tvType.text = "알림"
                    binding.tvType.backgroundTintList = binding.root.context.getColorStateList(R.color.grey_400)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}
