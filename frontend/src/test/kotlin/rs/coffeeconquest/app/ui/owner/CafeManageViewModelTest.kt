package rs.coffeeconquest.app.ui.owner

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.challenge
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.app.ui.review
import rs.coffeeconquest.shared.dto.CafeStats
import rs.coffeeconquest.shared.model.ChallengeScope

class CafeManageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    private val emptyStats = CafeStats(
        cafeId = "c1",
        cafeName = "Kafeterija",
        totalCheckIns = 12,
        checkInsLast7Days = 3,
        checkInsLast30Days = 9,
        uniqueVisitors = 5,
        averageRating = 4.5,
        reviewCount = 2,
        unansweredReviews = 1,
        dailyCheckIns = emptyList(),
        topVisitors = emptyList(),
    )

    @Before
    fun oneManagedCafe() {
        coEvery { repository.cafe(any(), any(), any()) } returns cafe("c1")
        coEvery { repository.cafeStats(any()) } returns emptyStats
        coEvery { repository.reviews(any()) } returns listOf(review("r1"))
        coEvery { repository.challenges(any(), any()) } returns listOf(challenge("ch1"))
        coEvery { repository.replyToReview(any(), any(), any()) } returns
            review("r1", ownerReply = "Hvala!")
        coEvery { repository.createChallenge(any()) } returns challenge("ch2")
        coEvery { repository.deleteChallenge(any()) } returns Unit
    }

    @Test
    fun `opening the dashboard loads cafe, numbers, reviews and challenges`() {
        val viewModel = CafeManageViewModel(repository)

        viewModel.load("c1")

        val state = viewModel.state.value
        assertEquals(cafe("c1"), state.cafe.dataOrNull)
        assertEquals(emptyStats, state.stats)
        assertEquals(1, state.reviews.size)
        assertEquals(1, state.challenges.size)
    }

    @Test
    fun `a cafe that is not yours becomes an error`() {
        coEvery { repository.cafeStats(any()) } throws AppException("Ovaj kafic nije vas.")
        coEvery { repository.cafe(any(), any(), any()) } throws AppException("Ovaj kafic nije vas.")
        val viewModel = CafeManageViewModel(repository)

        viewModel.load("c9")

        assertEquals(UiState.Error("Ovaj kafic nije vas."), viewModel.state.value.cafe)
    }

    @Test
    fun `an empty reply is refused before the network`() {
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")
        viewModel.onReplyDraftChange("r1", "   ")

        viewModel.sendReply("r1")

        assertEquals("Odgovor ne moze biti prazan.", viewModel.state.value.message)
        coVerify(exactly = 0) { repository.replyToReview(any(), any(), any()) }
    }

    @Test
    fun `a sent reply is trimmed, cleared from the draft and the page reloads`() {
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")
        viewModel.onReplyDraftChange("r1", "  Hvala!  ")

        viewModel.sendReply("r1")

        coVerify { repository.replyToReview("c1", "r1", "Hvala!") }
        assertEquals("Odgovor je objavljen.", viewModel.state.value.message)
        assertFalse(viewModel.state.value.replyDraft.containsKey("r1"))
        coVerify(exactly = 2) { repository.cafe("c1", any(), any()) }
    }

    @Test
    fun `a challenge without a title is refused`() {
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")

        viewModel.createChallenge()

        assertEquals("Unesite naziv izazova.", viewModel.state.value.message)
        coVerify(exactly = 0) { repository.createChallenge(any()) }
    }

    /** The form speaks in days; the request must speak in milliseconds. */
    @Test
    fun `the challenge window is built from the chosen number of days`() {
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")
        viewModel.onChallengeTitleChange("  Dupli poeni  ")
        viewModel.onChallengeMultiplierChange(3.0)
        viewModel.onChallengeDaysChange(2)

        viewModel.createChallenge()

        coVerify {
            repository.createChallenge(
                match {
                    it.title == "Dupli poeni" &&
                        it.scope == ChallengeScope.CAFE &&
                        it.cafeId == "c1" &&
                        it.multiplier == 3.0 &&
                        it.endsAtEpochMs - it.startsAtEpochMs == 2 * 24L * 60 * 60 * 1000
                },
            )
        }
        assertEquals("", viewModel.state.value.challengeTitle)
        assertEquals("Izazov je aktivan.", viewModel.state.value.message)
    }

    @Test
    fun `the challenge length is clamped to a sane range`() {
        val viewModel = CafeManageViewModel(repository)

        viewModel.onChallengeDaysChange(0)
        assertEquals(1, viewModel.state.value.challengeDays)

        viewModel.onChallengeDaysChange(99)
        assertEquals(14, viewModel.state.value.challengeDays)
    }

    @Test
    fun `deleting a challenge reloads the page`() {
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")

        viewModel.deleteChallenge("ch1")

        coVerify { repository.deleteChallenge("ch1") }
        coVerify(exactly = 2) { repository.challenges(null, "c1") }
    }

    @Test
    fun `a refused challenge reports the reason and stops being busy`() {
        coEvery { repository.createChallenge(any()) } throws
            AppException("Samo sampion grada moze da pokrene gradski izazov.")
        val viewModel = CafeManageViewModel(repository)
        viewModel.load("c1")
        viewModel.onChallengeTitleChange("Dupli poeni")

        viewModel.createChallenge()

        assertEquals(
            "Samo sampion grada moze da pokrene gradski izazov.",
            viewModel.state.value.message,
        )
        assertFalse(viewModel.state.value.busy)
    }
}
