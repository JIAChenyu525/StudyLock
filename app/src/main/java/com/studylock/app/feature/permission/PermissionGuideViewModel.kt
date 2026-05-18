package com.studylock.app.feature.permission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studylock.app.StudyLockApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionGuideUiState(
    val permissionStates: PermissionStates = PermissionStates(),
    val isGuideCompleted: Boolean = false,
    val showManufacturerDialog: Boolean = false,
    val manufacturerInfo: ManufacturerGuideInfo? = null
)

class PermissionGuideViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyLockApp.repository

    private val _uiState = MutableStateFlow(PermissionGuideUiState())
    val uiState: StateFlow<PermissionGuideUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
        checkGuideStatus()
        detectManufacturer()
    }

    private fun checkPermissions() {
        val context = getApplication<Application>()
        val states = PermissionUtils.getAllPermissionStates(context)

        _uiState.value = _uiState.value.copy(
            permissionStates = states
        )
    }

    private fun checkGuideStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val done = repository.userSettingsRepository.getValueByKey("permission_guide_done")
                _uiState.value = _uiState.value.copy(isGuideCompleted = done == "true")
            } catch (_: Exception) {
            }
        }
    }

    private fun detectManufacturer() {
        val context = getApplication<Application>()
        val info = ManufacturerUtils.getGuideInfo(context)
        _uiState.value = _uiState.value.copy(manufacturerInfo = info)
    }

    fun refreshPermissions() {
        checkPermissions()
    }

    fun showManufacturerGuide() {
        _uiState.value = _uiState.value.copy(showManufacturerDialog = true)
    }

    fun hideManufacturerGuide() {
        _uiState.value = _uiState.value.copy(showManufacturerDialog = false)
    }

    fun completeGuide() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.userSettingsRepository.insertSetting(
                    com.studylock.app.data.entity.UserSettings(
                        key = "permission_guide_done",
                        value = "true"
                    )
                )
                _uiState.value = _uiState.value.copy(isGuideCompleted = true)
            } catch (_: Exception) {
            }
        }
    }

    fun isGuideNeeded(): Boolean {
        return !_uiState.value.isGuideCompleted
    }
}