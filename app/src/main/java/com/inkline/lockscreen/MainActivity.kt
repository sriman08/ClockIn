package com.inkline.lockscreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inkline.lockscreen.ui.screens.AppearanceSettingsScreen
import com.inkline.lockscreen.ui.screens.DashboardScreen
import com.inkline.lockscreen.ui.screens.LockScreenPreviewScreen
import com.inkline.lockscreen.ui.screens.TaskManagerScreen
import com.inkline.lockscreen.ui.theme.InkLineTheme
import com.inkline.lockscreen.ui.viewmodel.InkLineViewModel

private enum class InkLineRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Today", Icons.Outlined.Home),
    TASKS("tasks", "Tasks", Icons.Outlined.CalendarMonth),
    THEMES("themes", "Themes", Icons.Outlined.Palette),
    LOCK_SCREEN("lock_screen", "Lock Screen", Icons.Outlined.Smartphone)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkLineTheme {
                InkLineAppRoot()
            }
        }
    }
}

@Composable
private fun InkLineAppRoot(viewModel: InkLineViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingEditDate by viewModel.pendingEditDate.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDateIfChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                InkLineRoute.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = InkLineRoute.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(InkLineRoute.DASHBOARD.route) {
                DashboardScreen(
                    uiState = uiState,
                    onQuickEditDate = { date ->
                        viewModel.openEditorForDate(date)
                        navigateToTab(InkLineRoute.TASKS.route)
                    },
                    onOpenLockScreenPreview = {
                        navigateToTab(InkLineRoute.LOCK_SCREEN.route)
                    },
                    onApplyNow = { viewModel.applyToLockScreen() }
                )
            }

            composable(InkLineRoute.TASKS.route) {
                TaskManagerScreen(
                    uiState = uiState,
                    initialEditDate = pendingEditDate,
                    onClearInitialEditDate = viewModel::consumePendingEditDate,
                    onSaveTask = viewModel::saveTask,
                    onDeleteTask = viewModel::deleteTask
                )
            }

            composable(InkLineRoute.THEMES.route) {
                AppearanceSettingsScreen(
                    uiState = uiState,
                    onUpdateSettings = viewModel::updateSettings
                )
            }

            composable(InkLineRoute.LOCK_SCREEN.route) {
                LockScreenPreviewScreen(
                    uiState = uiState,
                    onApplyToLockScreen = { viewModel.applyToLockScreen() },
                    onManualFallback = { viewModel.exportAndLaunchFallbackPicker() }
                )
            }
        }
    }
}
