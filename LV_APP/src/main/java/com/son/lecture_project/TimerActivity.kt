package com.son.lecture_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.son.lecture_project.databinding.ActivityTimerBinding
import java.util.concurrent.TimeUnit

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftInMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNumberPicker()

        binding.buttonStartStopTimer.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }
    }

    private fun setupNumberPicker() {
        binding.numberPickerMinutes.minValue = 1
        binding.numberPickerMinutes.maxValue = 180
        binding.numberPickerMinutes.value = 90 // 기본값
    }

    private fun startTimer() {
        val minutes = binding.numberPickerMinutes.value
        timeLeftInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                // 타이머가 끝나면, 성공했다는 결과(RESULT_OK)를 담아 액티비티를 종료한다.
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }.start()

        isTimerRunning = true
        updateUI()
    }

    private fun stopTimer() {
        timer?.cancel()
        isTimerRunning = false
        updateUI()
    }

    private fun updateTimerDisplay() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) - TimeUnit.MINUTES.toSeconds(minutes)
        binding.textTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateUI() {
        if (isTimerRunning) {
            binding.layoutTimePicker.visibility = View.GONE
            binding.textTimerDisplay.visibility = View.VISIBLE
            binding.buttonStartStopTimer.text = "중지"
        } else {
            binding.layoutTimePicker.visibility = View.VISIBLE
            binding.textTimerDisplay.visibility = View.GONE
            binding.buttonStartStopTimer.text = "출석 체크 시작"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
