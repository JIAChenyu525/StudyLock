package com.studylock.app.feature.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studylock.app.data.entity.ClassTimeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassTimeConfigScreen(
    onBack: () -> Unit,
    viewModel: ClassTimeConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("节次时间配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showResetConfirm() }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "恢复默认")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.configs, key = { it.sectionNo }) { config ->
                ClassTimeConfigItem(
                    config = config,
                    onStartClick = {
                        val parts = config.startTime.split(":")
                        val hour = parts.getOrElse(0) { "8" }.toIntOrNull() ?: 8
                        val minute = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
                        TimePickerDialog(
                            context,
                            { _, h, m -> viewModel.onTimeSelected(h, m) },
                            hour,
                            minute,
                            true
                        ).apply {
                            viewModel.startEditing(config, EditingField.START)
                            show()
                        }
                    },
                    onEndClick = {
                        val parts = config.endTime.split(":")
                        val hour = parts.getOrElse(0) { "8" }.toIntOrNull() ?: 8
                        val minute = parts.getOrElse(1) { "45" }.toIntOrNull() ?: 45
                        TimePickerDialog(
                            context,
                            { _, h, m -> viewModel.onTimeSelected(h, m) },
                            hour,
                            minute,
                            true
                        ).apply {
                            viewModel.startEditing(config, EditingField.END)
                            show()
                        }
                    }
                )
            }
        }
    }

    if (uiState.showResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetConfirm() },
            title = { Text("恢复默认") },
            text = { Text("确定要恢复默认节次时间配置吗？当前修改将被覆盖。") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetToDefault() }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ClassTimeConfigItem(
    config: ClassTimeConfig,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "第${config.sectionNo}节",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(56.dp)
            )

            Text(
                text = config.startTime,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onStartClick)
                    .padding(vertical = 8.dp)
            )

            Text(
                text = "-",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = config.endTime,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEndClick)
                    .padding(vertical = 8.dp)
            )
        }
    }
}
