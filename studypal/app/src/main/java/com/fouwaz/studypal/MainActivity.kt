package com.fouwaz.studypal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fouwaz.studypal.ui.navigation.Screen
import com.fouwaz.studypal.ui.screens.DraftViewScreen
import com.fouwaz.studypal.ui.screens.LectureOutputViewScreen
import com.fouwaz.studypal.ui.screens.LectureRecordingScreen
import com.fouwaz.studypal.ui.screens.PebbleCollectionScreen
import com.fouwaz.studypal.ui.screens.ProjectListScreen
import com.fouwaz.studypal.ui.screens.VoiceSessionScreen
import com.fouwaz.studypal.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle permission denial - show dialog or message
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request audio permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.ProjectList.route
                    ) {
                        composable(Screen.ProjectList.route) {
                            ProjectListScreen(
                                onNavigateToNewProject = { navController.navigate(Screen.NewProject.route) },
                                onNavigateToSession = { projectId ->
                                    navController.navigate(Screen.VoiceSession.createRoute(projectId))
                                },
                                onNavigateToDraft = { projectId ->
                                    navController.navigate(Screen.DraftView.createRoute(projectId))
                                },
                                onNavigateToPebbleCollection = {
                                    navController.navigate(Screen.PebbleCollection.route)
                                },
                                onNavigateToLectureRecording = {
                                    navController.navigate(Screen.LectureRecording.route)
                                },
                                onNavigateToLecture = { lectureId ->
                                    navController.navigate(Screen.LectureOutput.createRoute(lectureId))
                                }
                            )
                        }

                        composable(Screen.NewProject.route) {
                            com.fouwaz.studypal.ui.screens.NewProjectScreen(
                                onBack = { navController.popBackStack() },
                                onProjectCreatedNavigateToSession = { id ->
                                    navController.navigate(Screen.VoiceSession.createRoute(id))
                                }
                            )
                        }

                        composable(
                            route = Screen.VoiceSession.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            VoiceSessionScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDraft = { navController.navigate(Screen.DraftView.createRoute(it)) }
                            )
                        }

                        composable(
                            route = Screen.DraftView.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            DraftViewScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.PebbleCollection.route) {
                            PebbleCollectionScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.LectureRecording.route,
                            arguments = listOf(
                                navArgument("projectId") {
                                    type = NavType.LongType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId")?.takeIf { it != 0L }
                            LectureRecordingScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() },
                                onRecordingComplete = { lectureId ->
                                    navController.navigate(Screen.LectureOutput.createRoute(lectureId)) {
                                        popUpTo(Screen.ProjectList.route)
                                    }
                                }
                            )
                        }

                        composable(
                            route = Screen.LectureOutput.route,
                            arguments = listOf(navArgument("lectureId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val lectureId = backStackEntry.arguments?.getLong("lectureId") ?: 0L
                            LectureOutputViewScreen(
                                lectureId = lectureId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
