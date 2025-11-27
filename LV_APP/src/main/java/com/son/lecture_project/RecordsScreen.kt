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
import kotlin.math.roundToInt

class RecordsScreen : Fragment() {

    private var _binding: ScreenRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter
    
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
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(getString(R.string.records_tab_list)))
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(getString(R.string.records_tab_stats)))

        binding.tabLayoutViewMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) { // List
                    binding.contentFrame.visibility = View.VISIBLE
                    binding.layoutStats.visibility = View.GONE
                    binding.scrollFilters.visibility = View.VISIBLE
                } else { // Stats
                    binding.contentFrame.visibility = View.GONE
                    binding.layoutStats.visibility = View.VISIBLE
                    binding.scrollFilters.visibility = View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
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
                    updateStats(allRecords) // Update stats with loaded data
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = emptyList()
                    filterAndShowRecords()
                    updateStats(emptyList()) // Update stats with empty data
                    
                    val errorMsg = result.exception.message ?: "Unknown Error"
                    Toast.makeText(context, getString(R.string.msg_load_fail, errorMsg), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateStats(records: List<Upload>) {
        if (records.isEmpty()) {
            binding.tvAttendanceRate.text = "-"
            binding.progressAttendance.progress = 0
            binding.tvStatsPresent.text = "0명"
            binding.tvStatsAbsent.text = "-"
            resetBarChart()
            return
        }

        // 1. Calculate "Attendance Rate" (Actually Average Present Count)
        val totalPresent = records.sumOf { it.peopleCount }
        val avgPresent = totalPresent.toDouble() / records.size
        
        binding.tvAttendanceRate.text = String.format(Locale.getDefault(), "%.1f명", avgPresent)
        binding.progressAttendance.progress = 0 

        binding.tvStatsPresent.text = "${totalPresent}명"
        binding.tvStatsAbsent.text = "-" 

        // 2. Calculate Weekly Stats
        val dayCounts = MutableList(5) { 0 } // Mon, Tue, Wed, Thu, Fri
        val dayUploadCounts = MutableList(5) { 0 }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        records.forEach { record ->
            try {
                if (record.uploadedAt.length >= 10) {
                    val dateStr = record.uploadedAt.substring(0, 10)
                    val date = sdf.parse(dateStr)
                    if (date != null) {
                        cal.time = date
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        // Calendar.MONDAY = 2, FRIDAY = 6
                        if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                            val index = dayOfWeek - Calendar.MONDAY // 0 to 4
                            dayCounts[index] += record.peopleCount
                            dayUploadCounts[index]++
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing error
            }
        }
        
        // Update Bars
        val maxCount = dayCounts.maxOrNull() ?: 1
        val scale = if (maxCount > 0) maxCount else 1
        
        updateBar(binding.viewBarMon, dayCounts[0], scale)
        updateBar(binding.viewBarTue, dayCounts[1], scale)
        updateBar(binding.viewBarWed, dayCounts[2], scale)
        updateBar(binding.viewBarThu, dayCounts[3], scale)
        updateBar(binding.viewBarFri, dayCounts[4], scale)
    }

    private fun updateBar(view: View, count: Int, max: Int) {
        val maxBarHeight = 120 // Max height in dp
        val density = resources.displayMetrics.density
        val heightDp = if (max > 0) (count.toFloat() / max * maxBarHeight) else 0f
        
        val params = view.layoutParams
        params.height = (heightDp * density).toInt().coerceAtLeast((1 * density).toInt()) // Min 1dp
        view.layoutParams = params
    }
    
    private fun resetBarChart() {
        val zeroHeight = (1 * resources.displayMetrics.density).toInt()
        binding.viewBarMon.layoutParams.height = zeroHeight
        binding.viewBarTue.layoutParams.height = zeroHeight
        binding.viewBarWed.layoutParams.height = zeroHeight
        binding.viewBarThu.layoutParams.height = zeroHeight
        binding.viewBarFri.layoutParams.height = zeroHeight
        binding.viewBarMon.requestLayout()
    }

    private fun filterAndShowRecords() {
        val selectedDateFilter = binding.spinnerDate.selectedItemPosition

        val filteredList = if (selectedDateFilter == 0) {
            allRecords
        } else {
            val calendar = Calendar.getInstance()
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
                    if (record.uploadedAt.length >= 10) {
                        val recordDateStr = record.uploadedAt.substring(0, 10)
                        val recordDate = sdf.parse(recordDateStr)
                        recordDate != null && !recordDate.before(thresholdDate)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    true
                }
            }
        }
        
        updateRecordList(filteredList)
        updateStats(filteredList) // Recalculate stats based on filter
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
