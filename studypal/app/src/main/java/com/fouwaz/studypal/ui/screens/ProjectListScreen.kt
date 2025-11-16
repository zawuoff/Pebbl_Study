package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.R
import com.fouwaz.studypal.data.local.entity.AchievementEntity
import com.fouwaz.studypal.domain.model.PebbleType
import com.fouwaz.studypal.domain.model.PebbleTypes
import com.fouwaz.studypal.domain.model.Project
import com.fouwaz.studypal.ui.components.MilestoneCelebrationDialog
import com.fouwaz.studypal.ui.components.RecentPebblesRow
import com.fouwaz.studypal.ui.viewmodel.ProjectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel = viewModel(),
    onNavigateToNewProject: () -> Unit,
    onNavigateToSession: (Long) -> Unit,
    onNavigateToDraft: (Long) -> Unit,
    onNavigateToPebbleCollection: () -> Unit,
    onNavigateToLectureRecording: (Long?) -> Unit = { _ -> },
    onNavigateToLecture: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val database = (context.applicationContext as VoiceStreamApplication).database
    val draftDao = database.draftDao()
    val achievementDao = database.achievementDao()
    val lectureDao = database.lectureDao()
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Lectures state
    var lectures by remember { mutableStateOf<List<com.fouwaz.studypal.data.local.entity.LectureEntity>>(emptyList()) }

    // Load lectures
    LaunchedEffect(Unit) {
        lectureDao.getAllLectures().collect { lectureList ->
            lectures = lectureList
        }
    }

    // Category filter state - now includes Lectures tab
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Essay", "Thesis", "Research Notes", "Lectures")

    // Calculate stats from all drafts and voice streams
    var totalWords by remember { mutableStateOf(0) }
    var previousWordCount by remember { mutableStateOf(0) }
    var totalWPM by remember { mutableStateOf(0) }
    var timeSavedMinutes by remember { mutableStateOf(0) }
    var totalProjects by remember { mutableStateOf(0) }
    var essayCount by remember { mutableStateOf(0) }
    var thesisCount by remember { mutableStateOf(0) }
    var researchCount by remember { mutableStateOf(0) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // Pebble achievement state
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var newlyUnlockedPebble by remember { mutableStateOf<PebbleType?>(null) }
    var recentPebbles by remember { mutableStateOf<List<AchievementEntity>>(emptyList()) }

    // Load recent achievements on background thread
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            recentPebbles = achievementDao.getAllAchievementsSync().take(5)
        }
    }

    LaunchedEffect(projects) {
        // Run heavy computation on IO thread to keep UI smooth
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var wordCount = 0
            var streamCount = 0

            projects.forEach { project ->
                // Count words from voice stream transcriptions (chat mode)
                try {
                    val streams = database.voiceStreamDao().getStreamsByProjectSync(project.id)
                    streams.forEach { stream ->
                        // Count words from transcribed text
                        val transcribedWords = stream.transcribedText.split("\\s+".toRegex())
                            .filter { word -> word.isNotBlank() }.size
                        wordCount += transcribedWords
                    }
                    streamCount += streams.size
                } catch (e: Exception) {
                    // If streams can't be loaded, continue
                }

                // Count words from drafts (generated content)
                val draft = draftDao.getCurrentDraft(project.id)
                draft?.let {
                    wordCount += it.content.split("\\s+".toRegex()).filter { word -> word.isNotBlank() }.size
                }
            }

            val calculatedProjects = projects.size
            val calculatedEssayCount = projects.count { it.tags.contains("Essay") }
            val calculatedThesisCount = projects.count { it.tags.contains("Thesis") }
            val calculatedResearchCount = projects.count { it.tags.contains("Research Notes") }

            // Calculate WPM (assuming average speaking is ~150 WPM)
            val calculatedWPM = if (streamCount > 0) {
                // Estimate ~30 seconds per stream (average speaking time)
                val totalDurationMs = streamCount * 30000L
                val totalMinutes = totalDurationMs / 60000.0
                if (totalMinutes > 0) (wordCount / totalMinutes).toInt() else 150
            } else {
                0
            }

            // Calculate time saved (speaking vs typing)
            // Average typing speed: 40 WPM, Average speaking speed: 150 WPM
            // Time saved = (words / 40 WPM) - (words / 150 WPM)
            val calculatedTimeSaved = if (wordCount > 0) {
                val typingTime = wordCount / 40.0 // minutes
                val speakingTime = wordCount / 150.0 // minutes
                (typingTime - speakingTime).toInt()
            } else {
                0
            }

            // Switch back to main thread to update UI state
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                totalWords = wordCount
                totalProjects = calculatedProjects
                essayCount = calculatedEssayCount
                thesisCount = calculatedThesisCount
                researchCount = calculatedResearchCount
                totalWPM = calculatedWPM
                timeSavedMinutes = calculatedTimeSaved
                isDataLoaded = true

                // Check for new milestone achievements
                PebbleTypes.ALL_PEBBLES.forEach { pebble ->
                    val milestoneType = "words_${pebble.wordMilestone}"

                    // Check if we just crossed this milestone
                    if (wordCount >= pebble.wordMilestone && previousWordCount < pebble.wordMilestone) {
                        scope.launch {
                            // Check if not already unlocked
                            val alreadyUnlocked = achievementDao.isMilestoneUnlocked(milestoneType)

                            if (!alreadyUnlocked) {
                                // Unlock this pebble!
                                val achievement = AchievementEntity(
                                    milestoneType = milestoneType,
                                    pebbleType = pebble.id,
                                    unlockedAt = System.currentTimeMillis(),
                                    isNew = true
                                )
                                achievementDao.insertAchievement(achievement)

                                // Show celebration
                                newlyUnlockedPebble = pebble
                                showCelebrationDialog = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                // Reload recent pebbles
                                recentPebbles = achievementDao.getAllAchievementsSync().take(5)
                            }
                        }
                    }
                }

                // Update previous word count for next check
                previousWordCount = wordCount
            }
        }
    }

    // Filter projects by category
    val filteredProjects = remember(projects, selectedCategory) {
        if (selectedCategory == "All") {
            projects
        } else {
            projects.filter { it.tags.contains(selectedCategory) }
        }
    }

    // Breathing animation for FAB
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        containerColor = Color(0xFFFFFCF9)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // App Name with mascot
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.only_pebbl),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Pebbl",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF000000)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Filter Tabs
                CategoryFilterTabs(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                // Project List or Lecture List based on selected tab
                when {
                    selectedCategory == "Lectures" -> {
                        // Show lectures list only
                        if (lectures.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "No lectures yet",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF2A2A37)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tap + then the lecture icon to record",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            LectureGridList(
                                lectures = lectures,
                                onLectureClick = { onNavigateToLecture(it) }
                            )
                        }
                    }

                    selectedCategory == "All" -> {
                        // Show both projects and lectures when "All" is selected
                        if (projects.isEmpty() && lectures.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "No content yet",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF2A2A37)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tap + to create your first project or lecture",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            CombinedList(
                                projects = projects,
                                lectures = lectures,
                                onProjectClick = { onNavigateToSession(it) },
                                onLectureClick = { onNavigateToLecture(it) },
                                onProjectDelete = { projectId ->
                                    viewModel.deleteProject(projectId)
                                },
                                onNavigateToLectureRecording = onNavigateToLectureRecording
                            )
                        }
                    }

                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    filteredProjects.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No $selectedCategory projects",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF2A2A37)
                            )
                        }
                    }

                    else -> {
                        ProjectGridList(
                            projects = filteredProjects,
                            onProjectClick = { onNavigateToSession(it) },
                            onProjectDelete = { projectId ->
                                viewModel.deleteProject(projectId)
                            },
                            onNavigateToLectureRecording = onNavigateToLectureRecording
                        )
                    }
                }
            }

            // Floating Action Button with options
            FABWithOptions(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 24.dp),
                fabScale = fabScale,
                selectedCategory = selectedCategory,
                onNavigateToNewProject = onNavigateToNewProject,
                onNavigateToLectureRecording = onNavigateToLectureRecording
            )
        }
    }

    // Show celebration dialog when pebble is unlocked
    if (showCelebrationDialog && newlyUnlockedPebble != null) {
        MilestoneCelebrationDialog(
            pebble = newlyUnlockedPebble!!,
            onDismiss = {
                showCelebrationDialog = false
                newlyUnlockedPebble = null
            }
        )
    }
}
// End of ProjectListScreen

