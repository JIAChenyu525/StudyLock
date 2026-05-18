package com.studylock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "系统启动完成，启动专注守护服务")
                // 开机后自动启动专注守护服务
                FocusGuardService.startService(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "应用更新完成，重启专注守护服务")
                // 应用更新后重启服务
                FocusGuardService.startService(context)
            }
        }
    }
}

class FocusGuardReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "FocusGuardReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到定时检测广播")
        
        // 启动服务进行状态检测
        FocusGuardService.startService(context)
    }
}

class FocusGuardWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : androidx.work.Worker(context, params) {

    companion object {
        private const val TAG = "FocusGuardWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "WorkManager 唤醒，启动专注守护服务")

        try {
            // 启动服务进行状态检测
            val intent = Intent(applicationContext, FocusGuardService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager 执行失败", e)
            return Result.retry()
        }
    }
}
