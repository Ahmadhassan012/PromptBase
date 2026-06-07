package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PromptWithTags
import com.example.data.model.Tag
import com.example.ui.PromptViewModel
import com.example.ui.ProfileScreen
import com.example.ui.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.VariableParser
import kotlinx.coroutines.launch

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

sealed interface ScreenState {
    data object Home : ScreenState
    data object Editor : ScreenState
    data object Profile : ScreenState
    data object Trash : ScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptBaseApp(viewModel: PromptViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Home) }

    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val showOnlyUntagged by viewModel.showOnlyUntagged.collectAsStateWithLifecycle()
    val trashedPrompts by viewModel.trashedPrompts.collectAsStateWithLifecycle()

    var fillPromptTarget by remember { mutableStateOf<PromptWithTags?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var copyAlertMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_layout"),
        floatingActionButton = {
            if (currentScreen == ScreenState.Home) {
                ExtendedFloatingActionButton(
                    text = { Text("New Prompt", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Add New") },
                    onClick = {
                        viewModel.startNewPrompt()
                        currentScreen = ScreenState.Editor
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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val direction = when {
                        targetState is ScreenState.Editor && initialState is ScreenState.Home -> 1
                        targetState is ScreenState.Profile && initialState is ScreenState.Home -> 1
                        targetState is ScreenState.Trash && initialState is ScreenState.Profile -> 1
                        targetState is ScreenState.Home -> -1
                        else -> -1
                    }
                    slideInHorizontally(initialOffsetX = { if (direction > 0) it else -it }, animationSpec = spring()) togetherWith
                    slideOutHorizontally(targetOffsetX = { if (direction > 0) -it else it }, animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is ScreenState.Home -> {
                        HomeScreen(
                            prompts = prompts,
                            tags = tags,
                            searchQuery = searchQuery,
                            selectedTag = selectedTag,
                            showOnlyUntagged = showOnlyUntagged,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSelectTag = { tag ->
                                viewModel.selectTag(tag)
                            },
                            onSelectUntagged = { viewModel.selectUntagged() },
                            onProfileClick = { currentScreen = ScreenState.Profile },
                            onFillClick = { prompt -> fillPromptTarget = prompt },
                            onEditClick = { prompt ->
                                viewModel.startEditing(prompt)
                                currentScreen = ScreenState.Editor
                            },
                            onArchiveClick = { prompt ->
                                viewModel.softDeletePrompt(prompt.prompt.id)
                                Toast.makeText(context, "Moved to trash", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    is ScreenState.Editor -> {
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
                            onBack = { currentScreen = ScreenState.Home },
                            onSave = {
                                viewModel.saveCurrentPrompt {
                                    currentScreen = ScreenState.Home
                                    Toast.makeText(context, "Prompt Saved Successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    is ScreenState.Profile -> {
                        ProfileScreen(
                            prompts = prompts,
                            trashedCount = trashedPrompts.size,
                            onImportPrompts = { imported ->
                                viewModel.importPrompts(imported)
                            },
                            onNavigateToTrash = { currentScreen = ScreenState.Trash },
                            onBack = { currentScreen = ScreenState.Home }
                        )
                    }
                    is ScreenState.Trash -> {
                        TrashScreen(
                            trashedPrompts = trashedPrompts,
                            onRestore = { viewModel.restorePrompt(it) },
                            onPermanentDelete = { viewModel.permanentlyDeletePrompt(it) },
                            onEmptyTrash = { viewModel.emptyTrash() },
                            onBack = { currentScreen = ScreenState.Profile }
                        )
                    }
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

@Composable
fun HomeScreen(
    prompts: List<PromptWithTags>,
    tags: List<Tag>,
    searchQuery: String,
    selectedTag: Tag?,
    showOnlyUntagged: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectTag: (Tag?) -> Unit,
    onSelectUntagged: () -> Unit,
    onProfileClick: () -> Unit,
    onFillClick: (PromptWithTags) -> Unit,
    onEditClick: (PromptWithTags) -> Unit,
    onArchiveClick: (PromptWithTags) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PromptBase",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your prompt library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onProfileClick) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_prompt_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTag == null && !showOnlyUntagged,
                onClick = { onSelectTag(null) },
                label = { Text("All") },
                leadingIcon = if (selectedTag == null && !showOnlyUntagged) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )

            tags.forEach { tag ->
                val isSelected = selectedTag?.tagId == tag.tagId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTag(if (isSelected) null else tag) },
                    label = { Text(tag.name) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        { Icon(Icons.Rounded.Label, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FilterChip(
                selected = showOnlyUntagged,
                onClick = { onSelectUntagged() },
                label = { Text("Others") },
                leadingIcon = if (showOnlyUntagged) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    { Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp)) }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (prompts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = "Empty icon",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found" else "No Prompt Templates Saved",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try refining your keywords or clear active tag filters." else "Click the '+' button below to save your very first dynamic template using {{variable}} placeholders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onQueryChange("")
                                onSelectTag(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(prompts, key = { it.prompt.id }) { promptWithTags ->
                    PromptCard(
                        promptWithTags = promptWithTags,
                        onFillClick = { onFillClick(promptWithTags) },
                        onEditClick = { onEditClick(promptWithTags) },
                        onArchiveClick = { onArchiveClick(promptWithTags) }
                    )
                }
            }
        }
    }
}

@Composable
fun PromptCard(
    promptWithTags: PromptWithTags,
    onFillClick: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit
) {
    val prompt = promptWithTags.prompt
    val variables = remember(prompt.content) { VariableParser.extractVariables(prompt.content) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFillClick() }
            .testTag("prompt_card_${prompt.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EditNote,
                            contentDescription = "Edit Prompt",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete/Archive Prompt",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = prompt.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    promptWithTags.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (variables.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { onFillClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (variables.isNotEmpty()) Icons.Rounded.AutoAwesome else Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = if (variables.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (variables.isNotEmpty()) "${variables.size} fields" else "Use",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (variables.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    promptTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    var tagInput by remember { mutableStateOf("") }
    val parsedVariables = remember(content) { VariableParser.extractVariables(content) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Go back")
            }
            Text(
                text = if (isNew) "Create Prompt" else "Edit Prompt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onSave,
                enabled = title.isNotBlank() && content.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            placeholder = { Text("e.g. Code Reviewer, Creative Writing Assistant...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_title_input"),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text("Prompt Template") },
            placeholder = { Text("Write your prompt. Add dynamic blanks using {{ variable }} or {{ variable : default }} formatting.") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .testTag("prompt_content_input"),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = "Help Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dynamic Templating Helper",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Insert custom template variables inside curly brackets to easily swap values later. Try copying this template variable code standard:\n" +
                            "• {{ recipient_name }} for basic inputs.\n" +
                            "• {{ tone : enthusiastic }} to define a default value.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (parsedVariables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Detected Variables (${parsedVariables.size}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                parsedVariables.forEach { variable ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = variable.displayName + (variable.defaultValue?.let { " (default: $it)" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Categories / Tags:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            promptTags.forEach { t ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveTag(t) },
                    label = { Text(t) },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Cancel,
                            contentDescription = "Remove tag",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            placeholder = { Text("Add custom tag (e.g. Design, Personal...)") },
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onAddTag(tagInput.trim())
                            tagInput = ""
                        }
                    },
                    enabled = tagInput.isNotBlank()
                ) {
                    Icon(Icons.Rounded.AddCircle, contentDescription = "Add category tag")
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (tagInput.isNotBlank()) {
                        onAddTag(tagInput.trim())
                        tagInput = ""
                    }
                }
            )
        )
    }
}

@Composable
fun MagicFillSheetContent(
    promptWithTags: PromptWithTags,
    onDismiss: () -> Unit,
    onCopySuccess: (String) -> Unit
) {
    val prompt = promptWithTags.prompt
    val variables = remember(prompt.content) { VariableParser.extractVariables(prompt.content) }

    val inputValues = remember {
        mutableStateMapOf<String, String>().apply {
            variables.forEach { v ->
                put(v.key, v.defaultValue ?: "")
            }
        }
    }

    val resolvedPrompt = remember(prompt.content, inputValues) {
        derivedStateOf {
            VariableParser.replaceVariables(prompt.content, inputValues)
        }
    }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fill Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel fill")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (variables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "No dynamic variables detected. You can copy this static template instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                variables.forEach { variable ->
                    OutlinedTextField(
                        value = inputValues[variable.key] ?: "",
                        onValueChange = { inputValues[variable.key] = it },
                        label = { Text(variable.displayName) },
                        placeholder = {
                            Text(
                                text = variable.defaultValue?.let { "Default: $it" } ?: "Enter details..."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fill_input_${variable.key}"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Live Resulting Preview:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MarkdownRenderer(
                    content = resolvedPrompt.value,
                    variableColor = MaterialTheme.colorScheme.primaryContainer,
                    onVariableColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val finalContent = resolvedPrompt.value
                clipboardManager.setText(AnnotatedString(finalContent))
                onCopySuccess("Prompt copied seamlessly!")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("copy_resolved_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy text")
                Text(
                    text = "Copy Resolved Prompt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class CodeBlock(val text: String, val language: String?) : MarkdownBlock()
    data class BulletPoint(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

fun parseMarkdownToBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var insideCodeBlock = false
    val currentCodeLines = mutableListOf<String>()
    var currentCodeLang: String? = null

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (insideCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.joinToString("\n"), currentCodeLang))
                currentCodeLines.clear()
                currentCodeLang = null
                insideCodeBlock = false
            } else {
                val lang = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                currentCodeLang = lang
                insideCodeBlock = true
            }
            continue
        }

        if (insideCodeBlock) {
            currentCodeLines.add(line)
            continue
        }

        when {
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("# ").trim(), 1))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("## ").trim(), 2))
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("### ").trim(), 3))
            }
            trimmed.startsWith("- ") -> {
                blocks.add(MarkdownBlock.BulletPoint(trimmed.removePrefix("- ").trim()))
            }
            trimmed.startsWith("* ") -> {
                blocks.add(MarkdownBlock.BulletPoint(trimmed.removePrefix("* ").trim()))
            }
            else -> {
                if (line.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
        }
    }

    if (insideCodeBlock && currentCodeLines.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.joinToString("\n"), currentCodeLang))
    }

    return blocks
}

@Composable
fun MarkdownRenderer(
    content: String,
    variableColor: Color,
    onVariableColor: Color
) {
    val blocks = remember(content) { parseMarkdownToBlocks(content) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    Text(
                        text = block.text,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            fontFamily = FontFamily.SansSerif
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            block.language?.let { lang ->
                                Text(
                                    text = lang.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = block.text,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is MarkdownBlock.BulletPoint -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = renderAnnotatedText(block.text, variableColor, onVariableColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderAnnotatedText(block.text, variableColor, onVariableColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun renderAnnotatedText(
    text: String,
    variableColor: Color,
    onVariableColor: Color
): AnnotatedString {
    val variableRegex = remember { """\{\{\s*([^:\}]+?)\s*(?::\s*([^:\}]+?)\s*)?\}\}""".toRegex() }
    val builder = RememberAnnotatedStringBuilder(text, variableRegex, variableColor, onVariableColor)
    return builder
}

@Composable
fun RememberAnnotatedStringBuilder(
    text: String,
    regex: Regex,
    variableColor: Color,
    onVariableColor: Color
): AnnotatedString {
    return remember(text, variableColor, onVariableColor) {
        val builder = AnnotatedString.Builder()
        var lastIndex = 0

        for (match in regex.findAll(text)) {
            if (match.range.first > lastIndex) {
                builder.append(text.substring(lastIndex, match.range.first))
            }

            val originalToken = match.value
            val start = builder.length
            builder.append(originalToken)
            val end = builder.length

            builder.addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    background = variableColor,
                    color = onVariableColor
                ),
                start = start,
                end = end
            )

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }

        builder.toAnnotatedString()
    }
}
