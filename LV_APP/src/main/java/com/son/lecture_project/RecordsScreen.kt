package com.son.lecture_project

import android.os.Bundle
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
import com.son.lecture_project.ui.records.RecordsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class RecordsScreen : Fragment() {

    private var _binding: ScreenRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter
    
    private var allRecords: List<Upload> = emptyList()
    private var subjectList: List<String> = emptyList()

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
        observeSubjectList()

        recordsViewModel.loadRecords()
        recordsViewModel.loadSubjectList()
    }

    override fun onResume() {
        super.onResume()
        recordsViewModel.loadSubjectList()
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
        
        // 초기화 시 기본값 설정
        val defaultSubjects = listOf(getString(R.string.records_filter_all_subjects))
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, defaultSubjects)
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClass.adapter = classAdapter

        val dateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(
            getString(R.string.records_filter_all_period), 
            getString(R.string.records_filter_recent_7), 
            getString(R.string.records_filter_recent_30)
        ))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = dateAdapter
        
        // 스피너 선택 시 자동 검색 기능 추가 (선택 사항)
        binding.spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 사용자가 명시적으로 검색 버튼을 누르지 않아도 필터링을 원한다면 여기서 호출 가능
                // filterAndShowRecords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
    
    private fun observeSubjectList() {
        recordsViewModel.subjectList.observe(viewLifecycleOwner) { subjects ->
            val filterList = mutableListOf<String>()
            filterList.add(getString(R.string.records_filter_all_subjects))
            filterList.addAll(subjects)
            
            subjectList = subjects // 나중에 필터링할 때 인덱스 매핑용으로 저장 가능하나, 스피너 값을 직접 쓸 예정
            
            val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterList)
            classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClass.adapter = classAdapter
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

        val totalPresent = records.sumOf { it.peopleCount }
        val avgPresent = totalPresent.toDouble() / records.size
        
        binding.tvAttendanceRate.text = String.format(Locale.getDefault(), "%.1f명", avgPresent)
        binding.progressAttendance.progress = 0 

        binding.tvStatsPresent.text = "${totalPresent}명"
        binding.tvStatsAbsent.text = "-" 

        val dayCounts = MutableList(5) { 0 }
        val dayUploadCounts = MutableList(5) { 0 }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        records.forEach { record ->
            try {
                val dateString = if (record.uploadedAt.contains(".")) {
                     record.uploadedAt.substring(0, record.uploadedAt.indexOf(".")) 
                } else {
                     record.uploadedAt
                }
                
                val date = sdf.parse(dateString)
                if (date != null) {
                    cal.time = date
                    
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                        val index = dayOfWeek - Calendar.MONDAY
                        dayCounts[index] += record.peopleCount
                        dayUploadCounts[index]++
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing error
            }
        }
        
        val maxCount = dayCounts.maxOrNull() ?: 1
        val scale = if (maxCount > 0) maxCount else 1
        
        updateBar(binding.viewBarMon, dayCounts[0], scale)
        updateBar(binding.viewBarTue, dayCounts[1], scale)
        updateBar(binding.viewBarWed, dayCounts[2], scale)
        updateBar(binding.viewBarThu, dayCounts[3], scale)
        updateBar(binding.viewBarFri, dayCounts[4], scale)
    }

    private fun updateBar(view: View, count: Int, max: Int) {
        val maxBarHeight = 120
        val density = resources.displayMetrics.density
        val heightDp = if (max > 0) (count.toFloat() / max * maxBarHeight) else 0f
        
        val params = view.layoutParams
        params.height = (heightDp * density).toInt().coerceAtLeast((1 * density).toInt())
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
        
        // 과목 필터링
        val selectedSubject = binding.spinnerClass.selectedItem as? String
        val isAllSubjects = selectedSubject == getString(R.string.records_filter_all_subjects) || selectedSubject == null

        // 선택된 과목의 시간표 정보 가져오기 (필터링용)
        val targetSchedules = if (!isAllSubjects) {
            recordsViewModel.getSchedulesForSubject(selectedSubject!!)
        } else {
            emptyList()
        }

        // 날짜 필터 계산
        val thresholdDate = if (selectedDateFilter == 0) null else {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val daysToSubtract = if (selectedDateFilter == 1) 7 else 30
            calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            calendar.time
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        val filteredList = allRecords.filter { record ->
            // 1. 날짜/시간 파싱 (공통)
            var recordDateValid = false
            
            // 날짜 파싱 시도
            try {
                val dateString = if (record.uploadedAt.contains(".")) {
                     record.uploadedAt.substring(0, record.uploadedAt.indexOf(".")) 
                } else {
                     record.uploadedAt
                }
                val recordDate = sdf.parse(dateString)
                
                if (recordDate != null) {
                    // 1-1. 날짜 필터링 체크
                    val dateMatch = thresholdDate == null || !recordDate.before(thresholdDate)
                    
                    // 1-2. 과목 필터링 체크 (시간 비교)
                    val subjectMatch = if (isAllSubjects) {
                        true
                    } else {
                        cal.time = recordDate
                        isRecordInSchedules(cal, targetSchedules)
                    }
                    
                    recordDateValid = dateMatch && subjectMatch
                }
            } catch (e: Exception) {
                // 파싱 실패 시, 전체 보기면 포함하지만 필터링 중이면 제외하는게 안전
                recordDateValid = isAllSubjects && thresholdDate == null
            }
            
            recordDateValid
        }
        
        updateRecordList(filteredList)
        updateStats(filteredList) // Recalculate stats based on filter
    }
    
    // 기록된 시간이 해당 과목의 수업 시간 범위 내에 있는지 확인
    private fun isRecordInSchedules(cal: Calendar, schedules: List<ClassSchedule>): Boolean {
        // 1. 요일 확인
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayStr = when (dayOfWeek) {
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            else -> ""
        }
        
        if (dayStr.isEmpty()) return false
        
        // 2. 시간 확인 (분 단위)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val recordTime = hour * 60 + minute
        
        // 해당 요일이면서 시간이 범위 내에 있는 스케줄이 하나라도 있으면 OK
        return schedules.any { schedule ->
            schedule.day.contains(dayStr) && isTimeInRange(recordTime, schedule.startTime, schedule.endTime)
        }
    }
    
    private fun isTimeInRange(recordTime: Int, startStr: String, endStr: String): Boolean {
        val start = parseTimeToMinutes(startStr)
        val end = parseTimeToMinutes(endStr)
        
        if (start == -1 || end == -1) return false
        
        // 여유 시간 ±10분 고려? 일단 정확히 범위 내로
        return recordTime in start..end
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            h * 60 + m
        } catch (e: Exception) {
            -1
        }
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
