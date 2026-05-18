package com.studylock.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studylock.app.data.entity.Course
import com.studylock.app.data.entity.Semester
import com.studylock.app.ui.util.CourseColors

private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToClassTimeConfig: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScheduleTopBar(
                uiState = uiState,
                onSemesterSelected = { viewModel.selectSemester(it) },
                onPreviousWeek = { viewModel.previousWeek() },
                onNextWeek = { viewModel.nextWeek() },
                onAddClick = { viewModel.showAddDialog() },
                onOcrImportClick = { viewModel.showOcrImport() },
                onTimeConfigClick = onNavigateToClassTimeConfig
            )

            ScheduleHeaderRow()

            ScheduleGrid(
                uiState = uiState,
                onCourseClick = { viewModel.showDetailDialog(it) }
            )
        }

        AddEditCourseDialog(
            visible = uiState.showAddDialog,
            existingCourse = null,
            classTimeConfigs = uiState.classTimeConfigs,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { viewModel.saveCourse(it) }
        )

        AddEditCourseDialog(
            visible = uiState.showEditDialog,
            existingCourse = uiState.selectedCourse,
            classTimeConfigs = uiState.classTimeConfigs,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { viewModel.saveCourse(it) }
        )

        CourseDetailDialog(
            visible = uiState.showDetailDialog,
            course = uiState.selectedCourse,
            classTimeConfigs = uiState.classTimeConfigs,
            onDismiss = { viewModel.hideDetailDialog() },
            onEdit = { viewModel.showEditDialog(it) },
            onDelete = { viewModel.deleteCourse(it) }
        )

        if (uiState.showTimeConfig) {
            ClassTimeConfigScreen(
                onBack = { viewModel.hideTimeConfig() }
            )
        }

        if (uiState.showOcrImport) {
            CourseOcrImportScreen(
                semesterId = uiState.selectedSemester?.id ?: 0L,
                semesterName = uiState.selectedSemester?.name
                    ?: uiState.semesters.firstOrNull()?.name ?: "当前学期",
                onBack = { viewModel.hideOcrImport() },
                onImportCourses = { viewModel.importCourses(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTopBar(
    uiState: ScheduleUiState,
    onSemesterSelected: (Semester) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddClick: () -> Unit,
    onOcrImportClick: () -> Unit,
    onTimeConfigClick: () -> Unit
) {
    var semesterExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Text(
                        text = uiState.selectedSemester?.name ?: "选择学期",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .clickable { semesterExpanded = true }
                            .padding(vertical = 8.dp)
                    )
                    DropdownMenu(
                        expanded = semesterExpanded,
                        onDismissRequest = { semesterExpanded = false }
                    ) {
                        uiState.semesters.forEach { semester ->
                            DropdownMenuItem(
                                text = { Text(semester.name) },
                                onClick = {
                                    onSemesterSelected(semester)
                                    semesterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onTimeConfigClick) {
                Icon(Icons.Default.Settings, contentDescription = "节次设置")
            }
            IconButton(onClick = onOcrImportClick) {
                Icon(Icons.Default.CameraAlt, contentDescription = "OCR导入")
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousWeek, enabled = uiState.currentWeek > 1) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一周")
        }

        Text(
            text = "第${uiState.currentWeek}周",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        IconButton(onClick = onNextWeek, enabled = uiState.currentWeek < uiState.totalWeeks) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一周")
        }
    }
}

@Composable
private fun ScheduleHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Spacer(modifier = Modifier.width(36.dp))

        WEEKDAY_NAMES.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

private fun filterCoursesForCell(
    courses: List<Course>,
    weekday: Int,
    section: Int,
    currentWeek: Int
): List<Course> {
    return courses.filter { course ->
        course.weekday == weekday &&
        section in course.startSection..course.endSection &&
        currentWeek in course.startWeek..course.endWeek &&
        (course.weekType == 0 ||
         (course.weekType == 1 && currentWeek % 2 == 1) ||
         (course.weekType == 2 && currentWeek % 2 == 0))
    }
}

@Composable
private fun ScheduleGrid(
    uiState: ScheduleUiState,
    onCourseClick: (Course) -> Unit
) {
    val scrollState = rememberScrollState()
    val maxSection = uiState.maxSection
    val cellHeight = 56.dp

    val courseMap = remember(uiState.courses, uiState.currentWeek) {
        val map = mutableMapOf<Pair<Int, Int>, List<Course>>()
        for (section in 1..maxSection) {
            for (weekday in 1..7) {
                val filtered = filterCoursesForCell(uiState.courses, weekday, section, uiState.currentWeek)
                if (filtered.isNotEmpty()) {
                    map[Pair(weekday, section)] = filtered
                }
            }
        }
        map
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 4.dp)
    ) {
        for (section in 1..maxSection) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(cellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$section",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                for (weekday in 1..7) {
                    val courses = courseMap[Pair(weekday, section)]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeight)
                            .padding(1.dp)
                    ) {
                        val startCourse = courses?.firstOrNull { it.startSection == section }
                        if (startCourse != null) {
                            val spanCount = (startCourse.endSection - startCourse.startSection + 1).coerceAtLeast(1)
                            val courseColor = CourseColors.getColorFromHex(startCourse.colorTag)
                            val onCourseColor = CourseColors.getOnColor(courseColor)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cellHeight * spanCount + (spanCount - 1).dp * 2)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(courseColor.copy(alpha = 0.85f))
                                    .clickable { onCourseClick(startCourse) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Column {
                                    Text(
                                        text = startCourse.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = onCourseColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (spanCount >= 2) {
                                        startCourse.location?.let { loc ->
                                            Text(
                                                text = loc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = onCourseColor.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (courses == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
