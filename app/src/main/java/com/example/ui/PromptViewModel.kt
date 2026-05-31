package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Prompt
import com.example.data.model.PromptWithTags
import com.example.data.model.Tag
import com.example.data.repository.PromptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PromptViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PromptRepository(db.promptDao())

    // Search and filter parameters
    val searchQuery = MutableStateFlow("")
    val selectedTag = MutableStateFlow<Tag?>(null)

    // Tag list observed from Database
    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactively filtered Prompt list with single source of truth
    val prompts: StateFlow<List<PromptWithTags>> = combine(
        repository.allPrompts,
        searchQuery,
        selectedTag
    ) { list, query, tag ->
        var filtered = list
        if (tag != null) {
            filtered = filtered.filter { item ->
                item.tags.any { t -> t.tagId == tag.tagId }
            }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.prompt.title.contains(query, ignoreCase = true) ||
                item.prompt.content.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state for editing or viewing a detailed Prompt
    private val _editingPrompt = MutableStateFlow<PromptWithTags?>(null)
    val editingPrompt: StateFlow<PromptWithTags?> = _editingPrompt.asStateFlow()

    // Temporary form states for adding/editing a prompt
    val editTitle = MutableStateFlow("")
    val editContent = MutableStateFlow("")
    val editTags = MutableStateFlow<List<String>>(emptyList()) // List of Tag names

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectTag(tag: Tag?) {
        selectedTag.value = tag
    }

    fun startEditing(promptWithTags: PromptWithTags) {
        _editingPrompt.value = promptWithTags
        editTitle.value = promptWithTags.prompt.title
        editContent.value = promptWithTags.prompt.content
        editTags.value = promptWithTags.tags.map { it.name }
    }

    fun startNewPrompt() {
        _editingPrompt.value = null
        editTitle.value = ""
        editContent.value = ""
        editTags.value = emptyList()
    }

    fun addTagToEdit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = editTags.value.toMutableList()
        if (!current.any { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            editTags.value = current
        }
    }

    fun removeTagFromEdit(name: String) {
        val current = editTags.value.toMutableList()
        current.removeAll { it.equals(name, ignoreCase = true) }
        editTags.value = current
    }

    fun saveCurrentPrompt(onSuccess: () -> Unit) {
        val title = editTitle.value.trim()
        val content = editContent.value.trim()
        if (title.isEmpty() || content.isEmpty()) return

        val promptId = _editingPrompt.value?.prompt?.id

        viewModelScope.launch {
            repository.savePrompt(
                promptId = promptId,
                title = title,
                content = content,
                tagNames = editTags.value
            )
            onSuccess()
        }
    }

    fun archivePrompt(id: String) {
        viewModelScope.launch {
            repository.archivePrompt(id)
        }
    }

    fun deletePrompt(id: String) {
        viewModelScope.launch {
            repository.deletePrompt(id)
        }
    }

    fun addTagDirectly(tagName: String) {
        viewModelScope.launch {
            repository.createTag(tagName)
        }
    }
}
