package com.studylock.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studylock.app.data.dao.*
import com.studylock.app.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        Semester::class,
        Course::class,
        ClassTimeConfig::class,
        FocusRecord::class,
        UserSettings::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyLockDatabase : RoomDatabase() {

    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun classTimeConfigDao(): ClassTimeConfigDao
    abstract fun focusRecordDao(): FocusRecordDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: StudyLockDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS blacklist_apps")
                db.execSQL("DROP TABLE IF EXISTS whitelist_apps")
            }
        }

        fun getDatabase(context: Context): StudyLockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        StudyLockDatabase::class.java,
                        "studylock_database"
                    )
                        .addMigrations(MIGRATION_1_2)
                        .addCallback(DatabaseCallback(context.applicationContext))
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        populateInitialData(database)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: StudyLockDatabase) {
            val defaultTimeConfigs = listOf(
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

            database.classTimeConfigDao().insertAll(defaultTimeConfigs)

            val currentSemester = Semester(
                name = "2023-2024学年第二学期",
                startDate = "2024-02-26",
                totalWeeks = 20
            )
            database.semesterDao().insert(currentSemester)

            val defaultSettings = listOf(
                UserSettings(key = "focus_duration", value = "1800"),
                UserSettings(key = "break_duration", value = "300"),
                UserSettings(key = "auto_start_focus", value = "false"),
                UserSettings(key = "notification_enabled", value = "true")
            )

            database.userSettingsDao().insertAll(defaultSettings)
        }
    }
}
