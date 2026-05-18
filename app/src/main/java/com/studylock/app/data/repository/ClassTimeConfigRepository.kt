package com.studylock.app.data.repository

import com.studylock.app.data.dao.ClassTimeConfigDao
import com.studylock.app.data.entity.ClassTimeConfig
import kotlinx.coroutines.flow.Flow

class ClassTimeConfigRepository(private val classTimeConfigDao: ClassTimeConfigDao) : BaseRepository() {
    
    suspend fun insertConfig(config: ClassTimeConfig): Long {
        return classTimeConfigDao.insert(config)
    }
    
    suspend fun updateConfig(config: ClassTimeConfig) {
        classTimeConfigDao.update(config)
    }
    
    suspend fun deleteConfig(config: ClassTimeConfig) {
        classTimeConfigDao.delete(config)
    }
    
    suspend fun deleteConfigById(id: Long) {
        classTimeConfigDao.deleteById(id)
    }
    
    suspend fun getConfigById(id: Long): ClassTimeConfig? {
        return classTimeConfigDao.getById(id)
    }
    
    suspend fun getConfigBySectionNo(sectionNo: Int): ClassTimeConfig? {
        return classTimeConfigDao.getBySectionNo(sectionNo)
    }
    
    fun getAllConfigs(): Flow<List<ClassTimeConfig>> {
        return classTimeConfigDao.getAll()
    }

    suspend fun getAllConfigsOnce(): List<ClassTimeConfig> {
        return classTimeConfigDao.getAllOnce()
    }
    
    suspend fun getConfigsBySectionRange(startSection: Int, endSection: Int): List<ClassTimeConfig> {
        return classTimeConfigDao.getBySectionRange(startSection, endSection)
    }
    
    suspend fun getConfigCount(): Int {
        return classTimeConfigDao.getCount()
    }
    
    suspend fun insertConfigs(configs: List<ClassTimeConfig>) {
        classTimeConfigDao.insertAll(configs)
    }

    suspend fun deleteAllConfigs() {
        classTimeConfigDao.deleteAll()
    }
    
    fun validateTimeFormat(time: String): Boolean {
        return time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }
    
    suspend fun getClassDuration(sectionNo: Int): Int? {
        val config = getConfigBySectionNo(sectionNo)
        return config?.let {
            val start = it.startTime.split(":").map { it.toInt() }
            val end = it.endTime.split(":").map { it.toInt() }
            (end[0] * 60 + end[1]) - (start[0] * 60 + start[1])
        }
    }
}