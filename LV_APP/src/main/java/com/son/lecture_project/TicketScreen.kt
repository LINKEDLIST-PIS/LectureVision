package com.son.lecture_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.son.lecture_project.data.model.Ticket
import com.son.lecture_project.databinding.ScreenTicketBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.ticket.TicketViewModel

class TicketScreen : Fragment() {

    private var _binding: ScreenTicketBinding? = null
    private val binding get() = _binding!!

    // Use the new TicketViewModel
    private val ticketViewModel: TicketViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnIssueTicket.setOnClickListener {
            ticketViewModel.issueTicket()
        }
    }

    private fun observeViewModel() {
        ticketViewModel.ticketResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnIssueTicket.isEnabled = false
                    binding.cardTicketResult.isVisible = false
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnIssueTicket.isEnabled = true
                    binding.cardTicketResult.isVisible = true
                    
                    updateTicketUI(result.data)
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnIssueTicket.isEnabled = true
                    Toast.makeText(context, "티켓 발급 실패: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateTicketUI(ticket: Ticket) {
        // Display ticket information
        binding.tvTicketId.text = ticket.ticketId ?: "N/A"
        
        // isValidated가 true면 '이미 검증됨(사용됨)', false면 '아직 검증 안됨(사용 가능)'으로 해석
        // 다만 서버 로직에 따라 isValidated가 '유효성 여부'일 수도 있으나, 
        // 발급 직후 false라면 '검증 전' 상태일 확률이 높음.
        val isValidated = ticket.isValidated ?: false
        
        binding.tvTicketDesc.text = if (isValidated) {
            "상태: 검증 완료 (사용됨)"
        } else {
            "상태: 발급 완료 (검증 대기)"
        }
        
        binding.tvValidUntil.text = "유효기간: ${ticket.expiresAt ?: "정보 없음"}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
