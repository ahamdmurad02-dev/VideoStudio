package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.camera.CameraViewModel
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.recorder.ScreenRecorderScreen
import com.example.ui.settings.AppThemeMode
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.VideoStudioTheme

enum class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CAMERA("camera", R.string.nav_screen_recorder, Icons.Filled.ScreenShare, Icons.Outlined.ScreenShare),
    VIDEOS("videos", R.string.nav_videos, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    EDITOR("editor", R.string.nav_editor, Icons.Filled.Edit, Icons.Outlined.Edit),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val cameraViewModel: CameraViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()
            val isDark = when (settingsState.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            VideoStudioTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.CAMERA.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                            Screen.entries.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = stringResource(screen.titleRes)
                                        )
                                    },
                                    label = { Text(stringResource(screen.titleRes)) },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.CAMERA.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.CAMERA.route) {
                                ScreenRecorderScreen(
                                    cameraViewModel = cameraViewModel,
                                    onNavigateToEditor = { video ->
                                        editorViewModel.loadVideo(video)
                                        navController.navigate(Screen.EDITOR.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToVideos = {
                                        navController.navigate(Screen.VIDEOS.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(Screen.VIDEOS.route) {
                                LibraryScreen(
                                    viewModel = libraryViewModel,
                                    onNavigateToEditor = { video ->
                                        editorViewModel.loadVideo(video)
                                        navController.navigate(Screen.EDITOR.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(Screen.EDITOR.route) {
                                EditorScreen(
                                    viewModel = editorViewModel,
                                    onNavigateToLibrary = {
                                        navController.navigate(Screen.VIDEOS.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(Screen.SETTINGS.route) {
                                SettingsScreen(viewModel = settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
