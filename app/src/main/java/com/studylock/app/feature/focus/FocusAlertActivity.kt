package com.studylock.app.feature.focus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studylock.app.service.FocusAccessibilityService
import com.studylock.app.ui.theme.StudyLockTheme

class FocusAlertActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "FocusAlertActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
        }
        
        setContent {
            StudyLockTheme {
                FocusAlertScreen(
                    packageName = intent.getStringExtra("packageName") ?: "",
                    courseName = intent.getStringExtra("courseName") ?: "当前课程",
                    remainingSeconds = intent.getIntExtra("remainingSeconds", 0),
                    onClose = { finish() },
                    onSettings = { openAccessibilitySettings() }
                )
            }
        }
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开无障碍设置，尝试打开应用详情页
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                // 最后尝试打开系统设置
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusAlertScreen(
    packageName: String,
    courseName: String,
    remainingSeconds: Int,
    onClose: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val remainingMinutes = remainingSeconds / 60
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 警告图标
            Text(
                text = "🚫",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // 标题
            Text(
                text = "专注时间",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 课程信息
            Text(
                text = courseName,
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 剩余时间
            Text(
                text = "还剩 ${remainingMinutes} 分钟",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // 提示信息
            Text(
                text = "上课时间请保持专注，避免使用娱乐应用",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // 按钮区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 设置按钮
                FilledTonalButton(
                    onClick = onSettings,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("无障碍设置", color = Color.White)
                }
                
                // 关闭按钮
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("返回专注", color = Color.White)
                }
            }
        }
        
        // 顶部状态栏（显示当前时间等）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "StudyLock 专注守护",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            
            Text(
                text = "${remainingMinutes}min",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}
