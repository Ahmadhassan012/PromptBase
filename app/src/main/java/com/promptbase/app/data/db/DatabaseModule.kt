package com.promptbase.app.data.db

import android.content.Context
import androidx.room.Room
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
        .createFromAsset("databases/promptbase.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    fun providePromptDao(db: AppDatabase): PromptDao = db.promptDao()
}
