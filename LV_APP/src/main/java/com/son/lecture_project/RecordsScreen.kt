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

    // [리팩토링] 출석 데이터 처리용 데이터 클래스
    private data class AttendanceLog(
        val className: String,
        val sessionDate: String, // "yyyy-MM-dd"
        val timestamp: Date,     // 실제 측정 시간 (최신 데이터 판별용)
        val present: Int,
        val total: Int,
        val dayIndex: Int        // 0=월, 1=화, ..., 6=일
    ) {
        // 0~100 범위로 제한된 출석률
        val attendanceRate: Double
            get() = if (total > 0) (present.toDouble() / total * 100.0).coerceIn(0.0, 100.0) else 0.0
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
        
        val dummyListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Do nothing, wait for Search button click
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerClass.onItemSelectedListener = dummyListener
        binding.spinnerDate.onItemSelectedListener = dummyListener

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
                    filterAndShowRecords()
                    
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

    /**
     * [리팩토링] 통계 계산 로직 통합 함수
     */
    private fun updateStats(records: List<Upload>) {
        if (_binding == null || !isAdded) return
        try {
            // 1. 데이터가 없으면 초기화 후 종료
            if (records.isEmpty()) {
                resetStatsUI()
                return
            }

            // 파서 준비
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = KST }
            val sessionDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = KST }
            
            val allSchedules = recordsViewModel.getAllSchedules()

            // 2. Raw Data -> Valid Log Candidates 매핑
            val candidates = records.mapNotNull { record ->
                val recordDate = parseDate(record.uploadedAt, parsers) ?: return@mapNotNull null
                
                // 주말 체크 (토=7, 일=1) - 아예 제외
                val cal = Calendar.getInstance(KST).apply { time = recordDate }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    return@mapNotNull null
                }
                
                // 스케줄 매칭
                val matchedSchedule = if (allSchedules.any { it.name == record.originalName }) {
                    allSchedules.find { it.name == record.originalName }
                } else {
                    val dayName = getDayName(dayOfWeek) 
                    findMatchingSchedule(recordDate, dayName, allSchedules, timeFormat)
                }

                if (matchedSchedule == null || matchedSchedule.totalStudents <= 0) {
                    null // 매칭 안되거나 정원이 0인 경우 제외
                } else {
                    // 요일 인덱스 계산 (월=0, ..., 금=4)
                    // Calendar.MONDAY=2 -> 0
                    val dayIndex = dayOfWeek - Calendar.MONDAY
                    
                    // 세션 키 생성 (과목명 + 날짜) -> 같은 날짜 같은 과목 구분
                    val sessionDate = sessionDateFormat.format(recordDate)
                    
                    AttendanceLog(
                        className = matchedSchedule.name,
                        sessionDate = sessionDate,
                        timestamp = recordDate,
                        present = record.peopleCount,
                        total = matchedSchedule.totalStudents,
                        dayIndex = dayIndex
                    )
                }
            }

            // 3. [핵심] 같은 날짜+과목에 대해 '가장 마지막' 측정값만 선택
            val uniqueLogs = candidates
                .groupBy { "${it.className}_${it.sessionDate}" }
                .map { (_, logs) ->
                    // timestamp가 가장 늦은 기록 선택
                    logs.maxByOrNull { it.timestamp.time }!!
                }

            if (uniqueLogs.isEmpty()) {
                resetStatsUI()
                return
            }

            // 디버깅 로그
            Log.d("StatsLogic", "Original Records: ${records.size}, Candidates: ${candidates.size}, Unique Sessions: ${uniqueLogs.size}")

            // 4. 전체 통계 계산 (평균 방식)
            // 출석률 = (각 세션의 출석률 합) / 세션 수
            val overallAttendanceRate = uniqueLogs.map { it.attendanceRate }.average()
            // 결석률 = 100 - 출석률
            val overallAbsenceRate = 100.0 - overallAttendanceRate

            // UI 적용 (0~100 범위 제한 및 소수점 포맷팅)
            binding.tvRatePresent.text = String.format(Locale.getDefault(), "%.1f%%", overallAttendanceRate.coerceIn(0.0, 100.0))
            binding.tvRateAbsent.text = String.format(Locale.getDefault(), "%.1f%%", overallAbsenceRate.coerceIn(0.0, 100.0))


            // 5. 과목별 결석률 상위 3 (평균 방식)
            val subjectStats = uniqueLogs
                .groupBy { it.className }
                .map { (name, logs) ->
                    val avgAttendance = logs.map { it.attendanceRate }.average()
                    val avgAbsence = 100.0 - avgAttendance
                    name to avgAbsence
                }
                .sortedByDescending { it.second } // 결석률 높은 순

            updateTop3(subjectStats)


            // 6. 요일별 통계 (월~금)
            // 차트에는 '누적 인원'을 표시하여 시각적 볼륨감을 줌 (최종 선택된 데이터 기준)
            val dayPresentCounts = IntArray(5) { 0 }
            val dayAbsentCounts = IntArray(5) { 0 }

            uniqueLogs.forEach { log ->
                if (log.dayIndex in 0..4) { // 월~금 확인
                    // 인원 수 보정 (음수 방지, 정원 초과 방지)
                    val validPresent = log.present.coerceIn(0, log.total)
                    val validAbsent = log.total - validPresent
                    
                    dayPresentCounts[log.dayIndex] += validPresent
                    dayAbsentCounts[log.dayIndex] += validAbsent
                }
            }

            val maxDailyTotal = (0..4).maxOfOrNull { dayPresentCounts[it] + dayAbsentCounts[it] } ?: 0
            val finalMax = if (maxDailyTotal > 0) maxDailyTotal else 1

            updateDayBar(binding.viewBarMonPresent, binding.viewBarMonAbsent, dayPresentCounts[0], dayAbsentCounts[0], finalMax)
            updateDayBar(binding.viewBarTuePresent, binding.viewBarTueAbsent, dayPresentCounts[1], dayAbsentCounts[1], finalMax)
            updateDayBar(binding.viewBarWedPresent, binding.viewBarWedAbsent, dayPresentCounts[2], dayAbsentCounts[2], finalMax)
            updateDayBar(binding.viewBarThuPresent, binding.viewBarThuAbsent, dayPresentCounts[3], dayAbsentCounts[3], finalMax)
            updateDayBar(binding.viewBarFriPresent, binding.viewBarFriAbsent, dayPresentCounts[4], dayAbsentCounts[4], finalMax)

        } catch (e: Exception) {
            Log.e("RecordsScreen", "Error updating stats", e)
            resetStatsUI()
        }
    }

    private fun resetStatsUI() {
        binding.tvRatePresent.text = "0.0%"
        binding.tvRateAbsent.text = "0.0%"
        resetTop3()
        resetBarChart()
    }

    private fun parseDate(dateStr: String?, parsers: List<SimpleDateFormat>): Date? {
        if (dateStr.isNullOrEmpty()) return null
        val cleanDateStr = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr
        for (sdf in parsers) {
            try {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
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
            // [중요] 시간 비교 시 KST 타임존 필수 설정
            timeFormat.timeZone = KST

            // recordDate에서 날짜 정보를 제거하고 '시간'만 남김 (예: "14:30")
            val recordTimeStr = timeFormat.format(recordDate)
            val recordTime = timeFormat.parse(recordTimeStr) ?: return null

            // 조건에 맞는 스케줄을 찾아 반환 (find 사용으로 성능 최적화)
            return schedules.find { schedule ->
                // 1. 요일 이름("월", "화" 등)이 포함되어 있는지 확인
                schedule.day.contains(dayName) && try {
                    val startTime = timeFormat.parse(schedule.startTime) ?: return@find false
                    val endTime = timeFormat.parse(schedule.endTime) ?: return@find false

                    // 2. 기록된 시간이 수업 시간 범위 내인지 확인 (시작 <= 기록 < 종료)
                    !recordTime.before(startTime) && recordTime.before(endTime)
                } catch (e: Exception) {
                    false
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
            recordsAdapter.updateColorMap(recordsViewModel.getSubjectColorMap())

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

                // [중요] 토/일 데이터 원천 차단
                val cal = Calendar.getInstance(KST).apply { time = recordDate }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return@filter false

                val dateMatch = thresholdDate == null || !recordDate.before(thresholdDate)

                val subjectMatch = if (isAllSubjects) {
                    true
                } else {
                    val nameToCheck = record.originalName ?: ""
                    if (nameToCheck.isNotEmpty() && nameToCheck != "photo.jpg" && !nameToCheck.startsWith("photo_")) {
                         nameToCheck == selectedSubject
                    } else {
                        val dayName = getDayName(dayOfWeek)
                        val matchingSchedule = findMatchingSchedule(recordDate, dayName, recordsViewModel.getAllSchedules(), SimpleDateFormat("HH:mm", Locale.getDefault()))
                        matchingSchedule?.name == selectedSubject
                    }
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
