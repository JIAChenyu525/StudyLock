package com.studylock.app.data.dao

import androidx.room.*
import com.studylock.app.data.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)
    
    @Update
    suspend fun update(course: Course)
    
    @Delete
    suspend fun delete(course: Course)
    
    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteBySemesterId(semesterId: Long)
    
    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): Course?
    
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY weekday, startSection")
    fun getBySemesterId(semesterId: Long): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY weekday, startSection")
    suspend fun getBySemesterIdOnce(semesterId: Long): List<Course>
    
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId AND weekday = :weekday ORDER BY startSection")
    fun getBySemesterIdAndWeekday(semesterId: Long, weekday: Int): Flow<List<Course>>
    
    @Query("""
        SELECT * FROM courses 
        WHERE semesterId = :semesterId 
        AND weekday = :weekday 
        AND :week BETWEEN startWeek AND endWeek
        AND (
            weekType = 0 
            OR (weekType = 1 AND :week % 2 = 1) 
            OR (weekType = 2 AND :week % 2 = 0)
        )
        ORDER BY startSection
    """)
    fun getCoursesByWeek(semesterId: Long, weekday: Int, week: Int): Flow<List<Course>>
    
    @Query("SELECT COUNT(*) FROM courses WHERE semesterId = :semesterId")
    suspend fun getCountBySemesterId(semesterId: Long): Int
    
    @Query("SELECT * FROM courses WHERE name LIKE '%' || :keyword || '%' ORDER BY weekday, startSection")
    fun searchByName(keyword: String): Flow<List<Course>>
}