package com.studylock.app.data.repository

import com.studylock.app.data.dao.CourseDao
import com.studylock.app.data.entity.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) : BaseRepository() {
    
    suspend fun insertCourse(course: Course): Long {
        return courseDao.insert(course)
    }
    
    suspend fun updateCourse(course: Course) {
        courseDao.update(course)
    }
    
    suspend fun deleteCourse(course: Course) {
        courseDao.delete(course)
    }
    
    suspend fun deleteCourseById(id: Long) {
        courseDao.deleteById(id)
    }
    
    suspend fun deleteCoursesBySemesterId(semesterId: Long) {
        courseDao.deleteBySemesterId(semesterId)
    }
    
    suspend fun getCourseById(id: Long): Course? {
        return courseDao.getById(id)
    }
    
    fun getCoursesBySemesterId(semesterId: Long): Flow<List<Course>> {
        return courseDao.getBySemesterId(semesterId)
    }

    suspend fun getCoursesBySemesterIdOnce(semesterId: Long): List<Course> {
        return courseDao.getBySemesterIdOnce(semesterId)
    }
    
    fun getCoursesBySemesterIdAndWeekday(semesterId: Long, weekday: Int): Flow<List<Course>> {
        return courseDao.getBySemesterIdAndWeekday(semesterId, weekday)
    }
    
    fun getCoursesByWeek(semesterId: Long, weekday: Int, week: Int): Flow<List<Course>> {
        return courseDao.getCoursesByWeek(semesterId, weekday, week)
    }
    
    suspend fun getCourseCountBySemesterId(semesterId: Long): Int {
        return courseDao.getCountBySemesterId(semesterId)
    }
    
    fun searchCoursesByName(keyword: String): Flow<List<Course>> {
        return courseDao.searchByName(keyword)
    }
    
    suspend fun insertCourses(courses: List<Course>) {
        courseDao.insertAll(courses)
    }
    
    fun validateCourseData(course: Course): Boolean {
        if (course.name.isBlank() || course.name.length > 20) {
            return false
        }
        
        // 验证星期数范围
        if (course.weekday < 1 || course.weekday > 7) {
            return false
        }
        
        // 验证节次范围
        if (course.startSection < 1 || course.endSection < course.startSection) {
            return false
        }
        
        // 验证周数范围
        if (course.startWeek < 1 || course.endWeek < course.startWeek) {
            return false
        }
        
        // 验证周类型
        if (course.weekType !in 0..2) {
            return false
        }
        
        return true
    }
}