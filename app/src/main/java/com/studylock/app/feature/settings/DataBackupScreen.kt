package com.studylock.app.feature.settings

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.studylock.app.StudyLockApp
import com.studylock.app.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = StudyLockApp.repository

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var lastExportTime by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                lastExportTime = repository.userSettingsRepository.getValueByKey("last_backup_time")
            }
        } catch (_: Exception) {
        }
    }

    val exportData = {
        scope.launch {
            isExporting = true
            try {
                val data = withContext(Dispatchers.IO) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val semesters = repository.semesterRepository.getAllSemesters().first()
                    val courses = repository.courseRepository.getCoursesBySemesterId(
                        semesters.firstOrNull()?.id ?: -1L
                    ).first()
                    val classTimeConfigs = repository.classTimeConfigRepository.getAllConfigs().first()
                    val focusRecords = repository.focusRecordRepository.getAllRecords().first()
                    val userSettings = repository.userSettingsRepository.getAllSettings().first()

                    val backupData = mapOf(
                        "version" to 2,
                        "export_time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        "semesters" to semesters,
                        "courses" to courses,
                        "class_time_configs" to classTimeConfigs,
                        "focus_records" to focusRecords,
                        "user_settings" to userSettings
                    )
                    gson.toJson(backupData)
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "StudyLock_backup_${dateFormat.format(Date())}.json"

                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        }
                        val uri = context.contentResolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                        )
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { os ->
                                os.write(data.toByteArray(Charsets.UTF_8))
                            }
                        }
                    } else {
                        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = java.io.File(dir, fileName)
                        file.writeText(data, Charsets.UTF_8)
                    }
                }

                val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                withContext(Dispatchers.IO) {
                    repository.userSettingsRepository.insertSetting(
                        com.studylock.app.data.entity.UserSettings("last_backup_time", time)
                    )
                }
                lastExportTime = time
                statusMessage = "备份成功！文件已保存到 Download/$fileName"
                showExportDialog = true
            } catch (e: Exception) {
                statusMessage = "备份失败: ${e.message}"
                showExportDialog = true
            }
            isExporting = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isImporting = true
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: throw Exception("无法读取文件")
                }

                withContext(Dispatchers.IO) {
                    val gson = Gson()
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    val map: Map<String, Any> = gson.fromJson(json, type)
                    val version = (map["version"] as? Double)?.toInt() ?: 0
                    if (version < 1 || version > 2) throw Exception("不支持的数据格式版本: $version")
                    importDataToDatabase(gson, map)
                }

                statusMessage = "导入成功！"
                showImportDialog = true
            } catch (e: Exception) {
                statusMessage = "导入失败: ${e.message}"
                showImportDialog = true
            }
            isImporting = false
        }
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(3000)
            statusMessage = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("数据备份") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "备份将导出课程、专注记录、用户设置等所有数据为 JSON 文件。" +
                        "恢复时请选择之前导出的备份文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "导出备份",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            lastExportTime?.let {
                                Text(
                                    "上次备份: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { exportData() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出中…")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出所有数据")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "恢复备份",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "选择之前导出的 .json 文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导入中…")
                        } else {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择备份文件")
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                val isSuccess = msg.contains("成功")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("导出结果") },
                text = { Text(statusMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("导入结果") },
                text = { Text(statusMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

private suspend fun importDataToDatabase(gson: Gson, map: Map<String, Any>) {
    val repository = StudyLockApp.repository
    try {
        val semestersJson = gson.toJson(map["semesters"])
        val semesters: List<Semester> = gson.fromJson(semestersJson, object : TypeToken<List<Semester>>() {}.type)
        semesters.forEach { s -> runCatching { repository.semesterRepository.insertSemester(s) } }

        val coursesJson = gson.toJson(map["courses"])
        val courses: List<Course> = gson.fromJson(coursesJson, object : TypeToken<List<Course>>() {}.type)
        courses.forEach { c -> runCatching { repository.courseRepository.insertCourse(c.copy(id = 0)) } }

        val configsJson = gson.toJson(map["class_time_configs"])
        val configs: List<ClassTimeConfig> = gson.fromJson(configsJson, object : TypeToken<List<ClassTimeConfig>>() {}.type)
        configs.forEach { c -> runCatching { repository.classTimeConfigRepository.insertConfig(c.copy(id = 0)) } }

        val recordsJson = gson.toJson(map["focus_records"])
        val records: List<FocusRecord> = gson.fromJson(recordsJson, object : TypeToken<List<FocusRecord>>() {}.type)
        records.forEach { r -> runCatching { repository.focusRecordRepository.insertRecord(r.copy(id = 0)) } }

        val settingsJson = gson.toJson(map["user_settings"])
        val settings: List<UserSettings> = gson.fromJson(settingsJson, object : TypeToken<List<UserSettings>>() {}.type)
        settings.forEach { s -> runCatching { repository.userSettingsRepository.insertSetting(s) } }
    } catch (_: Exception) {}
}
