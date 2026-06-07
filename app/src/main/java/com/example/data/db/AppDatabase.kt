package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Prompt
import com.example.data.model.PromptTagCrossRef
import com.example.data.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Prompt::class, Tag::class, PromptTagCrossRef::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS prompts_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER DEFAULT NULL, `syncStatus` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO prompts_new (id, title, content, createdAt, updatedAt, deletedAt, syncStatus) SELECT id, title, content, createdAt, updatedAt, CASE WHEN isArchived = 1 THEN updatedAt ELSE NULL END, syncStatus FROM prompts")
                db.execSQL("DROP TABLE prompts")
                db.execSQL("ALTER TABLE prompts_new RENAME TO prompts")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prompts_title ON prompts(title)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "promptbase_database"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.promptDao())
                }
            }
        }

        suspend fun populateInitialData(dao: PromptDao) {
            val tags = listOf(
                Tag(tagId = "1", name = "Writing"),
                Tag(tagId = "2", name = "Coding"),
                Tag(tagId = "3", name = "Marketing"),
                Tag(tagId = "4", name = "Productivity"),
                Tag(tagId = "5", name = "Ideas")
            )
            tags.forEach { dao.insertTag(it) }

            val starterPrompt1 = Prompt(
                id = "p1",
                title = "Email Copilot",
                content = "Write a professional, polite, and persuasive email to {{ recipient : a busy stakeholder }} regarding {{ topic : project status update }}. Make sure to highlight that we need the {{ resource : budget approval }} by {{ deadline : this Friday }} to avoid critical delays. Maintain a {{ tone : collaborative }} tone throughout.",
                createdAt = System.currentTimeMillis() - 100000,
                updatedAt = System.currentTimeMillis() - 100000
            )

            val starterPrompt2 = Prompt(
                id = "p2",
                title = "Bug Squash Assistant",
                content = "Explain why this code block produces an unexpected behavioral error:\n\n```{{ language : kotlin }}\n{{ code_snippet : val list = listOf(1, 2, 3)\nlist[3] }}\n```\nProvide a step-by-step root cause analysis, write the corrected code block, and suggest a modern unit-level testing methodology to prevent future regressions under {{ environment : production }}.",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            dao.insertPrompt(starterPrompt1)
            dao.insertPrompt(starterPrompt2)

            dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "1"))
            dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "4"))
            dao.insertPromptTagCrossRef(PromptTagCrossRef("p2", "2"))
        }
    }
}
