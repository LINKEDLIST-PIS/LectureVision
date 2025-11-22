package com.son.lecture_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ScreenRecordsBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.records.RecordsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecordsScreen : Fragment() {

    private var _binding: ScreenRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter
    
    // 전체 데이터를 보관할 리스트
    private var allRecords: List<Upload> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabsAndFilters() 
        observeRecords()

        // 화면 진입 시 기록 데이터 로드
        recordsViewModel.loadRecords()
    }

    private fun setupRecyclerView() {
        recordsAdapter = RecordsAdapter(emptyList())
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordsAdapter
        }
    }
    
    private fun setupTabsAndFilters() {
        // 1. 탭 레이아웃 설정
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(getString(R.string.records_tab_list)))
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(getString(R.string.records_tab_stats)))

        binding.tabLayoutViewMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) { // 목록 탭
                    binding.contentFrame.visibility = View.VISIBLE
                    binding.layoutStats.visibility = View.GONE
                    binding.scrollFilters.visibility = View.VISIBLE
                } else { // 통계 탭
                    binding.contentFrame.visibility = View.GONE
                    binding.layoutStats.visibility = View.VISIBLE
                    binding.scrollFilters.visibility = View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // 2. 필터 스피너 설정
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(getString(R.string.records_filter_all_subjects)))
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClass.adapter = classAdapter

        val dateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(
            getString(R.string.records_filter_all_period), 
            getString(R.string.records_filter_recent_7), 
            getString(R.string.records_filter_recent_30)
        ))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = dateAdapter

        // 4. 조회 버튼 리스너 추가
        binding.btnSearch.setOnClickListener {
            filterAndShowRecords()
            Toast.makeText(context, getString(R.string.msg_searching), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRecords() {
        recordsViewModel.records.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = result.data
                    filterAndShowRecords()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    val errorMsg = result.exception.message ?: "Unknown Error"
                    Toast.makeText(context, getString(R.string.msg_load_fail, errorMsg), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterAndShowRecords() {
        val selectedDateFilter = binding.spinnerDate.selectedItemPosition
        // 0: 전체, 1: 최근 7일, 2: 최근 30일

        val filteredList = if (selectedDateFilter == 0) {
            allRecords
        } else {
            // API 레벨 호환성을 위해 Calendar와 SimpleDateFormat 사용
            val calendar = Calendar.getInstance()
            // 시간을 0시 0분 0초로 초기화하여 날짜만 비교
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val daysToSubtract = if (selectedDateFilter == 1) 7 else 30
            calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            val thresholdDate = calendar.time
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            allRecords.filter { record ->
                try {
                    // uploadedAt 형식에 따라 파싱 (예: 2023-10-27T10:00:00)
                    if (record.uploadedAt.length >= 10) {
                        val recordDateStr = record.uploadedAt.substring(0, 10) // 날짜 부분만 추출
                        val recordDate = sdf.parse(recordDateStr)
                        // recordDate가 thresholdDate보다 이전이 아니면(같거나 이후면) true
                        recordDate != null && !recordDate.before(thresholdDate)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    true // 파싱 실패 시 일단 포함
                }
            }
        }
        
        updateRecordList(filteredList)
    }

    private fun updateRecordList(records: List<Upload>) {
        binding.tvSummary.text = getString(R.string.records_total_count, records.size)

        if (records.isEmpty()) {
            binding.rvRecords.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvRecords.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            recordsAdapter.updateData(records)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
