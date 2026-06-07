package com.promptbase.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.data.model.Tag

@OptIn(ExperimentalMaterial3Api::class)
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
    onFillClick: (PromptWithTags) -> Unit,
    onEditClick: (PromptWithTags) -> Unit,
    onArchiveClick: (PromptWithTags) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "PromptBase",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.primary
            )
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search prompts...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found" else "Your Prompt Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try refining your keywords or clearing filters."
                        else "Tap + to create your first prompt template with {{variable}} placeholders.\nOr browse Categories for pre-built templates.",
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                        onArchiveClick = { onArchiveClick(promptWithTags) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
