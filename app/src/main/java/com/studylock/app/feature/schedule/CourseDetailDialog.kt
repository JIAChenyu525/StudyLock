package com.studylock.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.studylock.app.data.entity.ClassTimeConfig
import com.studylock.app.data.entity.Course
import com.studylock.app.ui.util.CourseColors

private val WEEKDAY_NAMES = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val WEEK_TYPE_NAMES = listOf("每周", "单周", "双周")

@Composable
fun CourseDetailDialog(
    visible: Boolean,
    course: Course?,
    classTimeConfigs: List<ClassTimeConfig>,
    onDismiss: () -> Unit,
    onEdit: (Course) -> Unit,
    onDelete: (Course) -> Unit
) {
    if (!visible || course == null) return

    val courseColor = CourseColors.getColorFromHex(course.colorTag)
    val onCourseColor = CourseColors.getOnColor(courseColor)

    val startTime = classTimeConfigs.find { it.sectionNo == course.startSection }?.startTime ?: ""
    val endTime = classTimeConfigs.find { it.sectionNo == course.endSection }?.endTime ?: ""

    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(courseColor.copy(alpha = 0.85f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = onCourseColor
                    )
                }

                DetailRow(label = "上课地点", value = course.location ?: "未设置")
                DetailRow(label = "任课教师", value = course.teacher ?: "未设置")
                DetailRow(
                    label = "上课时间",
                    value = "${WEEKDAY_NAMES.getOrElse(course.weekday) { "未知" }} 第${course.startSection}-${course.endSection}节"
                )
                if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    DetailRow(label = "具体时间", value = "$startTime - $endTime")
                }
                DetailRow(
                    label = "上课周次",
                    value = "第${course.startWeek}-${course.endWeek}周 ${WEEK_TYPE_NAMES.getOrElse(course.weekType) { "全部" }}"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onEdit(course) }) {
                        Text("编辑")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除课程「${course.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(course)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
