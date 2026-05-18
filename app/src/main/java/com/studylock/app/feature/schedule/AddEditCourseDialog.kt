package com.studylock.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.studylock.app.data.entity.ClassTimeConfig
import com.studylock.app.data.entity.Course
import com.studylock.app.ui.util.CourseColors

private val WEEKDAY_OPTIONS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val WEEK_TYPE_OPTIONS = listOf("每周", "单周", "双周")
private val SECTION_RANGE = 1..14

@Composable
fun AddEditCourseDialog(
    visible: Boolean,
    existingCourse: Course?,
    classTimeConfigs: List<ClassTimeConfig>,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    if (!visible) return

    val isEdit = existingCourse != null

    var name by remember(existingCourse) { mutableStateOf(existingCourse?.name ?: "") }
    var location by remember(existingCourse) { mutableStateOf(existingCourse?.location ?: "") }
    var teacher by remember(existingCourse) { mutableStateOf(existingCourse?.teacher ?: "") }
    var selectedWeekdays by remember(existingCourse) {
        mutableStateOf(existingCourse?.weekday?.let { setOf(it) } ?: emptySet())
    }
    var startSection by remember(existingCourse) { mutableStateOf(existingCourse?.startSection ?: 1) }
    var endSection by remember(existingCourse) { mutableStateOf(existingCourse?.endSection ?: 2) }
    var startWeek by remember(existingCourse) { mutableStateOf(existingCourse?.startWeek ?: 1) }
    var endWeek by remember(existingCourse) { mutableStateOf(existingCourse?.endWeek ?: 16) }
    var weekType by remember(existingCourse) { mutableStateOf(existingCourse?.weekType ?: 0) }
    var selectedColorIndex by remember(existingCourse) {
        mutableStateOf(
            existingCourse?.colorTag?.let { tag ->
                CourseColors.PALETTE_HEX.indexOf(tag).takeIf { it >= 0 } ?: 0
            } ?: 0
        )
    }

    var nameError by remember { mutableStateOf(false) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEdit) "编辑课程" else "添加课程",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 20) {
                            name = it
                            nameError = it.isBlank()
                        }
                    },
                    label = { Text("课程名称 *") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("课程名称不能为空") }} else null,
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

                Text(
                    text = "上课星期",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEKDAY_OPTIONS.forEachIndexed { index, day ->
                        val isSelected = (index + 1) in selectedWeekdays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val weekday = index + 1
                                selectedWeekdays = if (isSelected) {
                                    selectedWeekdays - weekday
                                } else {
                                    selectedWeekdays + weekday
                                }
                            },
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
                        var startExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { startExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("第${startSection}节")
                            }
                            DropdownMenu(expanded = startExpanded, onDismissRequest = { startExpanded = false }) {
                                SECTION_RANGE.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("第${s}节") },
                                        onClick = {
                                            startSection = s
                                            if (endSection < s) endSection = s
                                            startExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束节次", style = MaterialTheme.typography.labelMedium)
                        var endExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { endExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("第${endSection}节")
                            }
                            DropdownMenu(expanded = endExpanded, onDismissRequest = { endExpanded = false }) {
                                SECTION_RANGE.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("第${s}节") },
                                        onClick = {
                                            endSection = s
                                            if (startSection > s) startSection = s
                                            endExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("起始周", style = MaterialTheme.typography.labelMedium)
                        var swExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { swExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("第${startWeek}周")
                            }
                            DropdownMenu(expanded = swExpanded, onDismissRequest = { swExpanded = false }) {
                                (1..25).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("第${w}周") },
                                        onClick = {
                                            startWeek = w
                                            if (endWeek < w) endWeek = w
                                            swExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束周", style = MaterialTheme.typography.labelMedium)
                        var ewExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { ewExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("第${endWeek}周")
                            }
                            DropdownMenu(expanded = ewExpanded, onDismissRequest = { ewExpanded = false }) {
                                (1..25).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("第${w}周") },
                                        onClick = {
                                            endWeek = w
                                            if (startWeek > w) startWeek = w
                                            ewExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("周次类型", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WEEK_TYPE_OPTIONS.forEachIndexed { index, label ->
                        FilterChip(
                            selected = weekType == index,
                            onClick = { weekType = index },
                            label = { Text(label) }
                        )
                    }
                }

                Text("颜色标签", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(CourseColors.PALETTE) { index, color ->
                        val isSelected = index == selectedColorIndex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = CourseColors.getOnColor(color),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            val weekdays = selectedWeekdays.ifEmpty { setOf(1) }
                            weekdays.forEach { day ->
                                val course = Course(
                                    id = if (day == selectedWeekdays.first()) (existingCourse?.id ?: 0) else 0,
                                    name = name.trim(),
                                    location = location.ifBlank { null },
                                    teacher = teacher.ifBlank { null },
                                    weekday = day,
                                    startSection = startSection,
                                    endSection = endSection,
                                    startWeek = startWeek,
                                    endWeek = endWeek,
                                    weekType = weekType,
                                    colorTag = CourseColors.getHexFromIndex(selectedColorIndex),
                                    semesterId = existingCourse?.semesterId ?: 0
                                )
                                onSave(course)
                            }
                        }
                    ) {
                        Text(if (selectedWeekdays.size > 1) "保存 (${selectedWeekdays.size}天)" else "保存")
                    }
                }
            }
        }
    }
}
