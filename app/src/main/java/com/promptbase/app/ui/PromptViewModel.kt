package com.promptbase.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.data.model.Tag
import com.promptbase.app.data.repository.PromptRepository
import com.promptbase.app.util.ExportedPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val repository: PromptRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedTag = MutableStateFlow<Tag?>(null)
    val showOnlyUntagged = MutableStateFlow(false)

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val prompts: StateFlow<List<PromptWithTags>> = combine(
        searchQuery,
        selectedTag,
        showOnlyUntagged
    ) { query, tag, onlyUntagged ->
        Triple(query, tag, onlyUntagged)
    }.flatMapLatest { (query, tag, onlyUntagged) ->
        when {
            onlyUntagged && query.isNotBlank() -> repository.searchUntaggedPrompts(query)
            onlyUntagged -> repository.getUntaggedPrompts()
            tag != null && query.isNotBlank() -> repository.searchPromptsByTag(tag.tagId, query)
            tag != null -> repository.filterPromptsByTag(tag.tagId)
            query.isNotBlank() -> repository.searchPrompts(query)
            else -> repository.allPrompts
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val trashedPrompts: StateFlow<List<PromptWithTags>> = repository.trashedPrompts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allPromptsUnfiltered: StateFlow<List<PromptWithTags>> = repository.allPrompts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
        }
    }
}
