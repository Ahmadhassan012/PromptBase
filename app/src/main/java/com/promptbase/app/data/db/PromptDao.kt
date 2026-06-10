package com.promptbase.app.data.db

import androidx.room.*
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptTagCrossRef
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    @Transaction
    @Query("SELECT * FROM prompts WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllPromptsWithTags(): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM prompts 
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') 
        AND deletedAt IS NULL
        ORDER BY updatedAt DESC
    """)
    fun searchPromptsWithTags(query: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT p.* FROM prompts p
        INNER JOIN prompt_tag_cross_ref ref ON p.id = ref.promptId
        WHERE ref.tagId = :tagId AND p.deletedAt IS NULL
        ORDER BY p.updatedAt DESC
    """)
    fun getPromptsByTag(tagId: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM prompts
        WHERE id NOT IN (SELECT DISTINCT promptId FROM prompt_tag_cross_ref)
        AND deletedAt IS NULL
        ORDER BY updatedAt DESC
    """)
    fun getUntaggedPromptsWithTags(): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM prompts
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        AND id NOT IN (SELECT DISTINCT promptId FROM prompt_tag_cross_ref)
        AND deletedAt IS NULL
        ORDER BY updatedAt DESC
    """)
    fun searchUntaggedPromptsWithTags(query: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT p.* FROM prompts p
        INNER JOIN prompt_tag_cross_ref ref ON p.id = ref.promptId
        WHERE ref.tagId = :tagId
        AND (p.title LIKE '%' || :query || '%' OR p.content LIKE '%' || :query || '%')
        AND p.deletedAt IS NULL
        ORDER BY p.updatedAt DESC
    """)
    fun searchPromptsByTag(tagId: String, query: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("SELECT * FROM prompts WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedPromptsWithTags(): Flow<List<PromptWithTags>>

    @Transaction
    @Query("SELECT * FROM prompts WHERE deletedAt IS NULL")
    suspend fun getAllPromptsOnce(): List<PromptWithTags>

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

    @Query("UPDATE prompts SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePrompt(id: String, timestamp: Long)

    @Query("UPDATE prompts SET deletedAt = NULL, updatedAt = :timestamp WHERE id = :id")
    suspend fun restorePrompt(id: String, timestamp: Long)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deletePrompt(id: String)

    @Query("DELETE FROM prompts WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun deleteExpiredPrompts(cutoff: Long)

    @Query("DELETE FROM prompts WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPromptTagCrossRef(crossRef: PromptTagCrossRef)

    @Query("DELETE FROM prompt_tag_cross_ref WHERE promptId = :promptId")
    suspend fun deleteCrossRefsForPrompt(promptId: String)

    @Query("DELETE FROM prompt_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteCrossRefsForTag(tagId: String)

    @Query("DELETE FROM tags WHERE tagId = :tagId")
    suspend fun deleteTag(tagId: String)
}
