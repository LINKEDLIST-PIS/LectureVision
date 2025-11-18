package com.son.lecture_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.son.lecture_project.databinding.ActivityAttendanceRecordBinding

class AttendanceRecord : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 이 액티비티가 사용할 레이아웃 파일을 지정합니다.
        binding = ActivityAttendanceRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: 출석 기록 데이터를 불러와 RecyclerView에 표시하는 로직 구현
    }
}
