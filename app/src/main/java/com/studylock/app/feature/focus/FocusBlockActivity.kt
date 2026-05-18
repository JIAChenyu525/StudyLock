package com.studylock.app.feature.focus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.studylock.app.StudyLockApp
import com.studylock.app.data.entity.FocusRecord
import com.studylock.app.service.FocusGuardService
import com.studylock.app.ui.theme.StudyLockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FocusBlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IS_MANUAL_MODE = "is_manual_mode"
        private const val RELAUNCH_DELAY_MS = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isUnlocking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val isManual = intent.getBooleanExtra(EXTRA_IS_MANUAL_MODE, false)

        setContent {
            StudyLockTheme {
                LockScreen(
                    isManualMode = isManual,
                    onUnlock = { finish() },
                    onContinueFocus = { finish() },
                    onLaunchApp = { pkg ->
                        try {
                            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                            if (launchIntent != null) {
                                isUnlocking = true
                                startActivity(launchIntent)
                            }
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isUnlocking && !isFinishing && !isDestroyed) {
            handler.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    startActivity(Intent(this, FocusBlockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(EXTRA_IS_MANUAL_MODE, intent.getBooleanExtra(EXTRA_IS_MANUAL_MODE, false))
                    })
                }
            }, RELAUNCH_DELAY_MS)
        }
        isUnlocking = false
    }

    override fun onBackPressed() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

data class WhitelistApp(val name: String, val packageName: String, val emoji: String)

private val STUDY_WHITELIST = listOf(
    WhitelistApp("计算器", "com.android.calculator2", "🔢"),
    WhitelistApp("时钟", "com.android.deskclock", "⏰"),
    WhitelistApp("日历", "com.android.calendar", "📅"),
    WhitelistApp("词典", "com.pleco.chinesesystem", "📖"),
)

enum class LockMode { MAIN, PASSWORD, COOLDOWN, EMERGENCY }

@Composable
fun LockScreen(
    isManualMode: Boolean,
    onUnlock: () -> Unit,
    onContinueFocus: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    var mode by remember { mutableStateOf(LockMode.MAIN) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var quote by remember { mutableStateOf(MotivationalQuotes.getRandom()) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var errorAttempts by remember { mutableIntStateOf(0) }
    var lockoutRemaining by remember { mutableIntStateOf(0) }
    var cooldownRemaining by remember { mutableIntStateOf(60) }
    var emergencyCountdown by remember { mutableIntStateOf(0) }
    var displayedPassword by remember { mutableStateOf("") }
    var todayUnlockCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { StudyLockApp.repository }
    val focusRequester = remember { FocusRequester() }

    val maxEmergency = FocusGuardService.MAX_EMERGENCY_UNLOCKS
    val unlocksRemaining = (maxEmergency - todayUnlockCount).coerceAtLeast(0)

    LaunchedEffect(Unit) {
        remainingSeconds = if (isManualMode) {
            ((FocusGuardService.manualDeepWorkEndTimeMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        } else {
            ((FocusGuardService.manualDeepWorkEndTimeMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        }
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            todayUnlockCount = repository.focusRecordRepository.getRecordsByDateOnce(today).sumOf { it.unlockCount }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (remainingSeconds > 0) {
                remainingSeconds--
            } else if (isManualMode && remainingSeconds <= 0) {
                FocusGuardService.stopManualDeepWork(context)
                onUnlock()
            }
            if (lockoutRemaining > 0) {
                lockoutRemaining--
                if (lockoutRemaining <= 0) errorAttempts = 0
            }
            if (mode == LockMode.COOLDOWN && cooldownRemaining > 0) {
                cooldownRemaining--
                if (cooldownRemaining <= 0) {
                    doUnlock(repository, context, null, onUnlock)
                }
            }
            if (mode == LockMode.EMERGENCY && emergencyCountdown > 0) {
                emergencyCountdown--
                if (emergencyCountdown <= 0) doEmergencyUnlock(context, onUnlock)
            }
        }
    }

    LaunchedEffect(passwordInput) {
        if (passwordInput.isNotEmpty()) {
            delay(300)
            displayedPassword = passwordInput
        } else {
            displayedPassword = ""
        }
    }

    val title = if (isManualMode) "深度工作专注中" else "上课专注中"
    val minutes = remainingSeconds / 60
    val secs = remainingSeconds % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding().navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1).copy(alpha = 0.95f),
                        Color(0xFF1B5E20).copy(alpha = 0.95f),
                        Color(0xFF0D47A1).copy(alpha = 0.98f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "剩余 ${minutes}分 ${String.format("%02d", secs)}秒",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(40.dp))

            AnimatedVisibility(mode == LockMode.MAIN, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 36.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(quote, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, lineHeight = 24.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("学习工具", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        STUDY_WHITELIST.forEach { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onLaunchApp(app.packageName) }.padding(8.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(app.emoji, fontSize = 22.sp) }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(app.name, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onContinueFocus,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("我知道了，继续专注", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(Modifier.height(10.dp))

                    if (unlocksRemaining > 0) {
                        OutlinedButton(
                            onClick = { emergencyCountdown = 3; mode = LockMode.EMERGENCY },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0xFFFFB74D).copy(alpha = 0.7f), Color(0xFFFF8A65).copy(alpha = 0.7f)))),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFCC80))
                        ) {
                            Icon(Icons.Default.Warning, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("紧急解锁（剩${unlocksRemaining}次）", fontSize = 14.sp)
                        }
                    } else {
                        Text("今日紧急解锁次数已用完", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { mode = LockMode.PASSWORD; scope.launch { delay(100); focusRequester.requestFocus() } },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.3f)))),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("输入密码解锁", fontSize = 14.sp)
                    }
                }
            }

            AnimatedVisibility(mode == LockMode.PASSWORD, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                PasswordPanel(displayedPassword, passwordInput, passwordError, errorAttempts, lockoutRemaining, focusRequester,
                    { passwordInput = it; passwordError = false },
                    {
                        scope.launch(Dispatchers.IO) {
                            try {
                                if (repository.userSettingsRepository.verifyPassword(passwordInput)) {
                                    val cooldown = repository.userSettingsRepository.getValueByKey("cooldown_enabled")?.toBoolean() ?: false
                                    if (cooldown) { cooldownRemaining = 60; mode = LockMode.COOLDOWN }
                                    else doUnlock(repository, context, null, onUnlock)
                                } else {
                                    passwordError = true; errorAttempts++
                                    if (errorAttempts >= 3) lockoutRemaining = 600
                                    passwordInput = ""; displayedPassword = ""
                                }
                            } catch (_: Exception) { passwordError = true; passwordInput = ""; displayedPassword = "" }
                        }
                    },
                    { mode = LockMode.MAIN; passwordInput = ""; displayedPassword = ""; passwordError = false }
                )
            }

            AnimatedVisibility(mode == LockMode.COOLDOWN, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧘", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("冷静期", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("${cooldownRemaining}秒后解锁", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Spacer(Modifier.height(8.dp))
                            Text("想清楚是否真的需要解锁", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(progress = { (60 - cooldownRemaining) / 60f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF4CAF50), trackColor = Color.White.copy(alpha = 0.2f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = { mode = LockMode.MAIN; passwordInput = ""; displayedPassword = "" }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(22.dp)) { Text("取消，继续专注") }
                }
            }

            AnimatedVisibility(mode == LockMode.EMERGENCY, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF8A65).copy(alpha = 0.2f))) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("紧急解锁", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("${emergencyCountdown}秒后解锁", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                            Spacer(Modifier.height(8.dp))
                            Text("解锁后获得5分钟使用时间", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = { mode = LockMode.MAIN }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(22.dp)) { Text("取消，继续专注") }
                }
            }
        }
    }
}

