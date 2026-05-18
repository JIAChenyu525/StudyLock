package com.studylock.app.data.dao

import androidx.room.*
import com.studylock.app.data.entity.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(semester: Semester): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(semesters: List<Semester>)
    
    @Update
    suspend fun update(semester: Semester)
    
    @Delete
    suspend fun delete(semester: Semester)
    
    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getById(id: Long): Semester?
    
    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    fun getAll(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    suspend fun getAllOnce(): List<Semester>
    
    @Query("SELECT * FROM semesters WHERE name LIKE '%' || :keyword || '%' ORDER BY startDate DESC")
    fun searchByName(keyword: String): Flow<List<Semester>>
    
    @Query("SELECT * FROM semesters ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatest(): Semester?
}