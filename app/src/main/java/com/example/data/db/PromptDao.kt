package com.example.data.db

import androidx.room.*
import com.example.data.model.Prompt
import com.example.data.model.PromptTagCrossRef
import com.example.data.model.PromptWithTags
import com.example.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    @Transaction
    @Query("SELECT * FROM prompts WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllPromptsWithTags(): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM prompts 
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') 
        AND isArchived = 0
        ORDER BY updatedAt DESC
    """)
    fun searchPromptsWithTags(query: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT p.* FROM prompts p
        INNER JOIN prompt_tag_cross_ref ref ON p.id = ref.promptId
        WHERE ref.tagId = :tagId AND p.isArchived = 0
        ORDER BY p.updatedAt DESC
    """)
    fun getPromptsByTag(tagId: String): Flow<List<PromptWithTags>>

    @Query("SELECT * FROM prompts WHERE id = :id LIMIT 1")
    suspend fun getPromptById(id: String): Prompt?

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: Prompt)

    @Update
    suspend fun updatePrompt(prompt: Prompt)

    @Query("UPDATE prompts SET isArchived = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun archivePrompt(id: String, timestamp: Long)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deletePrompt(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPromptTagCrossRef(crossRef: PromptTagCrossRef)

    @Query("DELETE FROM prompt_tag_cross_ref WHERE promptId = :promptId")
    suspend fun deleteCrossRefsForPrompt(promptId: String)
}
