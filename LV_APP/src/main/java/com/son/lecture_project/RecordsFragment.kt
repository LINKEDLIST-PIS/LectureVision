package com.son.lecture_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.son.lecture_project.databinding.FragmentRecordsBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.records.RecordsViewModel

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter

    // Dummy data for class filter - replace with actual data later
    private val classFilterOptions = mapOf("전체 수업" to "all", "웹 프로그래밍" to "web", "자료구조" to "ds")
    private val dateFilterOptions = mapOf("전체 기간" to "all", "오늘" to "today", "최근 7일" to "week", "최근 30일" to "month")

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
        setupFilters()
        // setupClickListeners() // Removed as we now trigger search on filter change
        observeRecordsResult()

        // Load initial data
        recordsViewModel.fetchRecords()
    }

    private fun setupRecyclerView() {
        recordsAdapter = RecordsAdapter(emptyList())
        binding.rvRecords.apply { // Changed ID from recyclerViewRecords to rvRecords
            adapter = recordsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupFilters() {
        // Class Filter Spinner
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classFilterOptions.keys.toList())
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClass.adapter = classAdapter // Changed ID from spinnerClassFilter to spinnerClass

        binding.spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                triggerSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Date Filter Spinner
        val dateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dateFilterOptions.keys.toList())
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = dateAdapter // Changed ID from spinnerDateFilter to spinnerDate

        binding.spinnerDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                triggerSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        // Reset Filters Button
        binding.btnResetFilters.setOnClickListener {
            binding.spinnerClass.setSelection(0)
            binding.spinnerDate.setSelection(0)
            // Selection change will trigger search automatically via listeners
        }
    }

    private fun triggerSearch() {
        val selectedClassKey = binding.spinnerClass.selectedItem as? String ?: return
        val selectedDateKey = binding.spinnerDate.selectedItem as? String ?: return

        val classId = classFilterOptions[selectedClassKey]
        val dateFilter = dateFilterOptions[selectedDateKey]

        recordsViewModel.fetchRecords(classId, dateFilter)
    }

    private fun observeRecordsResult() {
        recordsViewModel.recordsResult.observe(viewLifecycleOwner) { result ->
            // There is no progress bar in the new layout for the whole screen, 
            // but you might want to add one or show a loading state.
            // For now, just hiding the list on load.
            when (result) {
                is Result.Loading -> {
                    binding.rvRecords.isVisible = false
                    binding.layoutEmpty.isVisible = false // Changed ID from tvNoRecords to layoutEmpty
                }
                is Result.Success -> {
                    val records = result.data
                    if (records.isEmpty()) {
                        binding.layoutEmpty.isVisible = true
                        binding.rvRecords.isVisible = false
                    } else {
                        binding.layoutEmpty.isVisible = false
                        binding.rvRecords.isVisible = true
                        recordsAdapter.updateData(records)
                        
                        // Update summary text
                        binding.tvSummary.text = "총 ${records.size}개의 기록"
                    }
                }
                is Result.Error -> {
                    binding.layoutEmpty.isVisible = true
                    // You might want to change the empty layout text to show the error message
                    binding.rvRecords.isVisible = false
                    Toast.makeText(context, "오류: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("RecordsFragment", "Error fetching records", result.exception)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
