package com.son.lecture_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
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
import java.util.concurrent.TimeUnit

class RecordsScreen : Fragment() {

    private var _binding: ScreenRecordsBinding? = null
    private val binding get() = _binding!!

    private val recordsViewModel: RecordsViewModel by viewModels()
    private lateinit var recordsAdapter: RecordsAdapter
    private var allRecords: List<Upload> = emptyList()
    private var subjectList: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        val ctx = context ?: return

        recordsAdapter = RecordsAdapter()
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = recordsAdapter
        }
    }

    private fun setupTabsAndFilters() {
        val ctx = context ?: return

        val listTabTitle = ctx.getString(R.string.records_tab_list)
        val statsTabTitle = ctx.getString(R.string.records_tab_stats)

        if (binding.tabLayoutViewMode.tabCount == 0) {
            binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(listTabTitle))
            binding.tabLayoutViewMode.addTab(binding.tabLayoutViewMode.newTab().setText(statsTabTitle))
        }

        binding.tabLayoutViewMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isListTab = tab?.position == 0
                binding.contentFrame.visibility = if (isListTab) View.VISIBLE else View.GONE
                binding.layoutStats.visibility = if (isListTab) View.GONE else View.VISIBLE
                binding.tvSummary.visibility = if (isListTab) View.VISIBLE else View.GONE
                binding.scrollFilters.visibility = View.VISIBLE
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val allSubjects = ctx.getString(R.string.records_filter_all_subjects)
        val classAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, listOf(allSubjects))
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

        binding.btnSearch.setOnClickListener {
            if (isAdded) {
                filterAndShowRecords()
                Toast.makeText(requireContext(), getString(R.string.msg_searching), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeRecords() {
        recordsViewModel.records.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe

            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = result.data
                    filterAndShowRecords()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    allRecords = emptyList()
                    filterAndShowRecords()
                    val errorMsg = result.exception.message ?: "Unknown Error"
                    context?.let {
                        Toast.makeText(it, it.getString(R.string.msg_load_fail, errorMsg), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeSubjectList() {
        recordsViewModel.subjectList.observe(viewLifecycleOwner) { subjects ->
            if (!isAdded) return@observe
            val ctx = context ?: return@observe

            val allSubjects = ctx.getString(R.string.records_filter_all_subjects)
            val filterList = mutableListOf(allSubjects).apply { addAll(subjects) }

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

    private fun filterAndShowRecords() {
        if (!isAdded) return

        val selectedSubject = binding.spinnerClass.selectedItem as? String ?: getString(R.string.records_filter_all_subjects)
        val selectedDateFilter = binding.spinnerDate.selectedItemPosition

        val filteredList = allRecords.filter { record ->
            val subjectMatch = selectedSubject == getString(R.string.records_filter_all_subjects) || record.originalName == selectedSubject
            val dateMatch = when (selectedDateFilter) {
                1 -> isWithinDays(record.uploadedAt, 1)
                2 -> isWithinDays(record.uploadedAt, 7)
                3 -> isWithinDays(record.uploadedAt, 30)
                else -> true
            }
            subjectMatch && dateMatch
        }

        recordsAdapter.submitList(filteredList)

        binding.tvSummary.text = getString(R.string.records_total_count, filteredList.size)

        binding.layoutEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRecords.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE

        updateStatistics(filteredList)
    }

    private fun updateStatistics(filteredRecords: List<Upload>) {
        val subjectTotalMap = recordsViewModel.getSubjectTotalCountMap()
        var totalPossibleStudents = 0
        var totalPresentStudents = 0

        val subjectStats = mutableMapOf<String, Pair<Int, Int>>()
        val dailyStats = mutableMapOf<Int, Pair<Int, Int>>().apply {
            (Calendar.MONDAY..Calendar.FRIDAY).forEach { put(it, 0 to 0) }
        }

        filteredRecords.forEach { record ->
            val subjectName = record.originalName
            if (subjectName != null) {
                val maxStudents = subjectTotalMap[subjectName] ?: 0

                if (maxStudents > 0) {
                    totalPossibleStudents += maxStudents
                    totalPresentStudents += record.peopleCount

                    val current = subjectStats.getOrPut(subjectName) { 0 to 0 }
                    subjectStats[subjectName] =
                        (current.first + record.peopleCount) to (current.second + maxStudents)

                    val cal = getKstCalendar(record.uploadedAt) ?: return@forEach
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    if (dailyStats.containsKey(dayOfWeek)) {
                        val dailyCurrent = dailyStats.getOrPut(dayOfWeek) { 0 to 0 }
                        val absent = (maxStudents - record.peopleCount).coerceAtLeast(0)
                        dailyStats[dayOfWeek] =
                            (dailyCurrent.first + record.peopleCount) to (dailyCurrent.second + absent)
                    }
                }
            }
        }

        val presentRate = if (totalPossibleStudents > 0) (totalPresentStudents.toDouble() / totalPossibleStudents * 100).toInt() else 0
        binding.tvRatePresent.text = "$presentRate%"
        binding.tvRateAbsent.text = "${100 - presentRate}%"

        updateTop3Absence(subjectStats)
        updateWeeklyChart(dailyStats)
    }

    private fun updateTop3Absence(subjectStats: Map<String, Pair<Int, Int>>) {
        val absenceRates = subjectStats.mapNotNull { (name, stats) ->
            val (present, possible) = stats
            if (possible > 0) {
                val rate = 100 - (present.toDouble() / possible * 100)
                name to rate
            } else null
        }.sortedByDescending { it.second }

        val topViews = listOf(
            binding.layoutTop1 to (binding.tvTop1Name to binding.tvTop1Rate),
            binding.layoutTop2 to (binding.tvTop2Name to binding.tvTop2Rate),
            binding.layoutTop3 to (binding.tvTop3Name to binding.tvTop3Rate)
        )

        topViews.forEachIndexed { index, (layout, tvs) ->
            val (nameTv, rateTv) = tvs
            if (index < absenceRates.size) {
                layout.isVisible = true
                nameTv.text = absenceRates[index].first
                rateTv.text = "${absenceRates[index].second.toInt()}%"
            } else {
                layout.isVisible = false
            }
        }
    }

    private fun updateWeeklyChart(dailyStats: Map<Int, Pair<Int, Int>>) {
        val barViews = mapOf(
            Calendar.MONDAY to (binding.viewBarMonPresent to binding.viewBarMonAbsent),
            Calendar.TUESDAY to (binding.viewBarTuePresent to binding.viewBarTueAbsent),
            Calendar.WEDNESDAY to (binding.viewBarWedPresent to binding.viewBarWedAbsent),
            Calendar.THURSDAY to (binding.viewBarThuPresent to binding.viewBarThuAbsent),
            Calendar.FRIDAY to (binding.viewBarFriPresent to binding.viewBarFriAbsent)
        )

        val maxDailyTotal = dailyStats.values.map { it.first + it.second }.maxOrNull()?.toFloat() ?: 1f

        barViews.forEach { (day, views) ->
            val (presentView, absentView) = views
            val stats = dailyStats[day] ?: (0 to 0)
            val (present, absent) = stats

            val presentHeight = if (maxDailyTotal > 0) (150 * (present.toFloat() / maxDailyTotal)).toInt().dpToPx() else 0
            val absentHeight = if (maxDailyTotal > 0) (150 * (absent.toFloat() / maxDailyTotal)).toInt().dpToPx() else 0

            presentView.layoutParams.height = presentHeight
            absentView.layoutParams.height = absentHeight

            presentView.requestLayout()
            absentView.requestLayout()
        }
    }

    private fun getKstCalendar(dateString: String?): Calendar? {
        if (dateString == null) return null
        return try {
            val cleanDate = dateString.replace("Z", "").substringBefore(".")
            val format = if (cleanDate.contains("T")) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(cleanDate)
            Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
                if (date != null) time = date
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isWithinDays(dateString: String?, days: Int): Boolean {
        if (dateString == null) return false
        return try {
            val cal = getKstCalendar(dateString) ?: return false
            val diff = Date().time - cal.timeInMillis
            TimeUnit.MILLISECONDS.toDays(diff) < days
        } catch (e: Exception) {
            Log.e("RecordsScreen", "Date parsing failed for: $dateString", e)
            false
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
