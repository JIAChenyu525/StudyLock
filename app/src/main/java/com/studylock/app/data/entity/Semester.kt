package com.studylock.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    val startDate: String, // yyyy-MM-dd 格式
    
    val totalWeeks: Int
)