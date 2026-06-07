package com.promptbase.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.data.model.Tag
import com.promptbase.app.ui.*
import com.promptbase.app.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: PromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PromptBaseApp(viewModel = viewModel)
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, "home"),
    BottomNavItem("Categories", Icons.Filled.GridView, "categories"),
    BottomNavItem("Profile", Icons.Filled.Person, "profile"),
)

private val bottomBarRoutes = bottomNavItems.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptBaseApp(viewModel: PromptViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val allPromptsUnfiltered by viewModel.allPromptsUnfiltered.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val showOnlyUntagged by viewModel.showOnlyUntagged.collectAsStateWithLifecycle()
    val trashedPrompts by viewModel.trashedPrompts.collectAsStateWithLifecycle()

    var fillPromptTarget by remember { mutableStateOf<PromptWithTags?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var copyAlertMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_layout"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route ||
                            (item.route == "home" && currentRoute == null)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                ExtendedFloatingActionButton(
                    text = { Text("New Prompt", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Add New") },
                    onClick = {
                        viewModel.startNewPrompt()
                        navController.navigate("editor/new")
                    },
                    modifier = Modifier.testTag("add_prompt_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring()) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = spring()) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring()) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring()) }
            ) {
                composable("home") {
                    HomeScreen(
                        prompts = prompts,
                        tags = tags,
                        searchQuery = searchQuery,
                        selectedTag = selectedTag,
                        showOnlyUntagged = showOnlyUntagged,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onSelectTag = { tag -> viewModel.selectTag(tag) },
                        onSelectUntagged = { viewModel.selectUntagged() },
                        onFillClick = { prompt -> fillPromptTarget = prompt },
                        onEditClick = { prompt ->
                            viewModel.startEditing(prompt)
                            navController.navigate("editor/${prompt.prompt.id}")
                        },
                        onArchiveClick = { prompt ->
                            viewModel.softDeletePrompt(prompt.prompt.id)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Moved to trash",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restorePrompt(prompt.prompt.id)
                                }
                            }
                        }
                    )
                }

                composable("categories") {
                    CategoriesScreen(
                        tags = tags,
                        onCategoryClick = { tag ->
                            navController.navigate("category/${tag.tagId}")
                        },
                        onCreateCategory = { name ->
                            viewModel.addTagDirectly(name)
                        }
                    )
                }

                composable(
                    route = "category/{tagId}",
                    arguments = listOf(navArgument("tagId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tagId = backStackEntry.arguments?.getString("tagId") ?: return@composable
                    val categoryTag = tags.find { it.tagId == tagId }
                    val categoryPrompts = remember(tagId, allPromptsUnfiltered) {
                        allPromptsUnfiltered.filter { pwt ->
                            pwt.tags.any { it.tagId == tagId }
                        }
                    }
                    CategoryDetailScreen(
                        tag = categoryTag,
                        prompts = categoryPrompts,
                        onBack = { navController.popBackStack() },
                        onFillClick = { prompt -> fillPromptTarget = prompt },
                        onEditClick = { prompt ->
                            viewModel.startEditing(prompt)
                            navController.navigate("editor/${prompt.prompt.id}")
                        },
                        onArchiveClick = { prompt ->
                            viewModel.softDeletePrompt(prompt.prompt.id)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Moved to trash",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restorePrompt(prompt.prompt.id)
                                }
                            }
                        }
                    )
                }

                composable(
                    route = "editor/{promptId}",
                    arguments = listOf(navArgument("promptId") { type = NavType.StringType })
                ) {
                    val editTitle by viewModel.editTitle.collectAsStateWithLifecycle()
                    val editContent by viewModel.editContent.collectAsStateWithLifecycle()
                    val editTags by viewModel.editTags.collectAsStateWithLifecycle()
                    val editingPrompt by viewModel.editingPrompt.collectAsStateWithLifecycle()

                    EditorScreen(
                        title = editTitle,
                        onTitleChange = { viewModel.editTitle.value = it },
                        content = editContent,
                        onContentChange = { viewModel.editContent.value = it },
                        promptTags = editTags,
                        onAddTag = { viewModel.addTagToEdit(it) },
                        onRemoveTag = { viewModel.removeTagFromEdit(it) },
                        isNew = editingPrompt == null,
                        onBack = { navController.popBackStack() },
                        onSave = {
                            viewModel.saveCurrentPrompt {
                                navController.popBackStack()
                                Toast.makeText(context, "Prompt Saved Successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        prompts = allPromptsUnfiltered,
                        trashedCount = trashedPrompts.size,
                        onImportPrompts = { imported ->
                            viewModel.importPrompts(imported)
                        },
                        onNavigateToTrash = { navController.navigate("trash") },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("trash") {
                    TrashScreen(
                        trashedPrompts = trashedPrompts,
                        onRestore = { viewModel.restorePrompt(it) },
                        onPermanentDelete = { viewModel.permanentlyDeletePrompt(it) },
                        onEmptyTrash = { viewModel.emptyTrash() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = copyAlertMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = copyAlertMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                LaunchedEffect(copyAlertMessage) {
                    if (copyAlertMessage != null) {
                        kotlinx.coroutines.delay(2000)
                        copyAlertMessage = null
                    }
                }
            }

            if (fillPromptTarget != null) {
                ModalBottomSheet(
                    onDismissRequest = { fillPromptTarget = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    modifier = Modifier.testTag("magic_bottom_sheet")
                ) {
                    val target = fillPromptTarget!!
                    MagicFillSheetContent(
                        promptWithTags = target,
                        onDismiss = { fillPromptTarget = null },
                        onCopySuccess = { message ->
                            copyAlertMessage = message
                            fillPromptTarget = null
                        }
                    )
                }
            }
        }
    }
}
