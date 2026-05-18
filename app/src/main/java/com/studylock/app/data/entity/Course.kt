package com.studylock.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semesterId"])]
)
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String, // 必填，最多20字
    
    val location: String? = null,
    
    val teacher: String? = null,
    
    val weekday: Int, // 1-7 (周一至周日)
    
    val startSection: Int,
    
    val endSection: Int,
    
    val startWeek: Int,
    
    val endWeek: Int,
    
    val weekType: Int, // 0:全部, 1:单周, 2:双周
    
    val colorTag: String, // hex色值，如 "#FF6750A4"
    
    val semesterId: Long
)