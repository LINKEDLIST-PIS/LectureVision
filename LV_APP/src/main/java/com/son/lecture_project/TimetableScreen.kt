package com.son.lecture_project

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.databinding.ScreenTimetableBinding
import com.son.lecture_project.ui.home.Result
import com.son.lecture_project.ui.timetable.TimetableViewModel

class TimetableScreen : Fragment() {

    private var _binding: ScreenTimetableBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel 연결
    private val timetableViewModel: TimetableViewModel by viewModels()

    private val classItems = mutableListOf<ClassSchedule>()
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeTimetable()
        observeActionResult()
        
        // 화면 진입 시 시간표 로드 요청
        timetableViewModel.loadTimetable()

        // 커스텀 뷰 클릭 리스너 설정 (삭제/수정 기능)
        binding.customTimetableView.setClasses(classItems) // 초기엔 빈 리스트 설정
        binding.customTimetableView.setOnClassClickListener { classItem ->
            if (isEditMode) {
                showEditDeleteDialog(classItem)
            } else {
                Toast.makeText(context, "${classItem.name} (${classItem.startTime}~${classItem.endTime})", Toast.LENGTH_SHORT).show()
            }
        }

        // 버튼 리스너 설정
        binding.buttonAddClass.setOnClickListener {
            showAddClassDialog()
        }

