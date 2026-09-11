package rs.coffeeconquest.app.ui.cafe

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.checkIn
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.app.ui.review

class CafeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()
    private val location = mockk<LocationProvider>()

    @Before
    fun oneCafeWithReviews() {
        coEvery { location.current(any(), any()) } returns null
        coEvery { repository.cafe(any(), any(), any()) } returns cafe("c1")
        coEvery { repository.reviews(any()) } returns listOf(review("r1"))
        coEvery { repository.cafeCheckIns(any()) } returns listOf(checkIn("ci1"))
        coEvery { repository.postReview(any(), any(), any()) } returns review("r1", rating = 4)
    }

    @Test
    fun `opening a cafe loads it with its reviews and recent check-ins`() {
        val viewModel = CafeDetailViewModel(repository, location)

        viewModel.load("c1")

        assertEquals(cafe("c1"), viewModel.state.value.cafe.dataOrNull)
        assertEquals(1, viewModel.state.value.reviews.size)
        assertEquals(1, viewModel.state.value.recentCheckIns.size)
    }

    /** With a fix the cafe can be told how far away it is; without one it cannot. */
    @Test
    fun `a location fix is passed to the query`() {
        coEvery { location.current(any(), any()) } returns LocationFix(
            latitude = 43.3209,
            longitude = 21.8958,
            source = LocationFix.Source.GPS,
            accuracyMeters = 5f,
        )
        val viewModel = CafeDetailViewModel(repository, location)

        viewModel.load("c1")

        coVerify { repository.cafe("c1", 43.3209, 21.8958) }
    }

    @Test
    fun `without a fix the cafe is asked for without coordinates`() {
        val viewModel = CafeDetailViewModel(repository, location)

        viewModel.load("c1")

        coVerify { repository.cafe("c1", null, null) }
    }

    /** Reviews and check-ins are secondary: a missing cafe must not hide the error. */
    @Test
    fun `a missing cafe becomes an error`() {
        coEvery { repository.cafe(any(), any(), any()) } throws AppException("Kafic ne postoji.")
        val viewModel = CafeDetailViewModel(repository, location)

        viewModel.load("nema")

        assertEquals(UiState.Error("Kafic ne postoji."), viewModel.state.value.cafe)
    }

    @Test
    fun `a review without a rating is refused before the network`() {
        val viewModel = CafeDetailViewModel(repository, location)
        viewModel.load("c1")
        viewModel.onCommentChange("Odlicna kafa")

        viewModel.submitReview()

        assertEquals("Izaberite ocenu od 1 do 5.", viewModel.state.value.message)
        coVerify(exactly = 0) { repository.postReview(any(), any(), any()) }
    }

    @Test
    fun `a posted review clears the draft and reloads the cafe`() {
        val viewModel = CafeDetailViewModel(repository, location)
        viewModel.load("c1")
        viewModel.onRatingChange(4)
        viewModel.onCommentChange("Odlicna kafa")

        viewModel.submitReview()

        coVerify { repository.postReview("c1", 4, "Odlicna kafa") }
        assertEquals("Hvala na oceni!", viewModel.state.value.message)
        assertEquals("", viewModel.state.value.myComment)
        assertEquals(false, viewModel.state.value.submittingReview)
        coVerify(exactly = 2) { repository.cafe("c1", any(), any()) }
    }

    /** A rating with no words is a rating, not an empty comment. */
    @Test
    fun `a blank comment is sent as null`() {
        val viewModel = CafeDetailViewModel(repository, location)
        viewModel.load("c1")
        viewModel.onRatingChange(5)
        viewModel.onCommentChange("   ")

        viewModel.submitReview()

        coVerify { repository.postReview("c1", 5, null) }
    }

    @Test
    fun `a refused review reports the reason`() {
        coEvery { repository.postReview(any(), any(), any()) } throws
            AppException("Nemate dozvolu za ovu akciju.")
        val viewModel = CafeDetailViewModel(repository, location)
        viewModel.load("c1")
        viewModel.onRatingChange(3)

        viewModel.submitReview()

        assertEquals("Nemate dozvolu za ovu akciju.", viewModel.state.value.message)
        assertEquals(false, viewModel.state.value.submittingReview)
    }

    @Test
    fun `dismissing clears the message`() {
        val viewModel = CafeDetailViewModel(repository, location)
        viewModel.load("c1")
        viewModel.submitReview()

        viewModel.dismissMessage()

        assertNull(viewModel.state.value.message)
    }
}
