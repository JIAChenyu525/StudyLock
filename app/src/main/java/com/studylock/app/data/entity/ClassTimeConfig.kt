package com.studylock.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_time_configs")
data class ClassTimeConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sectionNo: Int, // 节次号 (1-10)
    
    val startTime: String, // HH:mm 格式
    
    val endTime: String // HH:mm 格式
)