package com.promptbase.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptTagCrossRef
import com.promptbase.app.data.model.Tag

@Database(
    entities = [Prompt::class, Tag::class, PromptTagCrossRef::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS prompts_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER DEFAULT NULL, `syncStatus` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO prompts_new (id, title, content, createdAt, updatedAt, deletedAt, syncStatus) SELECT id, title, content, createdAt, updatedAt, CASE WHEN isArchived = 1 THEN updatedAt ELSE NULL END, syncStatus FROM prompts")
        db.execSQL("DROP TABLE prompts")
        db.execSQL("ALTER TABLE prompts_new RENAME TO prompts")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_prompts_title ON prompts(title)")
    }
}
