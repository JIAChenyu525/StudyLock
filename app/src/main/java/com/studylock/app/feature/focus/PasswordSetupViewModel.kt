package com.studylock.app.feature.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studylock.app.StudyLockApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PasswordSetupUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isExistingPassword: Boolean = false,
    val currentPassword: String = "",
    val step: PasswordSetupStep = PasswordSetupStep.INPUT,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

enum class PasswordSetupStep {
    INPUT,
    CONFIRM,
    SUCCESS
}

class PasswordSetupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = StudyLockApp.repository
    
    private val _uiState = MutableStateFlow(PasswordSetupUiState())
    val uiState: StateFlow<PasswordSetupUiState> = _uiState.asStateFlow()
    
    init {
        checkExistingPassword()
    }
    
    private fun checkExistingPassword() {
        viewModelScope.launch {
            val isSet = repository.userSettingsRepository.isPasswordSet()
            _uiState.value = _uiState.value.copy(isExistingPassword = isSet)
        }
    }
    
    fun onPasswordChange(password: String) {
        val strength = PasswordUtils.calculateStrength(password)
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordStrength = strength,
            errorMessage = null
        )
    }
    
    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            errorMessage = null
        )
    }
    
    fun onCurrentPasswordChange(currentPassword: String) {
        _uiState.value = _uiState.value.copy(
            currentPassword = currentPassword,
            errorMessage = null
        )
    }
    
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }
    
    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible
        )
    }
    
    fun goToConfirmStep() {
        val password = _uiState.value.password
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "密码长度至少6位"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            step = PasswordSetupStep.CONFIRM,
            errorMessage = null
        )
    }
    
    fun goBackToInputStep() {
        _uiState.value = _uiState.value.copy(
            step = PasswordSetupStep.INPUT,
            confirmPassword = "",
            errorMessage = null
        )
    }
    
    fun savePassword() {
        val state = _uiState.value
        
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(
                errorMessage = "两次输入的密码不一致"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                if (state.isExistingPassword && state.currentPassword.isNotEmpty()) {
                    val isCurrentCorrect = repository.userSettingsRepository
                        .verifyPassword(state.currentPassword)
                    if (!isCurrentCorrect) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "当前密码验证失败"
                        )
                        return@launch
                    }
                }
                
                repository.userSettingsRepository.setPassword(state.password)
                
                _uiState.value = _uiState.value.copy(
                    step = PasswordSetupStep.SUCCESS,
                    isSuccess = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存密码失败: ${e.message}"
                )
            }
        }
    }
    
    fun removePassword() {
        viewModelScope.launch {
            try {
                repository.userSettingsRepository.removePassword()
                _uiState.value = PasswordSetupUiState(isExistingPassword = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "移除密码失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSetup() {
        _uiState.value = PasswordSetupUiState(isExistingPassword = _uiState.value.isExistingPassword)
        checkExistingPassword()
    }
}
