package com.studylock.app.feature.schedule

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.studylock.app.data.entity.Course
import com.studylock.app.ui.util.CourseColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseOcrImportScreen(
    semesterId: Long,
    semesterName: String,
    onBack: () -> Unit,
    onImportCourses: (List<Course>) -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var parsedCourses by remember { mutableStateOf<List<OcrCourseCandidate>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var showRawText by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoFile = remember {
        try {
            val file = File(context.cacheDir, "ocr_photo_${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            file
        } catch (_: Exception) {
            null
        }
    }

    val cameraUri = remember(photoFile) {
        photoFile?.let { file ->
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            imageUri = cameraUri
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
    }

    fun onCameraClick() {
        if (hasCameraPermission) {
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun onGalleryClick() {
        galleryLauncher.launch("image/*")
    }

    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            isProcessing = true
            statusMessage = "正在加载图片..."
            recognizedText = ""
            parsedCourses = emptyList()

            try {
                val result = withContext(Dispatchers.IO) {
                    statusMessage = "正在识别文字，首次使用需下载模型..."

                    val scaledBitmap = loadScaledBitmap(context, uri, 1280)
                        ?: throw IllegalStateException("无法加载图片，请确认图片格式正确")

                    val w = scaledBitmap.width
                    val h = scaledBitmap.height
                    if (w < 400 || h < 400) {
                        scaledBitmap.recycle()
                        throw IllegalStateException("图片分辨率过低（${w}×${h}），请使用更清晰的图片")
                    }

                    val image = InputImage.fromBitmap(scaledBitmap, 0)
                    val recognizer = TextRecognition.getClient(
                        ChineseTextRecognizerOptions.Builder().build()
                    )

                    val mlKitResult: Text
                    try {
                        mlKitResult = withTimeout(40_000) {
                            recognizer.process(image).await()
                        }
                    } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                        scaledBitmap.recycle()
                        throw IllegalStateException(
                            "文字识别超时。首次使用需下载中文识别模型（约30秒），请确认网络连接正常后重试"
                        )
                    }

                    val ocrLines = mlKitResult.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            OcrTextLine(
                                text = line.text,
                                boundingBox = line.boundingBox
                            )
                        }
                    }

                    scaledBitmap.recycle()
                    Pair(mlKitResult.text, ocrLines)
                }

                val (rawText, structuredLines) = result
                recognizedText = rawText

                statusMessage = "正在解析课程..."

                parsedCourses = if (structuredLines.any { it.boundingBox != null }) {
                    CourseOcrParser.parseFromStructuredLines(structuredLines)
                } else {
                    CourseOcrParser.parse(rawText)
                }

                statusMessage = if (parsedCourses.isEmpty()) {
                    "未能自动解析出课程，请查看下方识别文字手动确认"
                } else {
                    ""
                }
            } catch (e: Throwable) {
                recognizedText = "识别失败: ${e.message ?: "未知错误"}"
                statusMessage = ""
            }
            isProcessing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "OCR课表导入 - $semesterName",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        statusMessage.ifBlank { "正在识别课表文字..." },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (statusMessage.contains("首次") || statusMessage.contains("下载")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "请耐心等待，后续使用将无需再次下载",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onCameraClick() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("拍照")
                        }
                        Button(
                            onClick = { onGalleryClick() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("从相册选择")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showManualInput = !showManualInput },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("手动输入课表文字")
                    }

                    if (showManualInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("在此粘贴或输入课表文字，支持常见格式...") },
                            maxLines = 8
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (manualText.isNotBlank()) {
                                    parsedCourses = CourseOcrParser.parse(manualText)
                                    recognizedText = manualText
                                    statusMessage = if (parsedCourses.isEmpty()) "未能解析出课程" else ""
                                    showRawText = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = manualText.isNotBlank()
                        ) {
                            Text("解析文字")
                        }
                    }
                }

                if (!hasCameraPermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "拍照需要相机权限，请点击拍照按钮后授权",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                imageUri?.let { uri ->
                    item {
                        var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(uri) {
                            previewBitmap = withContext(Dispatchers.IO) {
                                try {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        BitmapFactory.decodeStream(it)
                                    }
                                } catch (_: Exception) { null }
                            }
                        }

                        previewBitmap?.let { bmp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "课表图片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Photo guide overlay frame
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                            .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = "请将课表对准框内",
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp),
                                        color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (parsedCourses.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "识别到 ${parsedCourses.size} 门课程",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = {
                                parsedCourses = parsedCourses + OcrCourseCandidate(
                                    name = "新课程", weekday = 1, colorIndex = parsedCourses.size % CourseColors.PALETTE.size
                                )
                            }) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("添加", fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "识别到 ${parsedCourses.size} 门课程",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击课程可编辑详情，确认无误后点击下方按钮导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (statusMessage.isNotBlank() && parsedCourses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (recognizedText.isNotBlank() && !recognizedText.startsWith("识别失败")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = recognizedText.take(500),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (recognizedText.length > 500) {
                                        Text(
                                            text = "...（文字过长已截断，完整内容共${recognizedText.length}字）",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "提示：请确保课表图片清晰完整，包含「周一」等星期标识和节次信息",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (recognizedText.startsWith("识别失败")) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = recognizedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = {
                                    imageUri?.let {
                                        val currentUri = it
                                        imageUri = null
                                        imageUri = currentUri
                                    }
                                }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                }

                itemsIndexed(parsedCourses) { index, course ->
                    val color = CourseColors.getColorFromIndex(course.colorIndex)
                    val onColor = CourseColors.getOnColor(color)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                        onClick = {
                            editingIndex = index
                            showEditDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = course.name.take(1),
                                    color = onColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val dayNames = listOf("", "一", "二", "三", "四", "五", "六", "日")
                                    Text(
                                        text = "周${dayNames[course.weekday.coerceIn(1, 7)]} 第${course.startSection}-${course.endSection}节",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "第${course.startWeek}-${course.endWeek}周",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                course.location.ifBlank { null }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                course.teacher.ifBlank { null }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (recognizedText.isNotBlank() && parsedCourses.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { showRawText = !showRawText },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showRawText) "隐藏识别原文" else "查看识别原文",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (showRawText) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = recognizedText,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (parsedCourses.isNotEmpty()) {
            Button(
                onClick = {
                    val courses = parsedCourses.map { it.toCourse(semesterId) }
                    onImportCourses(courses)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导入 ${parsedCourses.size} 门课程")
            }
        }
    }

    if (showEditDialog && editingIndex in parsedCourses.indices) {
        OcrCourseEditDialog(
            candidate = parsedCourses[editingIndex],
            onDismiss = {
                showEditDialog = false
                editingIndex = -1
            },
            onSave = { updated ->
                parsedCourses = parsedCourses.toMutableList().apply {
                    set(editingIndex, updated)
                }
                showEditDialog = false
                editingIndex = -1
            },
            onDelete = {
                parsedCourses = parsedCourses.toMutableList().apply {
                    removeAt(editingIndex)
                }
                showEditDialog = false
                editingIndex = -1
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrCourseEditDialog(
    candidate: OcrCourseCandidate,
    onDismiss: () -> Unit,
    onSave: (OcrCourseCandidate) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(candidate.name) }
    var location by remember { mutableStateOf(candidate.location) }
    var teacher by remember { mutableStateOf(candidate.teacher) }
    var weekday by remember { mutableStateOf(candidate.weekday) }
    var startSection by remember { mutableStateOf(candidate.startSection) }
    var endSection by remember { mutableStateOf(candidate.endSection) }
    var startWeek by remember { mutableStateOf(candidate.startWeek) }
    var endWeek by remember { mutableStateOf(candidate.endWeek) }
    var weekType by remember { mutableStateOf(candidate.weekType) }
    var colorIndex by remember { mutableStateOf(candidate.colorIndex) }

    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val weekTypeNames = listOf("每周", "单周", "双周")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "编辑课程",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("课程名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("上课地点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("任课教师") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayNames.forEachIndexed { index, day ->
                        val selected = weekday == index + 1
                        FilterChip(
                            selected = selected,
                            onClick = { weekday = index + 1 },
                            label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开始节次", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = startSection.toString(),
                            onValueChange = {
                                startSection = it.toIntOrNull()?.coerceIn(1, 14) ?: startSection
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束节次", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = endSection.toString(),
                            onValueChange = {
                                endSection = it.toIntOrNull()?.coerceIn(1, 14) ?: endSection
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("起始周", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = startWeek.toString(),
                            onValueChange = {
                                startWeek = it.toIntOrNull()?.coerceIn(1, 25) ?: startWeek
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束周", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = endWeek.toString(),
                            onValueChange = {
                                endWeek = it.toIntOrNull()?.coerceIn(1, 25) ?: endWeek
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekTypeNames.forEachIndexed { index, label ->
                        FilterChip(
                            selected = weekType == index,
                            onClick = { weekType = index },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                candidate.copy(
                                    name = name.trim(),
                                    location = location.trim(),
                                    teacher = teacher.trim(),
                                    weekday = weekday,
                                    startSection = startSection,
                                    endSection = endSection,
                                    startWeek = startWeek,
                                    endWeek = endWeek,
                                    weekType = weekType,
                                    colorIndex = colorIndex
                                )
                            )
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

private fun loadScaledBitmap(context: android.content.Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val rawBytes = inputStream.use { it.readBytes() }

        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)

        val rawW = decodeOptions.outWidth
        val rawH = decodeOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        val scale = maxOf(rawW / maxDimension, rawH / maxDimension, 1)
        val sampleSize = Integer.highestOneBit(scale).coerceAtLeast(1)

        val finalOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions) ?: return null

        val exifRotation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }

        if (exifRotation != 0f) {
            val matrix = Matrix().apply { postRotate(exifRotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } else {
            bitmap
        }
    } catch (_: Throwable) {
        null
    }
}
