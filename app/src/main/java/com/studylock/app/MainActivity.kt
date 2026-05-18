package com.studylock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studylock.app.feature.deepwork.DeepWorkScreen
import com.studylock.app.feature.focus.PasswordSetupScreen
import com.studylock.app.feature.permission.PermissionGuideScreen
import com.studylock.app.feature.schedule.ClassTimeConfigScreen
import com.studylock.app.feature.schedule.ScheduleScreen
import com.studylock.app.feature.settings.AboutAppScreen
import com.studylock.app.feature.settings.DataBackupScreen
import com.studylock.app.ui.theme.StudyLockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object DeepWork : BottomNavItem("deep_work", "深度", Icons.Default.Psychology)
    object Schedule : BottomNavItem("schedule", "课表", Icons.Default.Schedule)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyLockTheme {
                StudyLockMainApp()
            }
        }
    }
}

@Composable
fun StudyLockMainApp() {
    val navController = rememberNavController()
    val bottomItems = listOf(BottomNavItem.DeepWork, BottomNavItem.Schedule)

    var isGuideNeeded by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val repository = StudyLockApp.repository
                val guideDone = repository.userSettingsRepository.getValueByKey("permission_guide_done")
                isGuideNeeded = guideDone != "true"
            } catch (_: Exception) {
                isGuideNeeded = true
            }
            isChecking = false
        }
    }

    if (isChecking) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isGuideNeeded) "permission_guide" else BottomNavItem.DeepWork.route

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomItems.map { it.route }

            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            Modifier.padding(innerPadding)
        ) {
            composable("permission_guide") {
                PermissionGuideScreen(
                    onComplete = {
                        isGuideNeeded = false
                        navController.navigate(BottomNavItem.DeepWork.route) {
                            popUpTo("permission_guide") { inclusive = true }
                        }
                    }
                )
            }
            composable(BottomNavItem.DeepWork.route) {
                DeepWorkScreen(
                    onNavigateToPasswordSetup = { navController.navigate("password_setup") },
                    onNavigateToPermissionGuide = { navController.navigate("permission_guide") },
                    onNavigateToDataBackup = { navController.navigate("data_backup") },
                    onNavigateToAbout = { navController.navigate("about_app") }
                )
            }
            composable(BottomNavItem.Schedule.route) {
                ScheduleScreen(
                    onNavigateToClassTimeConfig = { navController.navigate("class_time_config") }
                )
            }
            composable("password_setup") {
                PasswordSetupScreen(onBack = { navController.popBackStack() })
            }
            composable("class_time_config") {
                ClassTimeConfigScreen(onBack = { navController.popBackStack() })
            }
            composable("data_backup") {
                DataBackupScreen(onBack = { navController.popBackStack() })
            }
            composable("about_app") {
                AboutAppScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
