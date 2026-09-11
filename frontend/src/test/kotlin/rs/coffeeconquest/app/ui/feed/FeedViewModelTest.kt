package rs.coffeeconquest.app.ui.feed

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.app.ui.feedItem
import rs.coffeeconquest.shared.dto.FeedItem

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    @Test
    fun `the feed shows what the stream gives it`() {
        every { repository.feedStream(any(), any()) } returns flowOf(listOf(feedItem()))

        val viewModel = FeedViewModel(repository)
        viewModel.load()

        assertEquals(1, viewModel.state.value.items.dataOrNull?.size)
    }

    /** A check-in made on another phone should land here by itself. */
    @Test
    fun `a new event arrives without reopening the tab`() {
        val stream = MutableSharedFlow<List<FeedItem>>(replay = 1)
        stream.tryEmit(listOf(feedItem("f1")))
        every { repository.feedStream(any(), any()) } returns stream

        val viewModel = FeedViewModel(repository)
        viewModel.load()
        assertEquals(1, viewModel.state.value.items.dataOrNull?.size)

        stream.tryEmit(listOf(feedItem("f2"), feedItem("f1")))

        assertEquals(2, viewModel.state.value.items.dataOrNull?.size)
    }

    @Test
    fun `switching to following resubscribes with the narrower scope`() {
        every { repository.feedStream(any(), any()) } returns flowOf(emptyList())
        val viewModel = FeedViewModel(repository)
        viewModel.load()

        viewModel.onScopeChange(followingOnly = true)

        assertEquals(true, viewModel.state.value.followingOnly)
        verify { repository.feedStream(true, any()) }
    }

    @Test
    fun `a failing stream becomes an error message`() {
        every { repository.feedStream(any(), any()) } returns
            flow { throw AppException("Server nije dostupan.") }

        val viewModel = FeedViewModel(repository)
        viewModel.load()

        assertEquals(UiState.Error("Server nije dostupan."), viewModel.state.value.items)
    }
}
