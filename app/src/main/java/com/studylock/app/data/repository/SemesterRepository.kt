package com.studylock.app.data.repository

import com.studylock.app.data.dao.SemesterDao
import com.studylock.app.data.entity.Semester
import kotlinx.coroutines.flow.Flow

class SemesterRepository(private val semesterDao: SemesterDao) : BaseRepository() {
    
    suspend fun insertSemester(semester: Semester): Long {
        return semesterDao.insert(semester)
    }
    
    suspend fun updateSemester(semester: Semester) {
        semesterDao.update(semester)
    }
    
    suspend fun deleteSemester(semester: Semester) {
        semesterDao.delete(semester)
    }
    
    suspend fun deleteSemesterById(id: Long) {
        semesterDao.deleteById(id)
    }
    
    suspend fun getSemesterById(id: Long): Semester? {
        return semesterDao.getById(id)
    }
    
    fun getAllSemesters(): Flow<List<Semester>> {
        return semesterDao.getAll()
    }

    suspend fun getAllSemestersOnce(): List<Semester> {
        return semesterDao.getAllOnce()
    }
    
    fun searchSemestersByName(keyword: String): Flow<List<Semester>> {
        return semesterDao.searchByName(keyword)
    }
    
    suspend fun getLatestSemester(): Semester? {
        return semesterDao.getLatest()
    }
    
    suspend fun insertSemesters(semesters: List<Semester>) {
        semesterDao.insertAll(semesters)
    }
}