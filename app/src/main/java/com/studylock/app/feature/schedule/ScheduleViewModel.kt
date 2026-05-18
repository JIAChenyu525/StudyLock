package com.studylock.app.feature.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studylock.app.StudyLockApp
import com.studylock.app.data.entity.ClassTimeConfig
import com.studylock.app.data.entity.Course
import com.studylock.app.data.entity.Semester
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val semesters: List<Semester> = emptyList(),
    val selectedSemester: Semester? = null,
    val currentWeek: Int = 1,
    val totalWeeks: Int = 20,
    val courses: List<Course> = emptyList(),
    val classTimeConfigs: List<ClassTimeConfig> = emptyList(),
    val maxSection: Int = 14,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDetailDialog: Boolean = false,
    val showOcrImport: Boolean = false,
    val selectedCourse: Course? = null,
    val showTimeConfig: Boolean = false
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyLockApp.repository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSemesters()
        loadClassTimeConfigs()
    }

    private var courseCollectorJob: kotlinx.coroutines.Job? = null

    private fun loadSemesters() {
        viewModelScope.launch {
            repository.semesterRepository.getAllSemesters()
                .catch { e ->
                    android.util.Log.e("ScheduleVM", "加载学期失败", e)
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { semesters ->
                    val currentSemester = _uiState.value.selectedSemester ?: semesters.firstOrNull()
                    _uiState.update { it.copy(semesters = semesters, selectedSemester = currentSemester) }
                    currentSemester?.let { loadCoursesForSemester(it.id) }
                }
        }
    }

    private fun loadClassTimeConfigs() {
        viewModelScope.launch {
            repository.classTimeConfigRepository.getAllConfigs()
                .catch { e ->
                    android.util.Log.e("ScheduleVM", "加载节次配置失败", e)
                    _uiState.update { it.copy(classTimeConfigs = emptyList()) }
                }
                .collect { configs ->
                    val maxSection = configs.maxOfOrNull { it.sectionNo } ?: 14
                    _uiState.update { it.copy(classTimeConfigs = configs, maxSection = maxSection) }
                }
        }
    }

    private fun loadCoursesForSemester(semesterId: Long) {
        courseCollectorJob?.cancel()
        courseCollectorJob = viewModelScope.launch {
            repository.courseRepository.getCoursesBySemesterId(semesterId)
                .catch { e ->
                    android.util.Log.e("ScheduleVM", "加载课程失败", e)
                    _uiState.update { it.copy(courses = emptyList()) }
                }
                .collect { courses ->
                    _uiState.update { it.copy(courses = courses) }
                }
        }
    }

    fun selectSemester(semester: Semester) {
        _uiState.update { it.copy(selectedSemester = semester, currentWeek = 1, totalWeeks = semester.totalWeeks) }
        loadCoursesForSemester(semester.id)
    }

    fun setWeek(week: Int) {
        val clamped = week.coerceIn(1, _uiState.value.totalWeeks)
        _uiState.update { it.copy(currentWeek = clamped) }
    }

    fun previousWeek() {
        val current = _uiState.value.currentWeek
        if (current > 1) setWeek(current - 1)
    }

    fun nextWeek() {
        val current = _uiState.value.currentWeek
        if (current < _uiState.value.totalWeeks) setWeek(current + 1)
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, selectedCourse = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, selectedCourse = null) }
    }

    fun showDetailDialog(course: Course) {
        _uiState.update { it.copy(showDetailDialog = true, selectedCourse = course) }
    }

    fun hideDetailDialog() {
        _uiState.update { it.copy(showDetailDialog = false, selectedCourse = null) }
    }

    fun showEditDialog(course: Course) {
        _uiState.update { it.copy(showDetailDialog = false, showEditDialog = true, selectedCourse = course) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, selectedCourse = null) }
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            val semesterId = _uiState.value.selectedSemester?.id ?: return@launch
            val courseWithSemester = course.copy(semesterId = semesterId)
            try {
                if (course.id == 0L) {
                    repository.courseRepository.insertCourse(courseWithSemester)
                } else {
                    repository.courseRepository.updateCourse(courseWithSemester)
                }
                _uiState.update { it.copy(showAddDialog = false, showEditDialog = false, selectedCourse = null) }
            } catch (e: Exception) {
                android.util.Log.e("ScheduleVM", "保存课程失败", e)
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            try {
                repository.courseRepository.deleteCourse(course)
                _uiState.update { it.copy(showDetailDialog = false, selectedCourse = null) }
            } catch (e: Exception) {
                android.util.Log.e("ScheduleVM", "删除课程失败", e)
            }
        }
    }

    fun showTimeConfig() {
        _uiState.update { it.copy(showTimeConfig = true) }
    }

    fun hideTimeConfig() {
        _uiState.update { it.copy(showTimeConfig = false) }
    }

    fun showOcrImport() {
        _uiState.update { it.copy(showOcrImport = true) }
    }

    fun hideOcrImport() {
        _uiState.update { it.copy(showOcrImport = false) }
    }

    fun importCourses(courses: List<Course>) {
        viewModelScope.launch {
            val currentSemester = _uiState.value.selectedSemester
                ?: _uiState.value.semesters.firstOrNull()
            if (currentSemester == null) {
                android.util.Log.e("ScheduleVM", "导入失败：没有可用学期")
                _uiState.update { it.copy(showOcrImport = false) }
                return@launch
            }
            val semesterId = currentSemester.id
            try {
                val validCourses = courses.filter { it.name.isNotBlank() }
                if (validCourses.isEmpty()) {
                    _uiState.update { it.copy(showOcrImport = false) }
                    return@launch
                }
                val coursesWithSemester = validCourses.map { it.copy(semesterId = semesterId) }
                repository.courseRepository.insertCourses(coursesWithSemester)
                _uiState.update { it.copy(showOcrImport = false) }
            } catch (e: Exception) {
                android.util.Log.e("ScheduleVM", "导入课程失败", e)
                _uiState.update { it.copy(showOcrImport = false) }
            }
        }
    }

    fun getCoursesForCell(weekday: Int, section: Int): List<Course> {
        val week = _uiState.value.currentWeek
        return _uiState.value.courses.filter { course ->
            course.weekday == weekday &&
            section in course.startSection..course.endSection &&
            week in course.startWeek..course.endWeek &&
            (course.weekType == 0 ||
             (course.weekType == 1 && week % 2 == 1) ||
             (course.weekType == 2 && week % 2 == 0))
        }
    }

    fun isCourseStartCell(weekday: Int, section: Int, course: Course): Boolean {
        return course.weekday == weekday && course.startSection == section
    }
}
