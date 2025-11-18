package com.son.lecture_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.son.lecture_project.databinding.FragmentTimetableBinding
import com.son.lecture_project.data.model.ClassSchedule // 데이터 모델 import

class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TimetableAdapter
    private val classItems = mutableListOf<ClassSchedule>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTimetableData()

        binding.buttonAddClass.setOnClickListener {
            // TODO: React 코드의 Dialog처럼, 수업 추가를 위한 다이얼로그나 새 화면을 띄워야 합니다.
            Toast.makeText(context, "수업 추가 기능 구현 필요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {

        adapter = TimetableAdapter(classItems) { classItem ->
            showDeleteConfirmDialog(classItem)
        }
        binding.recyclerViewTimetable.adapter = adapter

        binding.recyclerViewTimetable.layoutManager = GridLayoutManager(context, 5)
    }

    private fun showDeleteConfirmDialog(classItem: ClassSchedule) {
        AlertDialog.Builder(requireContext())
            .setTitle("수업 삭제")
            .setMessage("'${classItem.name}' 수업을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                // TODO: 실제 데이터베이스나 서버에서 데이터를 삭제하는 로직이 필요합니다.
                val position = classItems.indexOf(classItem)
                if (position != -1) {
                    classItems.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    Toast.makeText(context, "'${classItem.name}' 수업이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun loadTimetableData() {
        // TODO: 이 부분은 실제 서버나 데이터베이스에서 시간표 데이터를 가져와야 합니다.

        classItems.clear()
        classItems.addAll(getDummyTimetableData())
        adapter.notifyDataSetChanged()
    }

    private fun getDummyTimetableData(): List<ClassSchedule> {

        return listOf(
            ClassSchedule(1, "웹 프로그래밍", "월요일", "09:00", "10:30", "#FF6B6B"),
            ClassSchedule(2, "자료구조", "화요일", "10:00", "12:00", "#4ECDC4"),
            ClassSchedule(3, "알고리즘", "수요일", "13:00", "15:00", "#FFD93D"),
            ClassSchedule(4, "운영체제", "목요일", "11:00", "12:30", "#A8A4FF"),
            ClassSchedule(5, "컴퓨터 네트워크", "금요일", "14:00", "16:00", "#45B7D1")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}