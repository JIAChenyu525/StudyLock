package com.studylock.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_records",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class FocusRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val date: String, // yyyy-MM-dd 格式
    
    val courseId: Long? = null,
    
    val durationSec: Int, // 专注时长（秒）
    
    val unlockCount: Int // 解锁次数
)