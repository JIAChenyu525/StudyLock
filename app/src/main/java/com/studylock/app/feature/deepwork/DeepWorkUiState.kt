package com.studylock.app.feature.deepwork

data class TimelinePeriod(
    val courseName: String,
    val startTime: String,
    val endTime: String,
    val sectionRange: String,
    val isActive: Boolean,
    val isClass: Boolean,
    val colorIndex: Int = 0
)

data class DeepWorkWindow(
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val label: String
)

data class DeepWorkUiState(
    val isGuardActive: Boolean = false,
    val isManualModeActive: Boolean = false,
    val manualModeRemainingMinutes: Int = 0,
    val selectedDuration: Int = 60,

    val consecutiveDays: Int = 0,
    val todayHasSession: Boolean = false,

    val todayClassPeriods: List<TimelinePeriod> = emptyList(),
    val suggestedDeepWorkWindows: List<DeepWorkWindow> = emptyList(),

    val emergencyUnlocksUsedToday: Int = 0,
    val emergencyUnlocksRemaining: Int = 3,

    val isAccessibilityEnabled: Boolean = false,
    val isUsageStatsGranted: Boolean = false,

    val isLoading: Boolean = false,
    val weeklyTotalMinutes: Long = 0,
    val errorMessage: String? = null
)
