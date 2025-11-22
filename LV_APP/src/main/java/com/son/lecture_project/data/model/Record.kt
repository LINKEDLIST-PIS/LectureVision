package com.son.lecture_project.data.model

import java.util.Date

data class Record(
    val id: String,
    val courseName: String,
    val date: Date,
    val status: String // e.g., "출석", "결석", "지각"
)
