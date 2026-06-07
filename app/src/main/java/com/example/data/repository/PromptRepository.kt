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
    val trashedPrompts: Flow<List<PromptWithTags>> = promptDao.getTrashedPromptsWithTags()

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
            deletedAt = null,
            syncStatus = 1
        )

        promptDao.insertPrompt(prompt)
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

    suspend fun softDeletePrompt(id: String) {
        promptDao.softDeletePrompt(id, System.currentTimeMillis())
    }

    suspend fun restorePrompt(id: String) {
        promptDao.restorePrompt(id, System.currentTimeMillis())
    }

    suspend fun permanentlyDeletePrompt(id: String) {
        promptDao.deletePrompt(id)
        promptDao.deleteCrossRefsForPrompt(id)
    }

    suspend fun emptyTrash() {
        promptDao.emptyTrash()
    }

    suspend fun deleteExpiredPrompts() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        promptDao.deleteExpiredPrompts(cutoff)
    }

    suspend fun importPrompts(imported: List<com.example.util.ExportedPrompt>) {
        val existing = promptDao.getAllPromptsOnce()
        val existingKeys = existing.map { "${it.prompt.title}|${it.prompt.content}" }.toSet()
        for (ep in imported) {
            val key = "${ep.title}|${ep.content}"
            if (key in existingKeys) continue
            val id = UUID.randomUUID().toString()
            val prompt = Prompt(
                id = id,
                title = ep.title,
                content = ep.content,
                createdAt = ep.createdAt,
                updatedAt = ep.updatedAt
            )
            promptDao.insertPrompt(prompt)
            for (tagName in ep.tags) {
                var tag = promptDao.getTagByName(tagName)
                if (tag == null) {
                    tag = Tag(name = tagName)
                    promptDao.insertTag(tag)
                }
                promptDao.insertPromptTagCrossRef(PromptTagCrossRef(id, tag.tagId))
            }
        }
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
