package com.example.data.repository

import com.example.data.db.PromptDao
import com.example.data.model.Prompt
import com.example.data.model.PromptTagCrossRef
import com.example.data.model.PromptWithTags
import com.example.data.model.Tag
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PromptRepository(private val promptDao: PromptDao) {

    val allPrompts: Flow<List<PromptWithTags>> = promptDao.getAllPromptsWithTags()
    val allTags: Flow<List<Tag>> = promptDao.getAllTags()

    fun searchPrompts(query: String): Flow<List<PromptWithTags>> {
        return if (query.isBlank()) {
            promptDao.getAllPromptsWithTags()
        } else {
            promptDao.searchPromptsWithTags(query)
        }
    }

    fun filterPromptsByTag(tagId: String): Flow<List<PromptWithTags>> {
        return promptDao.getPromptsByTag(tagId)
    }

    suspend fun savePrompt(promptId: String?, title: String, content: String, tagNames: List<String>) {
        val id = promptId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val existing = if (promptId != null) promptDao.getPromptById(promptId) else null
        val prompt = Prompt(
            id = id,
            title = title.trim(),
            content = content.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isArchived = existing?.isArchived ?: false,
            syncStatus = 1 // Mark dirty for sync compatibility
        )

        promptDao.insertPrompt(prompt)

        // Clear existing tags and link new ones
        promptDao.deleteCrossRefsForPrompt(id)
        for (name in tagNames) {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) continue
            
            var tag = promptDao.getTagByName(trimmedName)
            if (tag == null) {
                tag = Tag(name = trimmedName)
                promptDao.insertTag(tag)
            }
            promptDao.insertPromptTagCrossRef(PromptTagCrossRef(id, tag.tagId))
        }
    }

    suspend fun archivePrompt(id: String) {
        promptDao.archivePrompt(id, System.currentTimeMillis())
    }

    suspend fun deletePrompt(id: String) {
        promptDao.deletePrompt(id)
        promptDao.deleteCrossRefsForPrompt(id)
    }

    suspend fun createTag(name: String): Tag? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = promptDao.getTagByName(trimmed)
        if (existing != null) return existing
        val newTag = Tag(name = trimmed)
        promptDao.insertTag(newTag)
        return newTag
    }
}
