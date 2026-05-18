package com.studylock.app.data.dao

import androidx.room.*
import com.studylock.app.data.entity.ClassTimeConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassTimeConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ClassTimeConfig): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ClassTimeConfig>)
    
    @Update
    suspend fun update(config: ClassTimeConfig)
    
    @Delete
    suspend fun delete(config: ClassTimeConfig)
    
    @Query("DELETE FROM class_time_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM class_time_configs WHERE id = :id")
    suspend fun getById(id: Long): ClassTimeConfig?
    
    @Query("SELECT * FROM class_time_configs WHERE sectionNo = :sectionNo")
    suspend fun getBySectionNo(sectionNo: Int): ClassTimeConfig?
    
    @Query("SELECT * FROM class_time_configs ORDER BY sectionNo")
    fun getAll(): Flow<List<ClassTimeConfig>>

    @Query("SELECT * FROM class_time_configs ORDER BY sectionNo")
    suspend fun getAllOnce(): List<ClassTimeConfig>
    
    @Query("SELECT * FROM class_time_configs WHERE sectionNo BETWEEN :startSection AND :endSection ORDER BY sectionNo")
    suspend fun getBySectionRange(startSection: Int, endSection: Int): List<ClassTimeConfig>
    
    @Query("SELECT COUNT(*) FROM class_time_configs")
    suspend fun getCount(): Int

    @Query("DELETE FROM class_time_configs")
    suspend fun deleteAll()
}