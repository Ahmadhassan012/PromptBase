package com.promptbase.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptTagCrossRef
import com.promptbase.app.data.model.Tag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "promptbase_database"
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(SeedCallback)
        .build()
    }

    @Provides
    fun providePromptDao(db: AppDatabase): PromptDao = db.promptDao()
}

private val SeedCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val tags = listOf(
            Tag(tagId = "1", name = "Writing"),
            Tag(tagId = "2", name = "Coding"),
            Tag(tagId = "3", name = "Marketing"),
            Tag(tagId = "4", name = "Productivity"),
            Tag(tagId = "5", name = "Ideas")
        )

        val now = System.currentTimeMillis()
        tags.forEach { tag ->
            db.execSQL("INSERT OR IGNORE INTO tags (tagId, name) VALUES (?, ?)",
                arrayOf(tag.tagId, tag.name))
        }

        db.execSQL("INSERT OR IGNORE INTO prompts (id, title, content, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
            arrayOf("p1", "Email Copilot",
                "Write a professional, polite, and persuasive email to {{ recipient : a busy stakeholder }} regarding {{ topic : project status update }}. Make sure to highlight that we need the {{ resource : budget approval }} by {{ deadline : this Friday }} to avoid critical delays. Maintain a {{ tone : collaborative }} tone throughout.",
                now - 100000, now - 100000))

        db.execSQL("INSERT OR IGNORE INTO prompts (id, title, content, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
            arrayOf("p2", "Bug Squash Assistant",
                "Explain why this code block produces an unexpected behavioral error:\n\n```{{ language : kotlin }}\n{{ code_snippet : val list = listOf(1, 2, 3)\nlist[3] }}\n```\nProvide a step-by-step root cause analysis, write the corrected code block, and suggest a modern unit-level testing methodology to prevent future regressions under {{ environment : production }}.",
                now, now))

        db.execSQL("INSERT OR IGNORE INTO prompt_tag_cross_ref (promptId, tagId) VALUES (?, ?)", arrayOf("p1", "1"))
        db.execSQL("INSERT OR IGNORE INTO prompt_tag_cross_ref (promptId, tagId) VALUES (?, ?)", arrayOf("p1", "4"))
        db.execSQL("INSERT OR IGNORE INTO prompt_tag_cross_ref (promptId, tagId) VALUES (?, ?)", arrayOf("p2", "2"))
    }
}