// Unified Stats Card - Single card design inspired by reference image
@Composable
private fun StatsCardsSection(
    totalWords: Int,
    totalProjects: Int,
    timeSavedMinutes: Int,
    essayCount: Int,
    thesisCount: Int,
    researchCount: Int,
    isDataLoaded: Boolean,
    recentPebbles: List<AchievementEntity>,
    onNavigateToPebbleCollection: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = (context.applicationContext as VoiceStreamApplication).database
    val achievementDao = database.achievementDao()

    var unlockedPebbleIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load unlocked pebbles
    LaunchedEffect(recentPebbles) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val achievements = achievementDao.getAllAchievementsSync()
            unlockedPebbleIds = achievements.map { it.pebbleType }.toSet()
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isDataLoaded) {
        if (isDataLoaded) {
            isVisible = true
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(250, easing = LinearOutSlowInEasing)
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = tween(250, easing = LinearOutSlowInEasing)
        ) { it / 6 }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8F8F8),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top section with stats - Left/Right layout with divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Total Projects with vertical divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = totalProjects.toString(),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF000000)
                            )
                            Text(
                                text = "projects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            )
                        }

                        // Vertical divider line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(60.dp)
                                .background(Color(0xFF2A2A37).copy(alpha = 0.2f))
                        )
                    }

                    // Right side - Three stats in a horizontal row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Words written
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = totalWords.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF000000)
                            )
                            Text(
                                text = "words",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            )
                        }

                        // Time saved
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${timeSavedMinutes}m",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF000000)
                            )
                            Text(
                                text = "saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            )
                        }

                        // Pebbles collected
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = recentPebbles.size.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF000000)
                            )
                            Text(
                                text = "pebbles",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Bottom section - Pebbles collection preview with milestones (reduced height)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPebbleCollection() },
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color(0xFFE9DED9),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Show pebbles with milestones (unlocked + next locked)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Show first 4 pebbles (mix of unlocked and locked)
                            PebbleTypes.ALL_PEBBLES.take(4).forEach { pebble ->
                                val isUnlocked = unlockedPebbleIds.contains(pebble.id)
                                CompactPebbleItem(
                                    pebble = pebble,
                                    isUnlocked = isUnlocked
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "View collection",
                            tint = Color(0xFF000000),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer(rotationZ = -90f) // Rotate to make it point right (>)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FABWithOptions(
    modifier: Modifier = Modifier,
    fabScale: Float,
    selectedCategory: String,
    onNavigateToNewProject: () -> Unit,
    onNavigateToLectureRecording: (Long?) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "fab_rotation"
    )

    Box(modifier = modifier) {
        // Only show options when not on Lectures tab (on Lectures tab, + goes directly to recording)
        if (selectedCategory != "Lectures") {
            // Essay Mode FAB (appears when expanded)
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideOutVertically(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                )
            ) {
                FloatingActionButton(
                    onClick = {
                        isExpanded = false
                        onNavigateToNewProject()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-80).dp),
                    containerColor = Color(0xFFE9DED9),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Essay mode",
                        tint = Color(0xFF000000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Lecture Mode FAB (appears when expanded)
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(durationMillis = 250, delayMillis = 50, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = tween(durationMillis = 250, delayMillis = 50, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideOutVertically(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                )
            ) {
                FloatingActionButton(
                    onClick = {
                        isExpanded = false
                        onNavigateToLectureRecording(null)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-140).dp),
                    containerColor = Color(0xFFE9DED9),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Lecture mode",
                        tint = Color(0xFF000000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = {
                if (selectedCategory == "Lectures") {
                    // On Lectures tab, + goes directly to recording
                    onNavigateToLectureRecording(null)
                } else {
                    // On other tabs, toggle the menu
                    isExpanded = !isExpanded
                }
            },
            modifier = Modifier
                .size(56.dp)
                .scale(if (isExpanded) 1f else fabScale)
                .graphicsLayer {
                    rotationZ = if (selectedCategory == "Lectures") 0f else rotation
                },
            containerColor = Color(0xFFE9DED9),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (selectedCategory == "Lectures") Icons.Default.School else Icons.Default.Add,
                contentDescription = if (selectedCategory == "Lectures") "Record lecture" else (if (isExpanded) "Close" else "Create"),
                tint = Color(0xFF000000),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CompactPebbleItem(
    pebble: PebbleType,
    isUnlocked: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Pebble icon (smaller size)
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(pebble.color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🪨",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2A2A37).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Word milestone
        Text(
            text = "${pebble.wordMilestone / 1000}k",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (isUnlocked) Color(0xFF000000) else Color(0xFF2A2A37).copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun UnifiedStatItem(
    value: String,
    label: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CompactStatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF000000)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
        )
    }
}

// Category Filter Tabs - with horizontal scroll
@Composable
private fun CategoryFilterTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFFE9DED9) else Color(0xFFE9DED9).copy(alpha = 0.3f),
                onClick = { onCategorySelected(category) },
                shadowElevation = 0.dp
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color(0xFF000000) else Color(0xFF2A2A37).copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// Project Grid List
@Composable
private fun ProjectGridList(
    projects: List<Project>,
    onProjectClick: (Long) -> Unit,
    onProjectDelete: (Long) -> Unit,
    onNavigateToLectureRecording: (Long?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(projects) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project.id) },
                onDelete = { onProjectDelete(project.id) },
                onRecordLecture = { onNavigateToLectureRecording(project.id) }
            )
        }
    }

}
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRecordLecture: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = remember(density) { with(density) { 100.dp.toPx() } }
    val coroutineScope = rememberCoroutineScope()
    var offset by remember { mutableStateOf(0f) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    val revealFraction by remember {
        derivedStateOf {
            if (actionWidthPx == 0f) 0f else (-offset / actionWidthPx).coerceIn(0f, 1f)
        }
    }

    val deleteAlpha by animateFloatAsState(
        targetValue = revealFraction,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "delete_alpha"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .height(100.dp)
                    .width(72.dp)
                    .graphicsLayer { alpha = deleteAlpha }
                    .clickable(
                        enabled = revealFraction > 0.5f,
                        onClick = {
                            onDelete()
                            animationJob?.cancel()
                            offset = 0f
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 0.dp,
                shadowElevation = 4.dp,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete project",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset { IntOffset(offset.roundToInt(), 0) }
                .pointerInput(actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { animationJob?.cancel() },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offset + dragAmount).coerceIn(-actionWidthPx, 0f)
                            if (newOffset != offset) {
                                offset = newOffset
                            }
                        },
                        onDragEnd = {
                            val target = if (-offset > actionWidthPx / 2f) -actionWidthPx else 0f
                            animationJob = coroutineScope.launch {
                                animate(
                                    initialValue = offset,
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 220,
                                        easing = FastOutSlowInEasing
                                    )
                                ) { value, _ ->
                                    offset = value
                                }
                            }
                        },
                        onDragCancel = {
                            animationJob = coroutineScope.launch {
                                animate(
                                    initialValue = offset,
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 220,
                                        easing = FastOutSlowInEasing
                                    )
                                ) { value, _ ->
                                    offset = value
                                }
                            }
                        }
                    )
                }
                .clickable {
                    if (offset != 0f) {
                        animationJob?.cancel()
                        animationJob = coroutineScope.launch {
                            animate(
                                initialValue = offset,
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 220,
                                    easing = FastOutSlowInEasing
                                )
                            ) { value, _ ->
                                offset = value
                            }
                        }
                    } else {
                        onClick()
                    }
                },
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8F8F8)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2A2A37),
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (project.tags.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE9DED9).copy(alpha = 0.7f),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = project.tags.first(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF000000),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = formatDate(project.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                        )
                    }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFE9DED9),
                    shadowElevation = 2.dp
                ) {
                    IconButton(
                        onClick = {
                            if (offset != 0f) {
                                animationJob?.cancel()
                                animationJob = coroutineScope.launch {
                                    animate(
                                        initialValue = offset,
                                        targetValue = 0f,
                                        animationSpec = tween(
                                            durationMillis = 220,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) { value, _ ->
                                        offset = value
                                    }
                                }
                            }
                            onRecordLecture()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record lecture",
                            tint = Color(0xFF000000)
                        )
                    }
                }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

// Combined List (Projects + Lectures) for "All" tab
@Composable
private fun CombinedList(
    projects: List<Project>,
    lectures: List<com.fouwaz.studypal.data.local.entity.LectureEntity>,
    onProjectClick: (Long) -> Unit,
    onLectureClick: (Long) -> Unit,
    onProjectDelete: (Long) -> Unit,
    onNavigateToLectureRecording: (Long?) -> Unit
) {
    // Create a combined list with type indicators, sorted by date
    val combinedItems = remember(projects, lectures) {
        val projectItems = projects.map { project ->
            CombinedItem.ProjectItem(project)
        }
        val lectureItems = lectures.map { lecture ->
            CombinedItem.LectureItem(lecture)
        }
        (projectItems + lectureItems).sortedByDescending { item ->
            when (item) {
                is CombinedItem.ProjectItem -> item.project.updatedAt
                is CombinedItem.LectureItem -> item.lecture.updatedAt
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(combinedItems.size) { index ->
            when (val item = combinedItems[index]) {
                is CombinedItem.ProjectItem -> {
                    ProjectCard(
                        project = item.project,
                        onClick = { onProjectClick(item.project.id) },
                        onDelete = { onProjectDelete(item.project.id) },
                        onRecordLecture = { onNavigateToLectureRecording(item.project.id) }
                    )
                }
                is CombinedItem.LectureItem -> {
                    LectureCard(
                        lecture = item.lecture,
                        onClick = { onLectureClick(item.lecture.id) }
                    )
                }
            }
        }
    }
}

// Sealed class to represent either a Project or Lecture
private sealed class CombinedItem {
    data class ProjectItem(val project: Project) : CombinedItem()
    data class LectureItem(val lecture: com.fouwaz.studypal.data.local.entity.LectureEntity) : CombinedItem()
}

// Lecture Grid List
@Composable
private fun LectureGridList(
    lectures: List<com.fouwaz.studypal.data.local.entity.LectureEntity>,
    onLectureClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lectures, key = { it.id }) { lecture ->
            LectureCard(
                lecture = lecture,
                onClick = { onLectureClick(lecture.id) }
            )
        }
    }
}

@Composable
private fun LectureCard(
    lecture: com.fouwaz.studypal.data.local.entity.LectureEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8F8F8)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lecture.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2A2A37),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Duration:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatLectureDuration(lecture.durationSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF000000)
                        )
                    }

                    // Words chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Words:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                        )
                        Text(
                            text = lecture.wordCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF000000)
                        )
                    }

                    // Date
                    Text(
                        text = formatDate(lecture.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatLectureDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${secs}s"
    } else {
        "${secs}s"
    }
}









