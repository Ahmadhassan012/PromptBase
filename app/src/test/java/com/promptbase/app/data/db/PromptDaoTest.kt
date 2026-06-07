package com.promptbase.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptTagCrossRef
import com.promptbase.app.data.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PromptDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PromptDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.promptDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `getAllPromptsWithTags returns only active prompts ordered by updatedAt desc`() = runBlocking {
        val older = Prompt(id = "p1", title = "Older", content = "c", updatedAt = 100)
        val newer = Prompt(id = "p2", title = "Newer", content = "c", updatedAt = 200)
        val trashed = Prompt(id = "p3", title = "Trashed", content = "c", updatedAt = 300, deletedAt = 999)

        dao.insertPrompt(older)
        dao.insertPrompt(newer)
        dao.insertPrompt(trashed)

        val result = dao.getAllPromptsWithTags().first()
        assertEquals(2, result.size)
        assertEquals("Newer", result[0].prompt.title)
        assertEquals("Older", result[1].prompt.title)
    }

    @Test
    fun `searchPromptsWithTags matches title`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Alpha Beta", content = "xxx"))
        dao.insertPrompt(Prompt(id = "p2", title = "Gamma Delta", content = "xxx"))

        val result = dao.searchPromptsWithTags("Alpha").first()
        assertEquals(1, result.size)
        assertEquals("p1", result[0].prompt.id)
    }

    @Test
    fun `searchPromptsWithTags matches content`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "T", content = "secret sauce"))
        dao.insertPrompt(Prompt(id = "p2", title = "T", content = "other"))

        val result = dao.searchPromptsWithTags("secret").first()
        assertEquals(1, result.size)
        assertEquals("p1", result[0].prompt.id)
    }

    @Test
    fun `searchPromptsWithTags excludes trashed`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Visible", content = "test", deletedAt = null))
        dao.insertPrompt(Prompt(id = "p2", title = "Hidden", content = "test", deletedAt = 999))

        val result = dao.searchPromptsWithTags("test").first()
        assertEquals(1, result.size)
        assertEquals("Visible", result[0].prompt.title)
    }

    @Test
    fun `getPromptsByTag returns only prompts with that tag`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "A", content = "c"))
        dao.insertPrompt(Prompt(id = "p2", title = "B", content = "c"))
        dao.insertTag(Tag(tagId = "t1", name = "Coding"))
        dao.insertTag(Tag(tagId = "t2", name = "Writing"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "t1"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p2", "t2"))

        val result = dao.getPromptsByTag("t1").first()
        assertEquals(1, result.size)
        assertEquals("A", result[0].prompt.title)
    }

    @Test
    fun `getUntaggedPromptsWithTags returns prompts without tags`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Tagged", content = "c"))
        dao.insertPrompt(Prompt(id = "p2", title = "Untagged", content = "c"))
        dao.insertTag(Tag(tagId = "t1", name = "Coding"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "t1"))

        val result = dao.getUntaggedPromptsWithTags().first()
        assertEquals(1, result.size)
        assertEquals("Untagged", result[0].prompt.title)
    }

    @Test
    fun `searchUntaggedPromptsWithTags combines untagged and search`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Tagged A", content = "find me"))
        dao.insertPrompt(Prompt(id = "p2", title = "Untagged B", content = "find me"))
        dao.insertTag(Tag(tagId = "t1", name = "Coding"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "t1"))

        val result = dao.searchUntaggedPromptsWithTags("find").first()
        assertEquals(1, result.size)
        assertEquals("Untagged B", result[0].prompt.title)
    }

    @Test
    fun `searchPromptsByTag combines tag and search`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Coding Stuff", content = "fix bug"))
        dao.insertPrompt(Prompt(id = "p2", title = "Writing", content = "fix bug"))
        dao.insertTag(Tag(tagId = "t1", name = "Coding"))
        dao.insertTag(Tag(tagId = "t2", name = "Writing"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p1", "t1"))
        dao.insertPromptTagCrossRef(PromptTagCrossRef("p2", "t2"))

        val result = dao.searchPromptsByTag("t1", "bug").first()
        assertEquals(1, result.size)
        assertEquals("Coding Stuff", result[0].prompt.title)
    }

    @Test
    fun `getTrashedPromptsWithTags returns only trashed`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Active", content = "c"))
        dao.insertPrompt(Prompt(id = "p2", title = "Gone", content = "c", deletedAt = 500))

        val result = dao.getTrashedPromptsWithTags().first()
        assertEquals(1, result.size)
        assertEquals("Gone", result[0].prompt.title)
    }

    @Test
    fun `softDelete sets deletedAt`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "T", content = "c"))
        dao.softDeletePrompt("p1", 12345L)

        val prompt = dao.getPromptById("p1")!!
        assertNotNull(prompt.deletedAt)
        assertEquals(12345L, prompt.deletedAt)
    }

    @Test
    fun `restore clears deletedAt`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "T", content = "c", deletedAt = 500))
        dao.restorePrompt("p1", 999L)

        val prompt = dao.getPromptById("p1")!!
        assertNull(prompt.deletedAt)
    }

    @Test
    fun `deleteExpiredPrompts removes old trashed`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Recent", content = "c", deletedAt = 100))
        dao.insertPrompt(Prompt(id = "p2", title = "Old", content = "c", deletedAt = 1))

        dao.deleteExpiredPrompts(50)

        val result = dao.getTrashedPromptsWithTags().first()
        assertEquals(1, result.size)
        assertEquals("Recent", result[0].prompt.title)
    }

    @Test
    fun `emptyTrash removes all trashed`() = runBlocking {
        dao.insertPrompt(Prompt(id = "p1", title = "Active", content = "c"))
        dao.insertPrompt(Prompt(id = "p2", title = "Dead", content = "c", deletedAt = 500))

        dao.emptyTrash()

        val result = dao.getTrashedPromptsWithTags().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPromptById returns null for unknown`() = runBlocking {
        val result = dao.getPromptById("nonexistent")
        assertNull(result)
    }
}
