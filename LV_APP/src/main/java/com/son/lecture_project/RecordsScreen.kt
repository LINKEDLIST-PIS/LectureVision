package com.son.lecture_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.data.model.Upload
import com.son.lecture_project.databinding.ScreenRecordsBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.records.RecordsAdapter
import com.son.lecture_project.ui.records.RecordsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RecordsScreen : Fragment() {

    private var _binding: ScreenRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter
    
    private var allRecords: List<Upload> = emptyList()
    private var subjectList: List<String> = emptyList()

    companion object {
        private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context != null) {
            setupRecyclerView()
            setupTabsAndFilters() 
            observeRecords()
            observeSubjectList()

            recordsViewModel.loadRecords()
            recordsViewModel.loadSubjectList()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh subject list in case it was changed in another screen
        recordsViewModel.loadSubjectList()
    }

    private fun setupRecyclerView() {
        if (_binding == null) return
        val ctx = context ?: return

        recordsAdapter = RecordsAdapter()
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = recordsAdapter
        }
    }
    
    private fun setupTabsAndFilters() {
        if (_binding == null) return
        val ctx = context ?: return 

        val listTabTitle = ctx.getString(R.string.records_tab_list)
        val statsTabTitle = ctx.getString(R.string.records_tab_stats)
        
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(listTabTitle))
        binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(statsTabTitle))

        binding.tabLayoutViewMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (_binding == null) return
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
        
        val allSubjects = ctx.getString(R.string.records_filter_all_subjects)
        val defaultSubjects = listOf(allSubjects)
        
        val classAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, defaultSubjects)
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClass.adapter = classAdapter

        val dateAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, listOf(
            ctx.getString(R.string.records_filter_all_period),
            ctx.getString(R.string.records_filter_recent_1),
            ctx.getString(R.string.records_filter_recent_7), 
            ctx.getString(R.string.records_filter_recent_30)
        ))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = dateAdapter
        
        binding.spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // User must press search to filter
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnSearch.setOnClickListener {
            if (_binding != null && isAdded) {
                filterAndShowRecords()
                context?.let {
                    Toast.makeText(it, it.getString(R.string.msg_searching), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeRecords() {
        recordsViewModel.records.observe(viewLifecycleOwner) { result ->
            if (_binding == null || !isAdded) return@observe

            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = result.data
                    Log.d("RecordsScreen", "Records loaded: ${allRecords.size} items")
                    filterAndShowRecords()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = emptyList()
                    filterAndShowRecords() // Show empty list
                    
                    val errorMsg = result.exception.message ?: "Unknown Error"
                    Log.e("RecordsScreen", "Records load error: $errorMsg")
                    
                    context?.let {
                         Toast.makeText(it, it.getString(R.string.msg_load_fail, errorMsg), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun observeSubjectList() {
        recordsViewModel.subjectList.observe(viewLifecycleOwner) { subjects ->
            if (_binding == null || !isAdded) return@observe
            val ctx = context ?: return@observe

            val allSubjects = ctx.getString(R.string.records_filter_all_subjects)
            val filterList = mutableListOf(allSubjects)
            filterList.addAll(subjects)
            
            this.subjectList = subjects
            
            val currentSelection = binding.spinnerClass.selectedItem as? String
            val classAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, filterList)
            classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClass.adapter = classAdapter

            if (currentSelection != null) {
                val newPosition = filterList.indexOf(currentSelection)
                if (newPosition >= 0) {
                    binding.spinnerClass.setSelection(newPosition)
                }
            }

            if (::recordsAdapter.isInitialized) {
                recordsAdapter.updateSchedules(recordsViewModel.getAllSchedules())
                recordsAdapter.updateColorMap(recordsViewModel.getSubjectColorMap())
            }
            
            if (allRecords.isNotEmpty()) {
                filterAndShowRecords()
            }
        }
    }
    
    private fun updateStats(records: List<Upload>) {
        if (_binding == null || !isAdded) return
        try {
            val allSchedules = recordsViewModel.getAllSchedules()

            if (records.isNotEmpty() && allSchedules.isEmpty()) {
                Log.w("RecordsScreen", "Stats calculation skipped: schedule data not ready.")
                return
            }

            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sessionKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = KST }

            // Group records by session (course + day) and get the latest upload for each.
            val finalRecordsForStats = records
                .mapNotNull { record ->
                    val recordDate = parseDate(record.uploadedAt, parsers) ?: return@mapNotNull null
                    val cal = Calendar.getInstance(KST).apply { time = recordDate }
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        return@mapNotNull null
                    }
                    val dayName = getDayName(dayOfWeek)
                    val matchingSchedule = findMatchingSchedule(recordDate, dayName, allSchedules, timeFormat)
                    
                    if (matchingSchedule == null) {
                        null
                    } else {
                        val sessionDateStr = sessionKeyFormat.format(recordDate)
                        val sessionKey = "${matchingSchedule.name}_${sessionDateStr}"
                        Triple(sessionKey, recordDate, record)
                    }
                }
                .groupBy { it.first }
                .map { (_, triples) ->
                    triples.maxByOrNull { it.second }!!.third
                }

            if (finalRecordsForStats.isEmpty()) {
                binding.tvRatePresent.text = "0%"
                binding.tvRateAbsent.text = "0%"
                resetTop3()
                resetBarChart()
                return
            }

            var totalPossible = 0
            var totalPresent = 0
            
            val subjectStatsMap = mutableMapOf<String, Pair<Int, Int>>()
            val dayPresentCounts = IntArray(5)
            val dayAbsentCounts = IntArray(5)

            finalRecordsForStats.forEach { record ->
                val recordDate = parseDate(record.uploadedAt, parsers) ?: return@forEach
                val cal = Calendar.getInstance(KST).apply { time = recordDate }
                
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    return@forEach
                }

                val dayName = getDayName(dayOfWeek)
                val matchingSchedule = findMatchingSchedule(recordDate, dayName, allSchedules, timeFormat)

                if (matchingSchedule != null) {
                    val courseName = matchingSchedule.name
                    val maxStudents = matchingSchedule.totalStudents

                    if (maxStudents > 0) {
                        val present = record.peopleCount
                        val absent = (maxStudents - present).coerceAtLeast(0)

                        totalPossible += maxStudents
                        totalPresent += present

                        val currentStat = subjectStatsMap.getOrDefault(courseName, 0 to 0)
                        subjectStatsMap[courseName] = (currentStat.first + maxStudents) to (currentStat.second + absent)

                        if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                            val index = dayOfWeek - Calendar.MONDAY
                            dayPresentCounts[index] += present
                            dayAbsentCounts[index] += absent
                        }
                    }
                }
            }
            
            val attendanceRate = if (totalPossible > 0) (totalPresent.toDouble() / totalPossible * 100) else 0.0
            val absenceRate = if (totalPossible > 0) ((totalPossible - totalPresent).toDouble() / totalPossible * 100) else 0.0
            
            binding.tvRatePresent.text = String.format(Locale.getDefault(), "%.1f%%", attendanceRate)
            binding.tvRateAbsent.text = String.format(Locale.getDefault(), "%.1f%%", absenceRate)

            val sortedStats = subjectStatsMap.map { (name, stats) ->
                val (sTotal, sAbsent) = stats
                val sRate = if (sTotal > 0) (sAbsent.toDouble() / sTotal * 100) else 0.0
                name to sRate
            }.sortedByDescending { it.second }

            updateTop3(sortedStats)

            val maxDailyTotal = (0..4).maxOfOrNull { dayPresentCounts[it] + dayAbsentCounts[it] } ?: 0
            val finalMax = if (maxDailyTotal > 0) maxDailyTotal else 1
            
            updateDayBar(binding.viewBarMonPresent, binding.viewBarMonAbsent, dayPresentCounts[0], dayAbsentCounts[0], finalMax)
            updateDayBar(binding.viewBarTuePresent, binding.viewBarTueAbsent, dayPresentCounts[1], dayAbsentCounts[1], finalMax)
            updateDayBar(binding.viewBarWedPresent, binding.viewBarWedAbsent, dayPresentCounts[2], dayAbsentCounts[2], finalMax)
            updateDayBar(binding.viewBarThuPresent, binding.viewBarThuAbsent, dayPresentCounts[3], dayAbsentCounts[3], finalMax)
            updateDayBar(binding.viewBarFriPresent, binding.viewBarFriAbsent, dayPresentCounts[4], dayAbsentCounts[4], finalMax)

        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error updating stats", e)
        }
    }

    private fun parseDate(dateStr: String?, parsers: List<SimpleDateFormat>): Date? {
        if (dateStr.isNullOrEmpty()) return null
        val cleanDateStr = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr
        for (sdf in parsers) {
            try {
                sdf.timeZone = KST
                return sdf.parse(cleanDateStr)
            } catch (e: Exception) { /* ignore */ }
        }
        return null
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }

    private fun findMatchingSchedule(recordDate: Date, dayName: String, schedules: List<ClassSchedule>, timeFormat: SimpleDateFormat): ClassSchedule? {
        try {
            timeFormat.timeZone = KST
            val recordTime = timeFormat.parse(timeFormat.format(recordDate)) ?: return null

            val matchingSchedules = schedules.filter { schedule ->
                schedule.day.contains(dayName) && try {
                    val startTime = timeFormat.parse(schedule.startTime) ?: return@filter false
                    val endTime = timeFormat.parse(schedule.endTime) ?: return@filter false
                    !recordTime.before(startTime) && recordTime.before(endTime)
                } catch (e: Exception) {
                    false
                }
            }

            // If there are multiple candidates, pick the one with the longest duration.
            return matchingSchedules.maxByOrNull { schedule ->
                try {
                    val startTime = timeFormat.parse(schedule.startTime)?.time ?: -1
                    val endTime = timeFormat.parse(schedule.endTime)?.time ?: -1
                    if (startTime == -1L || endTime == -1L) Long.MIN_VALUE else endTime - startTime
                } catch (e: Exception) {
                    Long.MIN_VALUE
                }
            }
        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error in findMatchingSchedule", e)
            return null
        }
    }

    private fun updateTop3(sortedStats: List<Pair<String, Double>>) {
        resetTop3()
        
        if (sortedStats.isNotEmpty()) {
            binding.tvTop1Name.text = sortedStats[0].first
            binding.tvTop1Rate.text = String.format(Locale.getDefault(), "%.1f%%", sortedStats[0].second)
        }
        if (sortedStats.size >= 2) {
            binding.tvTop2Name.text = sortedStats[1].first
            binding.tvTop2Rate.text = String.format(Locale.getDefault(), "%.1f%%", sortedStats[1].second)
        }
        if (sortedStats.size >= 3) {
            binding.tvTop3Name.text = sortedStats[2].first
            binding.tvTop3Rate.text = String.format(Locale.getDefault(), "%.1f%%", sortedStats[2].second)
        }
    }

    private fun resetTop3() {
        binding.tvTop1Name.text = "-"
        binding.tvTop1Rate.text = "0%"
        binding.tvTop2Name.text = "-"
        binding.tvTop2Rate.text = "0%"
        binding.tvTop3Name.text = "-"
        binding.tvTop3Rate.text = "0%"
    }

    private fun updateDayBar(presentView: View, absentView: View, presentCount: Int, absentCount: Int, max: Int) {
        if (_binding == null) return
        try {
            val maxBarHeightDp = 150f 
            val density = resources.displayMetrics.density
            
            val presentHeight = if (max > 0) (presentCount.toFloat() / max * maxBarHeightDp) else 0f
            val absentHeight = if (max > 0) (absentCount.toFloat() / max * maxBarHeightDp) else 0f
            
            val minHeight = (1 * density).toInt()
            
            val pParams = presentView.layoutParams
            pParams.height = (presentHeight * density).toInt().coerceAtLeast(if (presentCount > 0) minHeight else 0)
            presentView.layoutParams = pParams
            
            val aParams = absentView.layoutParams
            aParams.height = (absentHeight * density).toInt().coerceAtLeast(if (absentCount > 0) minHeight else 0)
            absentView.layoutParams = aParams
            
        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error updating bar", e)
        }
    }
    
    private fun resetBarChart() {
        if (_binding == null) return
        try {
            val views = listOf(
                binding.viewBarMonPresent, binding.viewBarMonAbsent,
                binding.viewBarTuePresent, binding.viewBarTueAbsent,
                binding.viewBarWedPresent, binding.viewBarWedAbsent,
                binding.viewBarThuPresent, binding.viewBarThuAbsent,
                binding.viewBarFriPresent, binding.viewBarFriAbsent
            )
            
            views.forEach { v ->
                val params = v.layoutParams
                params.height = 0
                v.layoutParams = params
            }
        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error resetting bar chart", e)
        }
    }

    private fun filterAndShowRecords() {
        if (_binding == null || !isAdded) return
        
        try {
            if (!::recordsAdapter.isInitialized) return 

            recordsAdapter.updateSchedules(recordsViewModel.getAllSchedules())

            val selectedDateFilter = binding.spinnerDate.selectedItemPosition
            val selectedSubject = binding.spinnerClass.selectedItem as? String
            
            val allSubjectsString = context?.getString(R.string.records_filter_all_subjects) ?: "전체 과목"
            val isAllSubjects = selectedSubject == allSubjectsString || selectedSubject == null

            val thresholdDate = if (selectedDateFilter == 0) null else {
                Calendar.getInstance(KST).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    val daysToSubtract = when(selectedDateFilter) {
                        1 -> -1
                        2 -> -7
                        3 -> -30
                        else -> 0
                    }
                    add(Calendar.DAY_OF_YEAR, daysToSubtract)
                }.time
            }
            
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )

            val filteredRecords = allRecords.filter { record ->
                val recordDate = parseDate(record.uploadedAt, parsers) ?: return@filter false

                val cal = Calendar.getInstance(KST).apply { time = recordDate }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    return@filter false // Exclude weekends from list view
                }

                val dateMatch = thresholdDate == null || recordDate.after(thresholdDate)

                val subjectMatch = if (isAllSubjects) {
                    true
                } else {
                    val dayName = getDayName(dayOfWeek)
                    val matchingSchedule = findMatchingSchedule(recordDate, dayName, recordsViewModel.getAllSchedules(), SimpleDateFormat("HH:mm", Locale.getDefault()))
                    matchingSchedule?.name == selectedSubject
                }
                
                dateMatch && subjectMatch
            }
            
            updateRecordList(filteredRecords)
            updateStats(filteredRecords)
        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error filtering records", e)
        }
    }

    private fun updateRecordList(records: List<Upload>) {
        if (_binding == null || !isAdded) return
        
        recordsAdapter.submitList(records)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
