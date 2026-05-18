package com.studylock.app.data.repository

import com.studylock.app.data.dao.FocusRecordDao
import com.studylock.app.data.entity.FocusRecord
import kotlinx.coroutines.flow.Flow

class FocusRecordRepository(private val focusRecordDao: FocusRecordDao) : BaseRepository() {
    
    suspend fun insertRecord(record: FocusRecord): Long {
        return focusRecordDao.insert(record)
    }
    
    suspend fun updateRecord(record: FocusRecord) {
        focusRecordDao.update(record)
    }
    
    suspend fun deleteRecord(record: FocusRecord) {
        focusRecordDao.delete(record)
    }
    
    suspend fun deleteRecordById(id: Long) {
        focusRecordDao.deleteById(id)
    }
    
    suspend fun getRecordById(id: Long): FocusRecord? {
        return focusRecordDao.getById(id)
    }
    
    fun getRecordsByDate(date: String): Flow<List<FocusRecord>> {
        return focusRecordDao.getByDate(date)
    }

    suspend fun getRecordsByDateOnce(date: String): List<FocusRecord> {
        return focusRecordDao.getByDateOnce(date)
    }
    
    fun getRecordsByCourseId(courseId: Long): Flow<List<FocusRecord>> {
        return focusRecordDao.getByCourseId(courseId)
    }
    
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<FocusRecord>> {
        return focusRecordDao.getByDateRange(startDate, endDate)
    }
    
    suspend fun getTotalDurationByDateRange(startDate: String, endDate: String): Long {
        return focusRecordDao.getTotalDurationByDateRange(startDate, endDate) ?: 0L
    }
    
    suspend fun getRecordCountByDateRange(startDate: String, endDate: String): Int {
        return focusRecordDao.getCountByDateRange(startDate, endDate)
    }
    
    suspend fun getTotalUnlockCountByDateRange(startDate: String, endDate: String): Long {
        return focusRecordDao.getTotalUnlockCountByDateRange(startDate, endDate) ?: 0L
    }
    
    fun getAllRecords(): Flow<List<FocusRecord>> {
        return focusRecordDao.getAll()
    }
    
    suspend fun getRecentDates(limit: Int = 30): List<String> {
        return focusRecordDao.getRecentDates(limit)
    }
    
    suspend fun insertRecords(records: List<FocusRecord>) {
        focusRecordDao.insertAll(records)
    }
    
    fun formatDuration(durationSec: Int): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        
        return if (hours > 0) {
            "${hours}小时${minutes}分钟${seconds}秒"
        } else if (minutes > 0) {
            "${minutes}分钟${seconds}秒"
        } else {
            "${seconds}秒"
        }
    }
    
    suspend fun getWeeklyStats(startDate: String, endDate: String): Map<String, Any> {
        val totalDuration = getTotalDurationByDateRange(startDate, endDate)
        val recordCount = getRecordCountByDateRange(startDate, endDate)
        val unlockCount = getTotalUnlockCountByDateRange(startDate, endDate)
        
        return mapOf(
            "totalDuration" to totalDuration,
            "recordCount" to recordCount,
            "unlockCount" to unlockCount,
            "averageDuration" to if (recordCount > 0) totalDuration / recordCount else 0L
        )
    }
}