        binding.buttonEditClass.setOnClickListener {
            isEditMode = !isEditMode
            if (isEditMode) {
                binding.buttonEditClass.text = getString(R.string.timetable_edit_done)
                binding.buttonEditClass.setBackgroundColor(Color.parseColor("#4CAF50")) // 녹색
                Toast.makeText(context, getString(R.string.msg_select_edit_delete), Toast.LENGTH_SHORT).show()
            } else {
                binding.buttonEditClass.text = getString(R.string.timetable_edit)
                binding.buttonEditClass.setBackgroundColor(Color.parseColor("#EF4444")) // 빨강
            }
        }
    }

    private fun observeTimetable() {
        timetableViewModel.timetable.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // 로딩 중 처리 (필요 시)
                }
                is Result.Success -> {
                    classItems.clear()
                    classItems.addAll(result.data)
                    binding.customTimetableView.setClasses(classItems)
                    
                    if (classItems.isEmpty()) {
                        // 데이터가 없을 때만 토스트 표시
                    }
                }
                is Result.Error -> {
                    Log.e("TimetableScreen", "Load Error", result.exception)
                    classItems.clear()
                    binding.customTimetableView.setClasses(classItems)
                }
            }
        }
    }

    private fun observeActionResult() {
        timetableViewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // 액션 처리 중 로딩 표시 (필요하면 추가)
                }
                is Result.Success -> {
                    Toast.makeText(context, result.data, Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(context, "오류 발생: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTimeInputFormatter(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return

                isFormatting = true
                
                // 1. 입력 길이 제한 (5글자: HH:mm)
                if (s.length > 5) {
                    s.delete(5, s.length)
                }

                // 2. 자동 콜론 추가
                if (s.length == 2 && previousLength < 2 && !s.contains(":")) {
                     s.append(":")
                }
                
                val str = s.toString()
                val colonIndex = str.indexOf(":")
                
                // 3. 시간(Hour) 검증: 0~23
                if (colonIndex != -1) {
                    val hourPart = str.substring(0, colonIndex)
                    if (hourPart.isNotEmpty()) {
                        val hour = hourPart.toIntOrNull()
                        if (hour != null && hour > 23) {
                            s.replace(0, colonIndex, "23")
                        }
                    }
                } else {
                    // 콜론이 없는 경우 (아직 시간 입력 중)
                    if (str.length >= 2) {
                        val hour = str.toIntOrNull()
                        if (hour != null && hour > 23) {
                            s.replace(0, 2, "23")
                            // "23"으로 바꿨으니 콜론 추가
                             if (!s.toString().contains(":")) s.append(":")
                        }
                    }
                }
                
                // 4. 분(Minute) 검증: 0~59
                if (colonIndex != -1 && str.length > colonIndex + 1) {
                    val minPart = str.substring(colonIndex + 1)
                    
                    if (minPart.isNotEmpty()) {
                         // 분의 첫 번째 자리가 6 이상이면 5로 변경 (59분이 최대이므로)
                         val firstDigit = minPart[0].toString().toIntOrNull()
                         if (firstDigit != null && firstDigit > 5) {
                             s.replace(colonIndex + 1, colonIndex + 2, "5")
                         }
                         
                         // 전체 분이 59 초과인지 확인
                         if (minPart.length == 2) {
                             val min = minPart.toIntOrNull()
                             if (min != null && min > 59) {
                                 s.replace(colonIndex + 1, str.length, "59")
                             }
                         }
                    }
                }
                
                isFormatting = false
            }
        })
    }

    private fun showAddClassDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_class, null)
        
        val etClassName = dialogView.findViewById<TextInputEditText>(R.id.et_class_name)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinner_day)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.et_start_time)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.et_end_time)
        
        // 시간 입력 포맷터 적용
        setupTimeInputFormatter(etStartTime)
        setupTimeInputFormatter(etEndTime)

        val radioRed = dialogView.findViewById<RadioButton>(R.id.radio_red)
        val radioBlue = dialogView.findViewById<RadioButton>(R.id.radio_blue)
        val radioYellow = dialogView.findViewById<RadioButton>(R.id.radio_yellow)
        val radioGreen = dialogView.findViewById<RadioButton>(R.id.radio_green)

        val days = arrayOf(
            getString(R.string.day_monday), 
            getString(R.string.day_tuesday), 
            getString(R.string.day_wednesday), 
            getString(R.string.day_thursday), 
            getString(R.string.day_friday)
        )
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = spinnerAdapter

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_class_add_title))
            .setPositiveButton(getString(R.string.dialog_class_add_btn)) { _, _ ->
                val name = etClassName.text.toString()
                val day = spinnerDay.selectedItem.toString()
                val startTimeStr = etStartTime.text.toString().trim()
                val endTimeStr = etEndTime.text.toString().trim()
                
                if (name.isEmpty()) {
                     Toast.makeText(context, getString(R.string.msg_enter_class_name), Toast.LENGTH_SHORT).show()
                     return@setPositiveButton
                }

                // 시간 유효성 검사
                if (!isValidTimeFormat(startTimeStr) || !isValidTimeFormat(endTimeStr)) {
                    Toast.makeText(context, "시간 형식이 올바르지 않습니다. (HH:mm)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val color = when {
                    radioRed.isChecked -> "#FF6B6B"
                    radioBlue.isChecked -> "#45B7D1"
                    radioYellow.isChecked -> "#FFD93D"
                    radioGreen.isChecked -> "#4ECDC4"
                    else -> "#FF6B6B"
                }

                // ViewModel을 통한 API 호출로 변경
                timetableViewModel.addClass(name, day, startTimeStr, endTimeStr, null, color)
            }
            .setNegativeButton(getString(R.string.dialog_class_cancel_btn), null)
            .show()
    }

    private fun showEditDeleteDialog(classItem: ClassSchedule) {
        val options = arrayOf(getString(R.string.dialog_option_edit), getString(R.string.dialog_option_delete))
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setTitle(getString(R.string.dialog_manage_title, classItem.name))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(classItem)
                    1 -> deleteClass(classItem)
                }
            }
            .show()
    }

    private fun showEditDialog(classItem: ClassSchedule) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_class, null)

        val etClassName = dialogView.findViewById<TextInputEditText>(R.id.et_class_name)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinner_day)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.et_start_time)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.et_end_time)

        etClassName.setText(classItem.name)
        etStartTime.setText(classItem.startTime)
        etEndTime.setText(classItem.endTime)

        // 시간 입력 포맷터 적용
        setupTimeInputFormatter(etStartTime)
        setupTimeInputFormatter(etEndTime)

        val days = arrayOf(
            getString(R.string.day_monday), 
            getString(R.string.day_tuesday), 
            getString(R.string.day_wednesday), 
            getString(R.string.day_thursday), 
            getString(R.string.day_friday)
        )
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = spinnerAdapter

        val dayIndex = days.indexOfFirst { classItem.day.contains(it) }
        if (dayIndex != -1) {
            spinnerDay.setSelection(dayIndex)
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Lecture_project_AlertDialog)
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_class_edit_title))
            .setPositiveButton(getString(R.string.dialog_class_edit_btn)) { _, _ ->
                val name = etClassName.text.toString()
                val day = spinnerDay.selectedItem.toString()
                val startTimeStr = etStartTime.text.toString().trim()
                val endTimeStr = etEndTime.text.toString().trim()
                
                // 시간 유효성 검사
                if (!isValidTimeFormat(startTimeStr) || !isValidTimeFormat(endTimeStr)) {
                    Toast.makeText(context, "시간 형식이 올바르지 않습니다. (HH:mm)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // ViewModel을 통한 API 호출로 변경
                timetableViewModel.updateClass(classItem.id, name, day, startTimeStr, endTimeStr, classItem.classroom, classItem.color)
            }
            .setNegativeButton(getString(R.string.dialog_class_cancel_btn), null)
            .show()
    }

    private fun deleteClass(classItem: ClassSchedule) {
        // ViewModel을 통한 API 호출로 변경
        timetableViewModel.deleteClass(classItem.id)
    }

    private fun isValidTimeFormat(time: String): Boolean {
        return time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
