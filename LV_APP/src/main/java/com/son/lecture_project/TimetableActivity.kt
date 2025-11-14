package com.son.lecture_project


import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.timetable.databinding.ActivityTimetableBinding
import com.example.timetable.databinding.DialogAddClassBinding
import java.util.UUID

data class ClassItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val day: String,
    val startTime: String,
    val endTime: String,
    val color: Int
)

class TimetableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimetableBinding
    private lateinit var adapter: TimetableAdapter
    private val classList = mutableListOf<ClassItem>()

    private val days = listOf("월요일", "화요일", "수요일", "목요일", "금요일")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TimetableAdapter(classList) { item ->
            AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("\"${item.name}\" 수업을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    classList.remove(item)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "수업이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 5)
        binding.recyclerView.adapter = adapter

        binding.btnAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddClassBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("새 수업 추가")
            .setView(dialogBinding.root)
            .setPositiveButton("추가", null)
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etClassName.text.toString().trim()
                val day = dialogBinding.spinnerDay.selectedItem.toString()
                val startTime = dialogBinding.etStartTime.text.toString()
                val endTime = dialogBinding.etEndTime.text.toString()
                val color = dialogBinding.colorPicker.selectedColor

                if (name.isEmpty()) {
                    Toast.makeText(this, "수업 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                classList.add(ClassItem(name = name, day = day, startTime = startTime, endTime = endTime, color = color))
                adapter.notifyDataSetChanged()
                dialog.dismiss()
                Toast.makeText(this, "수업이 추가되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}
