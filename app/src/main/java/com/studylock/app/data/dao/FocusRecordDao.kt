package com.studylock.app.data.dao

import androidx.room.*
import com.studylock.app.data.entity.FocusRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FocusRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<FocusRecord>)
    
    @Update
    suspend fun update(record: FocusRecord)
    
    @Delete
    suspend fun delete(record: FocusRecord)
    
    @Query("DELETE FROM focus_records WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM focus_records WHERE id = :id")
    suspend fun getById(id: Long): FocusRecord?
    
    @Query("SELECT * FROM focus_records WHERE date = :date")
    fun getByDate(date: String): Flow<List<FocusRecord>>

    @Query("SELECT * FROM focus_records WHERE date = :date")
    suspend fun getByDateOnce(date: String): List<FocusRecord>
    
    @Query("SELECT * FROM focus_records WHERE courseId = :courseId")
    fun getByCourseId(courseId: Long): Flow<List<FocusRecord>>
    
    @Query("SELECT * FROM focus_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<FocusRecord>>
    
    @Query("SELECT SUM(durationSec) FROM focus_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDurationByDateRange(startDate: String, endDate: String): Long?
    
    @Query("SELECT COUNT(*) FROM focus_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCountByDateRange(startDate: String, endDate: String): Int
    
    @Query("SELECT SUM(unlockCount) FROM focus_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalUnlockCountByDateRange(startDate: String, endDate: String): Long?
    
    @Query("SELECT * FROM focus_records ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<FocusRecord>>
    
    @Query("SELECT DISTINCT date FROM focus_records ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDates(limit: Int): List<String>
}