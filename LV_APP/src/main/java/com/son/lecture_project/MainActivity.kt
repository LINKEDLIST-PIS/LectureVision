package com.son.lecture_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.son.lecture_project.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (savedInstanceState == null) {
            replaceFragment(HomeScreen())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->


            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }


            when (item.itemId) {

                R.id.nav_home -> replaceFragment(HomeScreen())


                R.id.nav_timetable -> replaceFragment(TimetableScreen())


                R.id.nav_records -> replaceFragment(RecordsScreen())


                R.id.nav_settings -> replaceFragment(SettingsScreen())
            }
            true
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}