private fun doUnlock(repo: com.studylock.app.data.repository.StudyLockRepository, ctx: android.content.Context, courseId: Long?, onFinish: () -> Unit) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repo.focusRecordRepository.insertRecord(FocusRecord(date = today, courseId = courseId, durationSec = 0, unlockCount = 1))
        } catch (_: Exception) {}
    }
    FocusGuardService.stopService(ctx)
    onFinish()
}

private fun doEmergencyUnlock(ctx: android.content.Context, onFinish: () -> Unit) {
    FocusGuardService.emergencyUnlockActive = true
    FocusGuardService.emergencyUnlockEndTimeMillis = System.currentTimeMillis() + FocusGuardService.EMERGENCY_UNLOCK_DURATION_MS
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            StudyLockApp.repository.focusRecordRepository.insertRecord(FocusRecord(date = today, courseId = null, durationSec = 0, unlockCount = 1))
        } catch (_: Exception) {}
    }
    FocusGuardService.startService(ctx)
    onFinish()
}

@Composable
private fun PasswordPanel(
    displayedPassword: String, passwordInput: String, passwordError: Boolean,
    errorAttempts: Int, lockoutRemaining: Int, focusRequester: FocusRequester,
    onPasswordChange: (String) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("输入密码以解锁", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = displayedPassword, onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("请输入密码", color = Color.White.copy(alpha = 0.4f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    singleLine = true, isError = passwordError,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White.copy(alpha = 0.6f), unfocusedBorderColor = Color.White.copy(alpha = 0.3f), errorBorderColor = Color(0xFFE57373), cursorColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = lockoutRemaining <= 0
                )
                if (passwordError && lockoutRemaining <= 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("密码错误，已尝试 $errorAttempts/3 次", color = Color(0xFFE57373), fontSize = 12.sp)
                }
                if (lockoutRemaining > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("连续3次错误，${lockoutRemaining / 60}分钟内不可再试", color = Color(0xFFE57373), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(22.dp), enabled = passwordInput.isNotEmpty() && lockoutRemaining <= 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), disabledContainerColor = Color.White.copy(alpha = 0.08f))
                ) { Text("确认解锁", color = if (passwordInput.isNotEmpty() && lockoutRemaining <= 0) Color.White else Color.White.copy(alpha = 0.4f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("返回", color = Color.White.copy(alpha = 0.6f)) }
    }
}
