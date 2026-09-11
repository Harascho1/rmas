package rs.coffeeconquest.app.ui.checkin

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.challenge
import rs.coffeeconquest.app.ui.checkIn
import rs.coffeeconquest.shared.dto.CheckInResult
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.rules.Geo
import rs.coffeeconquest.shared.rules.ScoreRules

class CheckInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()
    private val location = mockk<LocationProvider>()

    private val theCafe = cafe("c1", latitude = 43.3200, longitude = 21.9000)

    private val atTheDoor = fix(43.3200, 21.9000)

    private val result = CheckInResult(
        checkIn = checkIn("ci1"),
        breakdown = ScoreRules.checkInScore(CheckInMethod.GPS, false, true, 1, 1.0),
        newTotalPoints = 30,
        level = 1,
        streakDays = 1,
    )

    @Before
    fun standingAtTheCafe() {
        coEvery { location.current(any(), any()) } returns atTheDoor
        coEvery { repository.cafe(any(), any(), any()) } returns theCafe
        coEvery { repository.checkIn(any()) } returns result
    }

    @Test
    fun `opening the screen locates the user and loads the cafe`() {
        val viewModel = CheckInViewModel(repository, location)

        viewModel.load("c1")

        assertEquals(atTheDoor, viewModel.state.value.fix)
        coVerify { repository.cafe("c1", 43.3200, 21.9000) }
    }

    @Test
    fun `standing at the cafe is in range and allows a GPS check-in`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        assertEquals(0.0, viewModel.state.value.distanceMeters!!, 1.0)
        assertTrue(viewModel.state.value.inRange)
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a fix beyond the allowed radius blocks a GPS check-in`() {
        coEvery { location.current(any(), any()) } returns fix(43.4000, 21.9000)
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        assertTrue(viewModel.state.value.distanceMeters!! > Geo.MAX_CHECKIN_DISTANCE_M)
        assertFalse(viewModel.state.value.inRange)
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `honour mode is always allowed, however far away the user is`() {
        coEvery { location.current(any(), any()) } returns fix(43.4000, 21.9000)
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        viewModel.onMethodChange(CheckInMethod.HONOR)

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `QR is not a submittable method from this screen`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        viewModel.onMethodChange(CheckInMethod.QR)

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `the preview scores GPS above honour`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        val gps = viewModel.state.value.preview.total
        viewModel.onMethodChange(CheckInMethod.HONOR)
        val honor = viewModel.state.value.preview.total

        assertTrue(gps > honor)
    }

    @Test
    fun `an active challenge multiplies the previewed score`() {
        coEvery { repository.cafe(any(), any(), any()) } returns
            theCafe.copy(activeChallenges = listOf(challenge(multiplier = 2.0)))
        val withBonus = CheckInViewModel(repository, location)
        withBonus.load("c1")

        coEvery { repository.cafe(any(), any(), any()) } returns theCafe
        val plain = CheckInViewModel(repository, location)
        plain.load("c1")

        assertEquals(
            plain.state.value.preview.total * 2,
            withBonus.state.value.preview.total,
        )
    }

    @Test
    fun `refreshing without a fix says so`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        coEvery { location.current(any(), any()) } returns null
        viewModel.refreshLocation()

        assertNull(viewModel.state.value.fix)
        assertEquals("Lokacija nije dostupna.", viewModel.state.value.error)
    }

    @Test
    fun `a submitted check-in sends the method, position and optional fields`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")
        viewModel.onNoteChange("Prva kafa danas")
        viewModel.onRatingChange(5)
        viewModel.onCommentChange("  ")

        viewModel.submit()

        coVerify {
            repository.checkIn(
                match {
                    it.cafeId == "c1" &&
                        it.method == CheckInMethod.GPS &&
                        it.latitude == 43.3200 &&
                        it.note == "Prva kafa danas" &&
                        it.rating == 5 &&
                        it.comment == null &&
                        it.photoId == null
                },
            )
        }
        assertEquals(result, viewModel.state.value.result)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `a rating of zero is sent as no rating at all`() {
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        viewModel.submit()

        coVerify { repository.checkIn(match { it.rating == null }) }
    }

    @Test
    fun `a refused check-in reports the reason and produces no result`() {
        coEvery { repository.checkIn(any()) } throws
            AppException("Sacekajte jos malo pre sledeceg check-ina.")
        val viewModel = CheckInViewModel(repository, location)
        viewModel.load("c1")

        viewModel.submit()

        assertEquals("Sacekajte jos malo pre sledeceg check-ina.", viewModel.state.value.error)
        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.submitting)
    }

    private fun fix(latitude: Double, longitude: Double) = LocationFix(
        latitude = latitude,
        longitude = longitude,
        source = LocationFix.Source.GPS,
        accuracyMeters = 5f,
    )
}
