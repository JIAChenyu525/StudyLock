package com.studylock.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.studylock.app.StudyLockApp
import com.studylock.app.R
import com.studylock.app.MainActivity
import com.studylock.app.data.entity.FocusRecord
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class FocusGuardService : LifecycleService() {

    companion object {
        const val TAG = "FocusGuardService"
        private const val NOTIFICATION_CHANNEL_ID = "focus_guard_channel"
        const val NOTIFICATION_ID = 1
        const val MAX_EMERGENCY_UNLOCKS = 3
        const val EMERGENCY_UNLOCK_DURATION_MS = 5 * 60 * 1000L

        @Volatile var isGuardActive = false
        @Volatile var isManualDeepWorkActive = false
        @Volatile var manualDeepWorkEndTimeMillis = 0L
        @Volatile var remainingMinutes = 0
        @Volatile var emergencyUnlockActive = false
        @Volatile var emergencyUnlockEndTimeMillis = 0L

        fun startService(context: Context) {
            try {
                val intent = Intent(context, FocusGuardService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动失败（可能APP在后台）", e)
            }
        }

        fun stopService(context: Context) {
            try {
                context.stopService(Intent(context, FocusGuardService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "停止失败", e)
            }
        }

        fun startManualDeepWork(context: Context, durationMinutes: Int) {
            isManualDeepWorkActive = true
            manualDeepWorkEndTimeMillis = System.currentTimeMillis() + durationMinutes * 60 * 1000L
            isGuardActive = true
            remainingMinutes = durationMinutes
            startService(context)
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    StudyLockApp.repository.focusRecordRepository.insertRecord(
                        FocusRecord(date = today, courseId = null, durationSec = durationMinutes * 60, unlockCount = 0)
                    )
                } catch (_: Exception) {}
            }
        }

        fun stopManualDeepWork(context: Context) {
            isManualDeepWorkActive = false
            manualDeepWorkEndTimeMillis = 0L
            isGuardActive = false
            stopService(context)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundCompat(NOTIFICATION_ID, createNotification("专注守护", "守护服务启动中…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { updateNotificationFromState() }
        return START_STICKY
    }

    private suspend fun updateNotificationFromState() {
        if (isManualDeepWorkActive && System.currentTimeMillis() < manualDeepWorkEndTimeMillis) {
            remainingMinutes = ((manualDeepWorkEndTimeMillis - System.currentTimeMillis()) / 60000).toInt()
            updateNotification(
                "专注守护中🛡 深度工作 还剩${remainingMinutes}分钟",
                "正在守护您的专注时间"
            )
            if (remainingMinutes <= 0) {
                isManualDeepWorkActive = false
                manualDeepWorkEndTimeMillis = 0L
                isGuardActive = false
                sendEndNotification("深度工作")
                stopSelf()
            }
        } else if (isManualDeepWorkActive) {
            isManualDeepWorkActive = false
            isGuardActive = false
            stopSelf()
        } else {
            isGuardActive = false
            updateNotification("专注守护", "守护服务待机中")
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        Handler(Looper.getMainLooper()).post {
            startForegroundCompat(NOTIFICATION_ID, notification)
        }
    }

    private fun sendEndNotification(label: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("$label 结束")
            .setContentText("$label 时间结束，你做到了！")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        )
    }

    private fun createNotification(title: String, content: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title).setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher).setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).setContentIntent(pi).build()
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(NOTIFICATION_CHANNEL_ID, "专注守护", NotificationManager.IMPORTANCE_LOW).apply {
                description = "专注守护服务通知"; setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isGuardActive = false
        isManualDeepWorkActive = false
        manualDeepWorkEndTimeMillis = 0L
        emergencyUnlockActive = false
        emergencyUnlockEndTimeMillis = 0L
        scope.cancel()
        Handler(Looper.getMainLooper()).post { stopForeground(true) }
    }
}
