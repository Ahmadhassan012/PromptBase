package com.example.util

import com.example.data.model.PromptWithTags
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ExportedPrompt(
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExportedData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val prompts: List<ExportedPrompt>
)

object ExportImport {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val exportedDataAdapter = moshi.adapter(ExportedData::class.java)
    private val exportedPromptAdapter = moshi.adapter(ExportedPrompt::class.java)
    private val listType = Types.newParameterizedType(List::class.java, ExportedPrompt::class.java)
    private val listAdapter = moshi.adapter<List<ExportedPrompt>>(listType)

    fun exportToJson(prompts: List<PromptWithTags>): String {
        val exported = ExportedData(
            prompts = prompts.map { ptw ->
                ExportedPrompt(
                    title = ptw.prompt.title,
                    content = ptw.prompt.content,
                    tags = ptw.tags.map { it.name },
                    createdAt = ptw.prompt.createdAt,
                    updatedAt = ptw.prompt.updatedAt
                )
            }
        )
        return exportedDataAdapter.toJson(exported)
    }

    fun parseImportJson(json: String): Result<List<ExportedPrompt>> {
        return try {
            val adapter = moshi.adapter(ExportedData::class.java)
            val data = adapter.fromJson(json)
            if (data == null) {
                return Result.failure(Exception("Invalid JSON format"))
            }
            if (data.version != 1) {
                return Result.failure(Exception("Unsupported export version: ${data.version}"))
            }
            for ((i, prompt) in data.prompts.withIndex()) {
                if (prompt.title.isBlank()) {
                    return Result.failure(Exception("Prompt #${i + 1} has an empty title"))
                }
                if (prompt.content.isBlank()) {
                    return Result.failure(Exception("Prompt #${i + 1} has empty content"))
                }
            }
            Result.success(data.prompts)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse JSON: ${e.message}"))
        }
    }
}
