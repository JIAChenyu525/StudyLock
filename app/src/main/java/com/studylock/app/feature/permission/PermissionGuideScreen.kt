package com.studylock.app.feature.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(
    onComplete: () -> Unit,
    viewModel: PermissionGuideViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAdvanced by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("欢迎使用 StudyLock") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "只需两步，即可开启深度工作",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "建议开启以下权限以获得最佳体验",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step 1: Core permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "① 核心权限",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "检测前台应用，自动触发专注模式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SimplePermissionRow(
                        title = "无障碍服务",
                        subtitle = "检测前台应用",
                        isGranted = uiState.permissionStates.accessibilityEnabled,
                        onClick = { PermissionUtils.openAccessibilitySettings(context) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SimplePermissionRow(
                        title = "使用情况访问",
                        subtitle = "读取应用使用数据",
                        isGranted = uiState.permissionStates.usageStatsGranted,
                        onClick = { PermissionUtils.openUsageStatsSettings(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: Battery protection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "② 电池保护",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "防止系统在后台杀死守护服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SimplePermissionRow(
                        title = "忽略电池优化",
                        subtitle = "保持服务持续运行",
                        isGranted = uiState.permissionStates.batteryOptimizationIgnored,
                        onClick = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced settings (collapsible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                onClick = { showAdvanced = !showAdvanced }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "高级设置",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            SimplePermissionRow(
                                title = "通知权限",
                                subtitle = if (android.os.Build.VERSION.SDK_INT >= 33) "Android 13+ 需要" else "已自动授权",
                                isGranted = uiState.permissionStates.notificationGranted,
                                onClick = { PermissionUtils.openNotificationSettings(context) }
                            )

                            if (uiState.manufacturerInfo != null &&
                                uiState.manufacturerInfo!!.manufacturer != ManufacturerType.OTHER
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.showManufacturerGuide() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${uiState.manufacturerInfo!!.displayName} 保活设置",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val coreGranted = uiState.permissionStates.accessibilityEnabled

            Button(
                onClick = {
                    viewModel.completeGuide()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    if (coreGranted) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "开始使用 StudyLock",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (uiState.showManufacturerDialog && uiState.manufacturerInfo != null) {
        ManufacturerGuideDialog(
            info = uiState.manufacturerInfo!!,
            onDismiss = { viewModel.hideManufacturerGuide() },
            onOpenSettings = { intent ->
                try { context.startActivity(intent) } catch (_: Exception) {}
                viewModel.hideManufacturerGuide()
            }
        )
    }
}

@Composable
private fun SimplePermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isGranted) Color(0xFF4CAF50).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isGranted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) "已开启" else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isGranted) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "前往设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ManufacturerGuideDialog(
    info: ManufacturerGuideInfo,
    onDismiss: () -> Unit,
    onOpenSettings: (android.content.Intent?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${info.displayName} 保活设置")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                info.guideSteps.forEach { step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (info.settingsIntent != null) {
                TextButton(onClick = { onOpenSettings(info.settingsIntent) }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打开设置")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("我知道了")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后设置")
            }
        }
    )
}
