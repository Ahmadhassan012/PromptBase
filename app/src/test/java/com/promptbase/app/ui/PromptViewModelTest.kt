package com.promptbase.app.ui

import app.cash.turbine.test
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptWithTags
import com.promptbase.app.data.model.Tag
import com.promptbase.app.data.repository.PromptRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromptViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var repository: PromptRepository

    private lateinit var viewModel: PromptViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        val allTags = MutableStateFlow(
            listOf(Tag(tagId = "1", name = "Coding"), Tag(tagId = "2", name = "Writing"))
        )

        every { repository.allTags } returns allTags
        every { repository.allPrompts } returns MutableStateFlow(emptyList())
        every { repository.trashedPrompts } returns MutableStateFlow(emptyList())
        every { repository.searchPrompts(any()) } returns MutableStateFlow(emptyList())
        every { repository.filterPromptsByTag(any()) } returns MutableStateFlow(emptyList())
        every { repository.searchPromptsByTag(any(), any()) } returns MutableStateFlow(emptyList())
        every { repository.getUntaggedPrompts() } returns MutableStateFlow(emptyList())
        every { repository.searchUntaggedPrompts(any()) } returns MutableStateFlow(emptyList())
        coEvery { repository.deleteExpiredPrompts() } returns Unit
        coEvery { repository.savePrompt(any(), any(), any(), any()) } returns Unit
        coEvery { repository.softDeletePrompt(any()) } returns Unit

        viewModel = PromptViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init deletes expired prompts`() {
        coVerify { repository.deleteExpiredPrompts() }
    }

    @Test
    fun `allTags reflects repository`() {
        val tags = viewModel.allTags.value
        assertEquals(2, tags.size)
        assertEquals("Coding", tags[0].name)
    }

    @Test
    fun `setSearchQuery updates searchQuery state`() {
        viewModel.setSearchQuery("test")
        assertEquals("test", viewModel.searchQuery.value)
    }

    @Test
    fun `selectTag sets selected tag and clears untagged`() {
        viewModel.selectUntagged()
        viewModel.selectTag(Tag(tagId = "1", name = "Coding"))
        assertEquals("Coding", viewModel.selectedTag.value?.name)
        assertFalse(viewModel.showOnlyUntagged.value)
    }

    @Test
    fun `selectUntagged sets flag and clears tag`() {
        viewModel.selectTag(Tag(tagId = "1", name = "Coding"))
        viewModel.selectUntagged()
        assertTrue(viewModel.showOnlyUntagged.value)
        assertNull(viewModel.selectedTag.value)
    }

    @Test
    fun `startNewPrompt clears editing state`() {
        val prompt = PromptWithTags(
            Prompt(id = "p1", title = "Old", content = "old"),
            emptyList()
        )
        viewModel.startEditing(prompt)
        viewModel.startNewPrompt()

        assertNull(viewModel.editingPrompt.value)
        assertEquals("", viewModel.editTitle.value)
        assertEquals("", viewModel.editContent.value)
        assertTrue(viewModel.editTags.value.isEmpty())
    }

    @Test
    fun `startEditing loads prompt data`() {
        val tag = Tag(tagId = "1", name = "Coding")
        val prompt = PromptWithTags(
            Prompt(id = "p1", title = "Test Title", content = "Test Content"),
            listOf(tag)
        )

        viewModel.startEditing(prompt)

        assertEquals("Test Title", viewModel.editTitle.value)
        assertEquals("Test Content", viewModel.editContent.value)
        assertEquals(listOf("Coding"), viewModel.editTags.value)
    }

    @Test
    fun `addTagToEdit adds unique tag`() {
        viewModel.addTagToEdit("Coding")
        viewModel.addTagToEdit("Writing")

        assertEquals(listOf("Coding", "Writing"), viewModel.editTags.value)
    }

    @Test
    fun `addTagToEdit ignores duplicates`() {
        viewModel.addTagToEdit("Coding")
        viewModel.addTagToEdit("coding")

        assertEquals(listOf("Coding"), viewModel.editTags.value)
    }

    @Test
    fun `removeTagFromEdit removes tag`() {
        viewModel.addTagToEdit("Coding")
        viewModel.addTagToEdit("Writing")
        viewModel.removeTagFromEdit("Coding")

        assertEquals(listOf("Writing"), viewModel.editTags.value)
    }

    @Test
    fun `saveCurrentPrompt with null promptId creates new`() {
        viewModel.startNewPrompt()
        viewModel.editTitle.value = "New Title"
        viewModel.editContent.value = "New Content"

        var saved = false
        viewModel.saveCurrentPrompt { saved = true }

        coVerify { repository.savePrompt(null, "New Title", "New Content", emptyList()) }
        assertTrue(saved)
    }

    @Test
    fun `saveCurrentPrompt with existing promptId updates`() {
        val prompt = PromptWithTags(
            Prompt(id = "p1", title = "Old", content = "old"),
            listOf(Tag(tagId = "1", name = "Coding"))
        )
        viewModel.startEditing(prompt)
        viewModel.editTitle.value = "Updated"
        viewModel.editContent.value = "New content"

        var saved = false
        viewModel.saveCurrentPrompt { saved = true }

        coVerify { repository.savePrompt("p1", "Updated", "New content", listOf("Coding")) }
        assertTrue(saved)
    }

    @Test
    fun `saveCurrentPrompt does not save when title empty`() {
        viewModel.startNewPrompt()
        viewModel.editContent.value = "Content"

        viewModel.saveCurrentPrompt {}
        coVerify(exactly = 0) { repository.savePrompt(any(), any(), any(), any()) }
    }

    @Test
    fun `saveCurrentPrompt does not save when content empty`() {
        viewModel.startNewPrompt()
        viewModel.editTitle.value = "Title"

        viewModel.saveCurrentPrompt {}
        coVerify(exactly = 0) { repository.savePrompt(any(), any(), any(), any()) }
    }

    @Test
    fun `softDeletePrompt delegates to repository`() {
        viewModel.softDeletePrompt("p1")
        coVerify { repository.softDeletePrompt("p1") }
    }

    @Test
    fun `search query triggers repository search`() = runTest {
        val results = listOf(
            PromptWithTags(Prompt(id = "p1", title = "Found", content = "c"), emptyList())
        )
        every { repository.searchPrompts("query") } returns MutableStateFlow(results)

        viewModel.prompts.test {
            viewModel.setSearchQuery("query")
            skipItems(1)
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals("Found", emitted[0].prompt.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `select tag triggers repository filter`() = runTest {
        val codingTag = Tag(tagId = "1", name = "Coding")
        val results = listOf(
            PromptWithTags(Prompt(id = "p1", title = "Code", content = "c"), listOf(codingTag))
        )
        every { repository.filterPromptsByTag("1") } returns MutableStateFlow(results)

        viewModel.prompts.test {
            viewModel.selectTag(codingTag)
            skipItems(1)
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals("Code", emitted[0].prompt.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `select untagged triggers repository`() = runTest {
        val results = listOf(
            PromptWithTags(Prompt(id = "p1", title = "NoTag", content = "c"), emptyList())
        )
        every { repository.getUntaggedPrompts() } returns MutableStateFlow(results)

        viewModel.prompts.test {
            viewModel.selectUntagged()
            skipItems(1)
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
