package com.studylock.app.feature.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studylock.app.StudyLockApp
import com.studylock.app.data.entity.ClassTimeConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClassTimeConfigUiState(
    val configs: List<ClassTimeConfig> = emptyList(),
    val editingConfig: ClassTimeConfig? = null,
    val showTimePicker: Boolean = false,
    val editingField: EditingField = EditingField.START,
    val showResetConfirm: Boolean = false
)

enum class EditingField { START, END }

class ClassTimeConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyLockApp.repository

    private val _uiState = MutableStateFlow(ClassTimeConfigUiState())
    val uiState: StateFlow<ClassTimeConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            repository.classTimeConfigRepository.getAllConfigs().collect { configs ->
                _uiState.update { it.copy(configs = configs) }
            }
        }
    }

    fun startEditing(config: ClassTimeConfig, field: EditingField) {
        _uiState.update {
            it.copy(editingConfig = config, editingField = field, showTimePicker = true)
        }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val config = _uiState.value.editingConfig ?: return
        val timeStr = String.format("%02d:%02d", hour, minute)
        val updated = when (_uiState.value.editingField) {
            EditingField.START -> config.copy(startTime = timeStr)
            EditingField.END -> config.copy(endTime = timeStr)
        }
        viewModelScope.launch {
            repository.classTimeConfigRepository.updateConfig(updated)
        }
        _uiState.update { it.copy(showTimePicker = false, editingConfig = null) }
    }

    fun dismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false, editingConfig = null) }
    }

    fun showResetConfirm() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun hideResetConfirm() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val defaults = listOf(
                ClassTimeConfig(sectionNo = 1, startTime = "08:00", endTime = "08:45"),
                ClassTimeConfig(sectionNo = 2, startTime = "08:50", endTime = "09:35"),
                ClassTimeConfig(sectionNo = 3, startTime = "09:50", endTime = "10:35"),
                ClassTimeConfig(sectionNo = 4, startTime = "10:40", endTime = "11:25"),
                ClassTimeConfig(sectionNo = 5, startTime = "11:30", endTime = "12:15"),
                ClassTimeConfig(sectionNo = 6, startTime = "13:30", endTime = "14:15"),
                ClassTimeConfig(sectionNo = 7, startTime = "14:20", endTime = "15:05"),
                ClassTimeConfig(sectionNo = 8, startTime = "15:20", endTime = "16:05"),
                ClassTimeConfig(sectionNo = 9, startTime = "16:10", endTime = "16:55"),
                ClassTimeConfig(sectionNo = 10, startTime = "17:00", endTime = "17:45"),
                ClassTimeConfig(sectionNo = 11, startTime = "19:00", endTime = "19:45"),
                ClassTimeConfig(sectionNo = 12, startTime = "19:50", endTime = "20:35")
            )
            repository.classTimeConfigRepository.deleteAllConfigs()
            repository.classTimeConfigRepository.insertConfigs(defaults)
            _uiState.update { it.copy(showResetConfirm = false) }
        }
    }
}
