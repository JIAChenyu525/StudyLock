package com.studylock.app.data.repository

import android.content.Context
import com.studylock.app.data.database.StudyLockDatabase

class StudyLockRepository private constructor(context: Context) {
    
    private val database = StudyLockDatabase.getDatabase(context)
    
    val semesterRepository = SemesterRepository(database.semesterDao())
    val courseRepository = CourseRepository(database.courseDao())
    val classTimeConfigRepository = ClassTimeConfigRepository(database.classTimeConfigDao())
    val focusRecordRepository = FocusRecordRepository(database.focusRecordDao())
    val userSettingsRepository = UserSettingsRepository(database.userSettingsDao())
    
    companion object {
        @Volatile
        private var INSTANCE: StudyLockRepository? = null
        
        fun getInstance(context: Context): StudyLockRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val instance = StudyLockRepository(context.applicationContext)
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}