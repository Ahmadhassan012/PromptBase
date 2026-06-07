package com.promptbase.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.util.VariableParser

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
