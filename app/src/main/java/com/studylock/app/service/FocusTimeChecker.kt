package com.studylock.app.service

import com.studylock.app.data.entity.ClassTimeConfig
import com.studylock.app.data.entity.Course
import com.studylock.app.data.repository.StudyLockRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

data class FocusStatus(
    val isInClass: Boolean,
    val currentCourse: Course?,
    val remainingSeconds: Int,
    val nextCourse: Course?
)

class FocusTimeChecker(private val repository: StudyLockRepository) {
    
    companion object {
        private const val TAG = "FocusTimeChecker"
        
        // 获取当前时间信息
        private fun getCurrentTimeInfo(): TimeInfo {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 周日=1, 周一=2, ..., 周六=7
            
            // 转换为系统内部使用的星期（周一=1, 周日=7）
            val weekday = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 0
            }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = timeFormat.format(calendar.time)
            
            return TimeInfo(
                weekday = weekday,
                currentTime = currentTime,
                calendar = calendar
            )
        }
        
        private suspend fun getCurrentWeek(repository: StudyLockRepository): Int {
            try {
                val semesters = repository.semesterRepository.getAllSemesters().first()
                val currentSemester = semesters.firstOrNull() ?: return 1
                val startDateStr = currentSemester.startDate
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = sdf.parse(startDateStr) ?: return 1
                val calendar = Calendar.getInstance()
                val diffMs = calendar.timeInMillis - startDate.time
                val diffDays = diffMs / (24 * 60 * 60 * 1000)
                val week = (diffDays / 7 + 1).toInt()
                return week.coerceIn(1, currentSemester.totalWeeks)
            } catch (_: Exception) {
                return 1
            }
        }
    }
    
    suspend fun checkFocusStatus(): FocusStatus {
        val timeInfo = getCurrentTimeInfo()
        val currentWeek = getCurrentWeek(repository)
        
        // 获取所有课程和节次配置
        val semesterId = repository.semesterRepository.getAllSemesters().first().firstOrNull()?.id ?: -1L
        val courses = if (semesterId != -1L) {
            repository.courseRepository.getCoursesBySemesterId(semesterId).first()
        } else {
            emptyList()
        }
        val classTimeConfigs = repository.classTimeConfigRepository.getAllConfigs().first()
        
        // 查找当前正在进行的课程
        val currentCourse = findCurrentCourse(courses, classTimeConfigs, timeInfo, currentWeek)
        
        return if (currentCourse != null) {
            val remainingSeconds = calculateRemainingSeconds(currentCourse, classTimeConfigs, timeInfo)
            val nextCourse = findNextCourse(courses, classTimeConfigs, timeInfo, currentWeek)
            
            FocusStatus(
                isInClass = true,
                currentCourse = currentCourse,
                remainingSeconds = remainingSeconds,
                nextCourse = nextCourse
            )
        } else {
            val nextCourse = findNextCourse(courses, classTimeConfigs, timeInfo, currentWeek)
            
            FocusStatus(
                isInClass = false,
                currentCourse = null,
                remainingSeconds = 0,
                nextCourse = nextCourse
            )
        }
    }
    
    private fun findCurrentCourse(
        courses: List<Course>,
        classTimeConfigs: List<ClassTimeConfig>,
        timeInfo: TimeInfo,
        currentWeek: Int
    ): Course? {
        return courses.find { course ->
            // 检查星期是否匹配
            course.weekday == timeInfo.weekday &&
            // 检查周次是否匹配
            currentWeek in course.startWeek..course.endWeek &&
            // 检查单双周
            checkWeekType(course.weekType, currentWeek) &&
            // 检查当前时间是否在课程节次内
            isTimeInCourseSection(course, classTimeConfigs, timeInfo.currentTime)
        }
    }
    
    private fun checkWeekType(weekType: Int, currentWeek: Int): Boolean {
        return when (weekType) {
            0 -> true // 全部周
            1 -> currentWeek % 2 == 1 // 单周
            2 -> currentWeek % 2 == 0 // 双周
            else -> true
        }
    }
    
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return -1
        val h = parts[0].toIntOrNull() ?: return -1
        val m = parts[1].toIntOrNull() ?: return -1
        return h * 60 + m
    }

    private fun isTimeInCourseSection(
        course: Course,
        classTimeConfigs: List<ClassTimeConfig>,
        currentTime: String
    ): Boolean {
        val startConfig = classTimeConfigs.find { it.sectionNo == course.startSection }
        val endConfig = classTimeConfigs.find { it.sectionNo == course.endSection }
        if (startConfig == null || endConfig == null) return false

        val nowMin = timeToMinutes(currentTime)
        val startMin = timeToMinutes(startConfig.startTime)
        val endMin = timeToMinutes(endConfig.endTime)
        if (nowMin < 0 || startMin < 0 || endMin < 0) return false

        return nowMin in startMin..endMin
    }
    
    private fun calculateRemainingSeconds(
        course: Course,
        classTimeConfigs: List<ClassTimeConfig>,
        timeInfo: TimeInfo
    ): Int {
        val endConfig = classTimeConfigs.find { it.sectionNo == course.endSection }
        if (endConfig == null) return 0
        
        // 解析结束时间
        val endParts = endConfig.endTime.split(":")
        if (endParts.size != 2) return 0
        
        val endHour = endParts[0].toIntOrNull() ?: return 0
        val endMinute = endParts[1].toIntOrNull() ?: return 0
        
        // 设置结束时间
        val endCalendar = timeInfo.calendar.clone() as Calendar
        endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
        endCalendar.set(Calendar.MINUTE, endMinute)
        endCalendar.set(Calendar.SECOND, 0)
        
        // 计算剩余秒数
        val currentMillis = timeInfo.calendar.timeInMillis
        val endMillis = endCalendar.timeInMillis
        
        return if (endMillis > currentMillis) {
            ((endMillis - currentMillis) / 1000).toInt()
        } else {
            0
        }
    }
    
    private fun findNextCourse(
        courses: List<Course>,
        classTimeConfigs: List<ClassTimeConfig>,
        timeInfo: TimeInfo,
        currentWeek: Int
    ): Course? {
        val nowMin = timeToMinutes(timeInfo.currentTime)
        return courses
            .filter { course ->
                course.weekday == timeInfo.weekday &&
                currentWeek in course.startWeek..course.endWeek &&
                checkWeekType(course.weekType, currentWeek)
            }
            .filter { course ->
                val startConfig = classTimeConfigs.find { it.sectionNo == course.startSection }
                val startMin = startConfig?.let { timeToMinutes(it.startTime) } ?: Int.MAX_VALUE
                startMin > nowMin
            }
            .minByOrNull { course ->
                val startConfig = classTimeConfigs.find { it.sectionNo == course.startSection }
                startConfig?.let { timeToMinutes(it.startTime) } ?: Int.MAX_VALUE
            }
    }
}

private data class TimeInfo(
    val weekday: Int,
    val currentTime: String,
    val calendar: Calendar
)
