package com.studylock.app.feature.deepwork

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studylock.app.service.FocusGuardService
import com.studylock.app.ui.util.CourseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepWorkScreen(
    onNavigateToPasswordSetup: () -> Unit = {},
    onNavigateToPermissionGuide: () -> Unit = {},
    onNavigateToDataBackup: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: DeepWorkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "深度工作",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            actions = {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.errorMessage?.contains("显示在其他应用上层") == true) {
                        val appContext = LocalContext.current
                        OutlinedButton(onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${appContext.packageName}")
                            )
                            appContext.startActivity(intent)
                        }) {
                            Text("去开启「显示在上层」权限")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("重试")
                    }
                }
            }
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item { DeepWorkStartCard(uiState, showDurationPicker, { showDurationPicker = it }, viewModel) }
                item { ChainCard(uiState) }
                item { GuardStatusCard(uiState, viewModel) }
                item { TodayTimelineCard(uiState) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showSettings) {
        SettingsBottomSheet(
            onDismiss = { showSettings = false },
            onNavigateToPasswordSetup = {
                showSettings = false
                onNavigateToPasswordSetup()
            },
            onNavigateToDataBackup = {
                showSettings = false
                onNavigateToDataBackup()
            },
            onNavigateToAbout = {
                showSettings = false
                onNavigateToAbout()
            }
        )
    }
}

@Composable
private fun DeepWorkStartCard(
    uiState: DeepWorkUiState,
    showPicker: Boolean,
    onTogglePicker: (Boolean) -> Unit,
    viewModel: DeepWorkViewModel
) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isManualModeActive)
                Color(0xFF4CAF50).copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isManualModeActive) {
                Text(
                    text = "深度工作中",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "剩余 ${uiState.manualModeRemainingMinutes} 分钟",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.stopManualDeepWork() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373)
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("停止深度工作", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .clickable {
                            onTogglePicker(!showPicker)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "开始深度工作",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "开始深度工作",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "选择一个时长，进入完全专注状态",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = showPicker) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(30, 60, 90, 120).forEach { duration ->
                                val selected = uiState.selectedDuration == duration
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.startManualDeepWork(duration)
                                        viewModel.launchLockActivity(ctx)
                                        onTogglePicker(false)
                                    },
                                    label = {
                                        Text(
                                            "${duration}分钟",
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainCard(uiState: DeepWorkUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chainColor = when {
                uiState.consecutiveDays >= 7 -> Color(0xFFFF6D00)
                uiState.consecutiveDays >= 3 -> Color(0xFFFFB74D)
                uiState.consecutiveDays > 0 -> Color(0xFF81C784)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }

            Text(
                text = if (uiState.consecutiveDays > 0) "🔥" else "🌱",
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.consecutiveDays > 0) {
                        "已连续专注 ${uiState.consecutiveDays} 天"
                    } else {
                        "开始你的深度工作之旅"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = chainColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (uiState.todayHasSession) {
                    Text(
                        text = "今天已完成深度工作 ✅",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "今天还没有深度工作记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.consecutiveDays > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.consecutiveDays}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp
                        ),
                        color = chainColor
                    )
                    Text(
                        text = "天",
                        style = MaterialTheme.typography.labelSmall,
                        color = chainColor
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.weeklyTotalMinutes}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "本周分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GuardStatusCard(
    uiState: DeepWorkUiState,
    viewModel: DeepWorkViewModel
) {
    val context = LocalContext.current
    val overlayGranted = try { android.provider.Settings.canDrawOverlays(context) } catch (_: Exception) { false }
    val batteryGranted = try {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: Exception) { false }
    val allReady = overlayGranted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("需要开启的权限", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            QuickPermRow(
                label = "显示在其他应用上层",
                description = "锁机页面覆盖其他APP",
                isGranted = overlayGranted,
                onClick = {
                    context.startActivity(android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                }
            )
            QuickPermRow(
                label = "忽略电池优化（建议）",
                description = "防止系统杀死后台",
                isGranted = batteryGranted,
                onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (allReady) "✅ 权限已就绪" else "⚠ 请开启「显示在上层」权限后使用",
                style = MaterialTheme.typography.bodySmall,
                color = if (allReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun QuickPermRow(
    label: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isGranted) Color(0xFF4CAF50).copy(alpha = 0.08f) else Color(0xFFE57373).copy(alpha = 0.08f),
        onClick = if (isGranted) ({}) else onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFE57373)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) "已授权" else description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isGranted) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "去开启",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayTimelineCard(uiState: DeepWorkUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "今日时间线",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.todayClassPeriods.isEmpty() && uiState.suggestedDeepWorkWindows.isEmpty()) {
                Text(
                    text = "今天没有课程，是深度工作的好日子！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.todayClassPeriods.forEach { period ->
                TimelineRow(
                    isActive = period.isActive,
                    isClass = true,
                    timeLabel = "${period.startTime} - ${period.endTime}",
                    title = period.courseName,
                    subtitle = period.sectionRange,
                    colorIndex = period.colorIndex
                )
            }

            uiState.suggestedDeepWorkWindows.forEach { window ->
                TimelineRow(
                    isActive = false,
                    isClass = false,
                    timeLabel = "${window.startTime} - ${window.endTime}",
                    title = "${window.durationMinutes}分钟深度工作建议",
                    subtitle = window.label,
                    colorIndex = -1
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    isActive: Boolean,
    isClass: Boolean,
    timeLabel: String,
    title: String,
    subtitle: String,
    colorIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when {
                        isActive -> Color(0xFF4CAF50)
                        isClass -> CourseColors.getColorFromIndex(colorIndex.coerceAtLeast(0))
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isActive) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "进行中",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!isClass) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "建议",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToPasswordSetup: () -> Unit,
    onNavigateToDataBackup: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "密码设置",
                subtitle = "设置或修改解锁密码",
                onClick = onNavigateToPasswordSetup
            )

            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = "数据备份",
                subtitle = "导出或导入应用数据",
                onClick = onNavigateToDataBackup
            )

            SettingsItem(
                icon = Icons.Default.Info,
                title = "关于应用",
                subtitle = "StudyLock v1.0",
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
