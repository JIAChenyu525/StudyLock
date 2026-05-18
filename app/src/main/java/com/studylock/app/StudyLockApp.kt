package com.studylock.app

import android.app.Application
import com.studylock.app.data.database.StudyLockDatabase
import com.studylock.app.data.repository.StudyLockRepository

class StudyLockApp : Application() {
    
    companion object {
        lateinit var instance: StudyLockApp
            private set
        
        val database: StudyLockDatabase by lazy {
            StudyLockDatabase.getDatabase(instance)
        }
        
        val repository: StudyLockRepository by lazy {
            StudyLockRepository.getInstance(instance)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}