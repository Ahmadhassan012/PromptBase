package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction
import java.util.UUID

@Entity(
    tableName = "prompts",
    indices = [Index(value = ["title"])]
)
data class Prompt(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val syncStatus: Int = 0
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val tagId: String = UUID.randomUUID().toString(),
    val name: String
)

@Entity(
    tableName = "prompt_tag_cross_ref",
    primaryKeys = ["promptId", "tagId"],
    indices = [Index(value = ["tagId"]), Index(value = ["promptId"])]
)
data class PromptTagCrossRef(
    val promptId: String,
    val tagId: String
)

data class PromptWithTags(
    @Embedded val prompt: Prompt,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(
            value = PromptTagCrossRef::class,
            parentColumn = "promptId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
