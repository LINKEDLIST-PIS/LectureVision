package com.son.lecture_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.databinding.ItemTicketBinding

class TicketAdapter(
    private val tickets: MutableList<Ticket>
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    inner class TicketViewHolder(private val binding: ItemTicketBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: Ticket) {
            // 수정됨: Ticket 모델의 실제 필드를 사용하도록 변경
            binding.tvTicketTitle.text = "티켓 ID: ${ticket.ticketId ?: "N/A"}"
            
            // isValidated가 Nullable(Boolean?)이므로 안전하게 체크
            val isValid = ticket.isValidated ?: false
            binding.tvTicketDesc.text = if (isValid) "상태: 유효함" else "상태: 만료됨/사용불가"
            
            binding.tvValidUntil.text = "유효기간: ${ticket.expiresAt ?: "정보 없음"}"

            binding.btnUseTicket.setOnClickListener {
                // TODO: 티켓 사용 로직 구현 (서버 통신 등)
                // 임시로 사용 완료 처리
                // tickets.removeAt(adapterPosition)
                // notifyItemRemoved(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int = tickets.size
}
