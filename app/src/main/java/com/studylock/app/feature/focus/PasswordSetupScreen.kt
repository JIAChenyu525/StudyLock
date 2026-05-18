package com.studylock.app.feature.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupScreen(
    onBack: () -> Unit,
    viewModel: PasswordSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isExistingPassword) "修改密码" else "设置密码") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.step) {
                PasswordSetupStep.INPUT -> {
                    PasswordInputStep(
                        uiState = uiState,
                        onPasswordChange = viewModel::onPasswordChange,
                        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                        onToggleVisibility = viewModel::togglePasswordVisibility,
                        onNext = {
                            viewModel.goToConfirmStep()
                            focusManager.clearFocus()
                        },
                        onRemovePassword = viewModel::removePassword
                    )
                }

                PasswordSetupStep.CONFIRM -> {
                    PasswordConfirmStep(
                        uiState = uiState,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onToggleVisibility = viewModel::toggleConfirmPasswordVisibility,
                        onBack = {
                            viewModel.goBackToInputStep()
                            focusManager.clearFocus()
                        },
                        onSave = viewModel::savePassword
                    )
                }

                PasswordSetupStep.SUCCESS -> {
                    PasswordSuccessStep(
                        onDone = onBack
                    )
                }
            }

            // 错误提示 Snackbar
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                uiState.errorMessage?.let { error ->
                    Snackbar(
                        action = {
                            TextButton(onClick = viewModel::clearError) {
                                Text("关闭", color = MaterialTheme.colorScheme.inversePrimary)
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordInputStep(
    uiState: PasswordSetupUiState,
    onPasswordChange: (String) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onNext: () -> Unit,
    onRemovePassword: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 提示信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "密码设置建议",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "建议设置16位以上复杂密码，密码越复杂专注越有效。包含大小写字母、数字和特殊字符的密码更安全。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // 如果已有密码，先验证当前密码
        if (uiState.isExistingPassword) {
            OutlinedTextField(
                value = uiState.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("当前密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 新密码输入
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("新密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (uiState.isPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (uiState.isPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (uiState.isPasswordVisible) "隐藏密码" else "显示密码"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onNext() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                Text("${uiState.password.length} 个字符")
            }
        )

        // 密码强度指示条
        PasswordStrengthIndicator(strength = uiState.passwordStrength)

        Spacer(modifier = Modifier.height(8.dp))

        // 下一步按钮
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            enabled = uiState.password.length >= 6
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("下一步：确认密码", fontSize = 16.sp)
        }

        // 如果已有密码，提供移除密码选项
        if (uiState.isExistingPassword) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onRemovePassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "移除密码保护",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "密码强度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PasswordUtils.getStrengthColor(strength)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { PasswordUtils.getStrengthProgress(strength) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = PasswordUtils.getStrengthColor(strength),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // 强度说明
        val description = when (strength) {
            PasswordStrength.WEAK -> "密码过于简单，容易被破解"
            PasswordStrength.MEDIUM -> "密码强度一般，建议增加复杂度"
            PasswordStrength.STRONG -> "密码强度很高，非常安全"
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PasswordConfirmStep(
    uiState: PasswordSetupUiState,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 确认提示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "确认密码",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请再次输入密码以确认，确保您记住了这个密码。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // 密码强度回顾
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("密码强度：", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = uiState.passwordStrength.label,
                fontWeight = FontWeight.Bold,
                color = PasswordUtils.getStrengthColor(uiState.passwordStrength)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${uiState.password.length}位)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 确认密码输入
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("再次输入密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (uiState.isConfirmPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (uiState.isConfirmPasswordVisible) "隐藏密码" else "显示密码"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSave() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            isError = uiState.confirmPassword.isNotEmpty() &&
                    uiState.confirmPassword != uiState.password,
            supportingText = {
                if (uiState.confirmPassword.isNotEmpty() &&
                    uiState.confirmPassword != uiState.password
                ) {
                    Text("两次输入的密码不一致")
                } else if (uiState.confirmPassword == uiState.password &&
                    uiState.confirmPassword.isNotEmpty()
                ) {
                    Text("密码一致 ✓")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 保存按钮
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            enabled = uiState.confirmPassword == uiState.password &&
                    uiState.confirmPassword.isNotEmpty()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存密码", fontSize = 16.sp)
        }

        // 返回按钮
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("返回修改密码")
        }
    }
}

@Composable
private fun PasswordSuccessStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✅",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "密码设置成功",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "密码已安全加密存储，解锁时需要输入正确密码才能退出专注模式。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("完成", fontSize = 16.sp)
        }
    }
}
