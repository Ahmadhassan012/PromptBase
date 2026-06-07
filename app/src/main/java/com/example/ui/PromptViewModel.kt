package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Prompt
import com.example.data.model.PromptWithTags
import com.example.data.model.Tag
import com.example.data.repository.PromptRepository
import com.example.util.ExportedPrompt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PromptViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PromptRepository(db.promptDao())

    val searchQuery = MutableStateFlow("")
    val selectedTag = MutableStateFlow<Tag?>(null)
    val showOnlyUntagged = MutableStateFlow(false)

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val prompts: StateFlow<List<PromptWithTags>> = combine(
        repository.allPrompts,
        searchQuery,
        selectedTag,
        showOnlyUntagged
    ) { list, query, tag, onlyUntagged ->
        var filtered = list
        if (onlyUntagged) {
            filtered = filtered.filter { it.tags.isEmpty() }
        } else if (tag != null) {
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

    val trashedPrompts: StateFlow<List<PromptWithTags>> = repository.trashedPrompts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _editingPrompt = MutableStateFlow<PromptWithTags?>(null)
    val editingPrompt: StateFlow<PromptWithTags?> = _editingPrompt.asStateFlow()

    val editTitle = MutableStateFlow("")
    val editContent = MutableStateFlow("")
    val editTags = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch {
            repository.deleteExpiredPrompts()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectTag(tag: Tag?) {
        selectedTag.value = tag
        showOnlyUntagged.value = false
    }

    fun selectUntagged() {
        showOnlyUntagged.value = true
        selectedTag.value = null
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

    fun softDeletePrompt(id: String) {
        viewModelScope.launch {
            repository.softDeletePrompt(id)
        }
    }

    fun restorePrompt(id: String) {
        viewModelScope.launch {
            repository.restorePrompt(id)
        }
    }

    fun permanentlyDeletePrompt(id: String) {
        viewModelScope.launch {
            repository.permanentlyDeletePrompt(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun importPrompts(imported: List<ExportedPrompt>) {
        viewModelScope.launch {
            repository.importPrompts(imported)
        }
    }

    fun addTagDirectly(tagName: String) {
        viewModelScope.launch {
            repository.createTag(tagName)
        }
    }
}
