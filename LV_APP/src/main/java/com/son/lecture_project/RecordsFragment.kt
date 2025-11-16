package com.son.lecture_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.son.lecture_project.data.model.AttendanceRecord // 데이터 모델 import
import com.son.lecture_project.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecordsAdapter
    private val recordItems = mutableListOf<AttendanceRecord>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadRecordsData()
    }

    private fun setupRecyclerView() {
        adapter = RecordsAdapter(recordItems)
        binding.recyclerViewRecords.adapter = adapter
        binding.recyclerViewRecords.layoutManager = LinearLayoutManager(context)
    }

    private fun loadRecordsData() {
        // TODO: 이 부분은 실제 서버나 데이터베이스에서 출석 기록 데이터를 가져와야 합니다.
        val dummyData = getDummyRecordsData()

        if (dummyData.isEmpty()) {
            binding.recyclerViewRecords.isVisible = false
            binding.tvNoRecords.isVisible = true
        } else {
            binding.recyclerViewRecords.isVisible = true
            binding.tvNoRecords.isVisible = false
            recordItems.clear()
            recordItems.addAll(dummyData)
            adapter.notifyDataSetChanged()
        }
    }

    private fun getDummyRecordsData(): List<AttendanceRecord> {
        // React 코드의 기록과 유사한 구조의 더미(가짜) 데이터입니다.
        return listOf(
            AttendanceRecord(1, "웹 프로그래밍", "2025-11-17", 28, 30, 2),
            AttendanceRecord(2, "자료구조", "2025-11-16", 29, 30, 1),
            AttendanceRecord(3, "알고리즘", "2025-11-15", 30, 30, 0),
            AttendanceRecord(4, "운영체제", "2025-11-14", 25, 30, 5)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
