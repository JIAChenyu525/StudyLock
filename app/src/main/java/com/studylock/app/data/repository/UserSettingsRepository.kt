package com.studylock.app.data.repository

import com.studylock.app.data.dao.UserSettingsDao
import com.studylock.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) : BaseRepository() {
    
    suspend fun insertSetting(setting: UserSettings) {
        userSettingsDao.insert(setting)
    }
    
    suspend fun updateSetting(setting: UserSettings) {
        userSettingsDao.update(setting)
    }
    
    suspend fun deleteSetting(setting: UserSettings) {
        userSettingsDao.delete(setting)
    }
    
    suspend fun deleteSettingByKey(key: String) {
        userSettingsDao.deleteByKey(key)
    }
    
    suspend fun getSettingByKey(key: String): UserSettings? {
        return userSettingsDao.getByKey(key)
    }
    
    suspend fun getValueByKey(key: String): String? {
        return userSettingsDao.getValueByKey(key)
    }
    
    fun getAllSettings(): Flow<List<UserSettings>> {
        return userSettingsDao.getAll()
    }
    
    suspend fun getSettingsCount(): Int {
        return userSettingsDao.getCount()
    }
    
    fun searchSettings(keyword: String): Flow<List<UserSettings>> {
        return userSettingsDao.search(keyword)
    }
    
    suspend fun insertSettings(settings: List<UserSettings>) {
        userSettingsDao.insertAll(settings)
    }
    
    // 密码管理相关方法
    suspend fun setPassword(password: String) {
        val hashedPassword = hashPassword(password)
        val setting = UserSettings(key = "password_hash", value = hashedPassword)
        userSettingsDao.insert(setting)
    }
    
    suspend fun verifyPassword(password: String): Boolean {
        val hashedPassword = userSettingsDao.getValueByKey("password_hash")
        return if (hashedPassword != null) {
            checkPassword(password, hashedPassword)
        } else {
            false
        }
    }
    
    suspend fun isPasswordSet(): Boolean {
        return userSettingsDao.getValueByKey("password_hash") != null
    }
    
    suspend fun removePassword() {
        userSettingsDao.deleteByKey("password_hash")
    }
    
    // 常用设置操作方法
    suspend fun getFocusDuration(): Int {
        return getValueByKey("focus_duration")?.toIntOrNull() ?: 1800
    }
    
    suspend fun setFocusDuration(durationSeconds: Int) {
        val setting = UserSettings(key = "focus_duration", value = durationSeconds.toString())
        userSettingsDao.insert(setting)
    }
    
    suspend fun getBreakDuration(): Int {
        return getValueByKey("break_duration")?.toIntOrNull() ?: 300
    }
    
    suspend fun setBreakDuration(durationSeconds: Int) {
        val setting = UserSettings(key = "break_duration", value = durationSeconds.toString())
        userSettingsDao.insert(setting)
    }
    
    suspend fun isAutoStartFocusEnabled(): Boolean {
        return getValueByKey("auto_start_focus")?.toBoolean() ?: false
    }
    
    suspend fun setAutoStartFocusEnabled(enabled: Boolean) {
        val setting = UserSettings(key = "auto_start_focus", value = enabled.toString())
        userSettingsDao.insert(setting)
    }
    
    suspend fun isNotificationEnabled(): Boolean {
        return getValueByKey("notification_enabled")?.toBoolean() ?: true
    }
    
    suspend fun setNotificationEnabled(enabled: Boolean) {
        val setting = UserSettings(key = "notification_enabled", value = enabled.toString())
        userSettingsDao.insert(setting)
    }
}