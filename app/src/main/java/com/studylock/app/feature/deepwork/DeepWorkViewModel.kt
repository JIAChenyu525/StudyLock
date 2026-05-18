package com.studylock.app.feature.deepwork

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studylock.app.StudyLockApp
import com.studylock.app.data.entity.ClassTimeConfig
import com.studylock.app.service.FocusAccessibilityService
import com.studylock.app.service.FocusGuardService
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DeepWorkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyLockApp.repository

    private val _uiState = MutableStateFlow(DeepWorkUiState())
    val uiState: StateFlow<DeepWorkUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    loadGuardStatus()
                    calculateConsecutiveDays()
                    buildTodayTimeline()
                    loadEmergencyUnlockCount()
                    checkPermissions()
                    loadWeeklyStats()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "数据加载失败: ${e.message}", isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadGuardStatus() {
        val isActive = FocusGuardService.isGuardActive
        val isManualActive = FocusGuardService.isManualDeepWorkActive
        val remainingMin = if (isManualActive) {
            ((FocusGuardService.manualDeepWorkEndTimeMillis - System.currentTimeMillis()) / 60000).toInt()
        } else {
            0
        }
        _uiState.update {
            it.copy(
                isGuardActive = isActive,
                isManualModeActive = isManualActive,
                manualModeRemainingMinutes = remainingMin.coerceAtLeast(0)
            )
        }
    }

    private suspend fun calculateConsecutiveDays() {
        try {
        val dates = repository.focusRecordRepository.getRecentDates(365)
        if (dates.isEmpty()) {
            _uiState.update { it.copy(consecutiveDays = 0, todayHasSession = false) }
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val today = sdf.format(cal.time)

        var count = 0
        val hasToday = dates.contains(today)
        if (hasToday) count = 1

        cal.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            val checkDate = sdf.format(cal.time)
            if (dates.contains(checkDate)) {
                count++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        _uiState.update {
            it.copy(consecutiveDays = count, todayHasSession = hasToday)
        }
        } catch (_: Exception) {
            _uiState.update { it.copy(consecutiveDays = 0, todayHasSession = false) }
        }
    }

    private suspend fun buildTodayTimeline() {
        try {
        val calendar = Calendar.getInstance()
        val weekday = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        val semesters = repository.semesterRepository.getAllSemestersOnce()
        val activeSemester = semesters.firstOrNull() ?: run {
            _uiState.update { it.copy(todayClassPeriods = emptyList(), suggestedDeepWorkWindows = emptyList()) }
            return
        }

        val allCourses = repository.courseRepository.getCoursesBySemesterIdOnce(activeSemester.id)
        val allConfigs = repository.classTimeConfigRepository.getAllConfigsOnce()

        val timeConfigs = allConfigs.associateBy { it.sectionNo }

        val startDate = parseDate(activeSemester.startDate) ?: return
        val diffMs = calendar.timeInMillis - startDate.time
        val currentWeek = ((diffMs / (7 * 24 * 60 * 60 * 1000)) + 1).toInt().coerceIn(1, activeSemester.totalWeeks)

        val todayCourses = allCourses.filter { course ->
            course.weekday == weekday &&
            currentWeek in course.startWeek..course.endWeek &&
            (course.weekType == 0 ||
             (course.weekType == 1 && currentWeek % 2 == 1) ||
             (course.weekType == 2 && currentWeek % 2 == 0))
        }.sortedBy { it.startSection }

        val nowTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

        val periods = mutableListOf<TimelinePeriod>()
        for (course in todayCourses) {
            val startCfg = timeConfigs[course.startSection] ?: continue
            val endCfg = timeConfigs[course.endSection] ?: continue
            val active = nowTime in startCfg.startTime..endCfg.endTime
            periods.add(
                TimelinePeriod(
                    courseName = course.name,
                    startTime = startCfg.startTime,
                    endTime = endCfg.endTime,
                    sectionRange = "第${course.startSection}-${course.endSection}节",
                    isActive = active,
                    isClass = true,
                    colorIndex = course.colorTag.hashCode() % 10
                )
            )
        }

        val windows = findDeepWorkWindows(periods, todayCourses, timeConfigs)

        _uiState.update {
            it.copy(
                todayClassPeriods = periods,
                suggestedDeepWorkWindows = windows
            )
        }
        } catch (_: Exception) {
            _uiState.update { it.copy(todayClassPeriods = emptyList(), suggestedDeepWorkWindows = emptyList()) }
        }
    }

    private fun findDeepWorkWindows(
        periods: List<TimelinePeriod>,
        @Suppress("UNUSED_PARAMETER") courses: List<com.studylock.app.data.entity.Course>,
        configs: Map<Int, ClassTimeConfig>
    ): List<DeepWorkWindow> {
        val windows = mutableListOf<DeepWorkWindow>()

        val allSections = configs.values.sortedBy { it.sectionNo }
        if (allSections.isEmpty()) return windows

        val firstTime = allSections.first().startTime
        val lastTime = allSections.last().endTime

        if (periods.isEmpty()) {
            windows.add(DeepWorkWindow(firstTime, lastTime, estimateDuration(firstTime, lastTime), "全天可深度工作"))
            return windows
        }

        val occupiedTimes = periods.map { it.startTime to it.endTime }.sortedBy { it.first }

        val dayStart = firstTime
        val firstOccupied = occupiedTimes.first()
        val firstGap = estimateDuration(dayStart, firstOccupied.first)
        if (firstGap >= 30) {
            windows.add(DeepWorkWindow(dayStart, firstOccupied.first, firstGap, "课前深度工作"))
        }

        for (i in 0 until occupiedTimes.size - 1) {
            val gapStart = occupiedTimes[i].second
            val gapEnd = occupiedTimes[i + 1].first
            val gapMin = estimateDuration(gapStart, gapEnd)
            if (gapMin >= 30) {
                windows.add(DeepWorkWindow(gapStart, gapEnd, gapMin, "课间深度工作"))
            }
        }

        val lastOccupied = occupiedTimes.last()
        val lastGap = estimateDuration(lastOccupied.second, lastTime)
        if (lastGap >= 30) {
            windows.add(DeepWorkWindow(lastOccupied.second, lastTime, lastGap, "课后深度工作"))
        }

        return windows
    }

    private fun estimateDuration(startTime: String, endTime: String): Int {
        val startParts = startTime.split(":").map { it.toIntOrNull() ?: 0 }
        val endParts = endTime.split(":").map { it.toIntOrNull() ?: 0 }
        val startMin = startParts[0] * 60 + startParts[1]
        val endMin = endParts[0] * 60 + endParts[1]
        return if (endMin >= startMin) endMin - startMin else 0
    }

    private suspend fun loadEmergencyUnlockCount() {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val count = repository.focusRecordRepository.getRecordsByDateOnce(today)
                .sumOf { it.unlockCount }
            val max = FocusGuardService.MAX_EMERGENCY_UNLOCKS
            _uiState.update {
                it.copy(
                    emergencyUnlocksUsedToday = count,
                    emergencyUnlocksRemaining = (max - count).coerceAtLeast(0)
                )
            }
        } catch (_: Exception) {}
    }

    private fun checkPermissions() {
        val context = getApplication<Application>()
        val accEnabled = FocusAccessibilityService.isServiceRunning

        var usageGranted = false
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
            usageGranted = appOps?.let {
                val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    it.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } ?: false
        } catch (_: Exception) {}

        _uiState.update {
            it.copy(
                isAccessibilityEnabled = accEnabled,
                isUsageStatsGranted = usageGranted
            )
        }
    }

    private suspend fun loadWeeklyStats() {
        try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val endDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = sdf.format(cal.time)
            val total = repository.focusRecordRepository.getTotalDurationByDateRange(startDate, endDate)
            _uiState.update { it.copy(weeklyTotalMinutes = total / 60) }
        } catch (_: Exception) {}
    }

    fun startManualDeepWork(durationMinutes: Int) {
        val context = getApplication<Application>()
        try {
            FocusGuardService.startManualDeepWork(context, durationMinutes)
            _uiState.update {
                it.copy(isGuardActive = true, isManualModeActive = true, manualModeRemainingMinutes = durationMinutes)
            }
            val intent = android.content.Intent(context, com.studylock.app.feature.focus.FocusBlockActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(com.studylock.app.feature.focus.FocusBlockActivity.EXTRA_IS_MANUAL_MODE, true)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "开启深度工作失败: ${e.message}") }
            return
        }
        loadData()
    }

    fun stopManualDeepWork() {
        try {
            val context = getApplication<Application>()
            FocusGuardService.stopManualDeepWork(context)
            _uiState.update {
                it.copy(
                    isGuardActive = FocusGuardService.isGuardActive,
                    isManualModeActive = false,
                    manualModeRemainingMinutes = 0
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "停止失败: ${e.message}") }
            return
        }
        loadData()
    }

    fun toggleGuardService() {
        try {
            val context = getApplication<Application>()
            if (FocusGuardService.isGuardActive) {
                FocusGuardService.stopService(context)
            } else {
                FocusGuardService.startService(context)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "切换服务失败: ${e.message}") }
            return
        }
        viewModelScope.launch {
            delay(500)
            loadData()
        }
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }
}
