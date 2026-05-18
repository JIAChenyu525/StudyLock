package com.studylock.app.data.dao

import androidx.room.*
import com.studylock.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: UserSettings)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<UserSettings>)
    
    @Update
    suspend fun update(setting: UserSettings)
    
    @Delete
    suspend fun delete(setting: UserSettings)
    
    @Query("DELETE FROM user_settings WHERE key = :key")
    suspend fun deleteByKey(key: String)
    
    @Query("SELECT * FROM user_settings WHERE key = :key")
    suspend fun getByKey(key: String): UserSettings?
    
    @Query("SELECT value FROM user_settings WHERE key = :key")
    suspend fun getValueByKey(key: String): String?
    
    @Query("SELECT * FROM user_settings ORDER BY key")
    fun getAll(): Flow<List<UserSettings>>
    
    @Query("SELECT COUNT(*) FROM user_settings")
    suspend fun getCount(): Int
    
    @Query("SELECT * FROM user_settings WHERE key LIKE '%' || :keyword || '%' ORDER BY key")
    fun search(keyword: String): Flow<List<UserSettings>>
}