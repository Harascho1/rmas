package rs.coffeeconquest.app.ui.staff

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.dataOrNull

/**
 * Only the parts that stay in the JVM. Generating the code itself calls into
 * android.graphics, so refreshToken() is covered up to that line and no further.
 */
class StaffQrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    @Test
    fun `the first cafe is selected so staff can start without tapping`() {
        coEvery { repository.myCafes() } returns listOf(cafe("c1"), cafe("c2", "Bigmajstor"))

        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        assertEquals(2, viewModel.state.value.cafes.dataOrNull?.size)
        assertEquals("c1", viewModel.state.value.selectedCafeId)
    }

    /** Reloading must not drag the user back to the first cafe. */
    @Test
    fun `a reload keeps the cafe the user picked`() {
        coEvery { repository.myCafes() } returns listOf(cafe("c1"), cafe("c2", "Bigmajstor"))
        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        viewModel.select("c2")
        viewModel.load()

        assertEquals("c2", viewModel.state.value.selectedCafeId)
    }

    @Test
    fun `switching cafes drops the code that belonged to the old one`() {
        coEvery { repository.myCafes() } returns listOf(cafe("c1"), cafe("c2", "Bigmajstor"))
        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        viewModel.select("c2")

        assertNull(viewModel.state.value.token)
        assertNull(viewModel.state.value.qrBitmap)
    }

    @Test
    fun `staff assigned to nothing sees an empty list and selects nobody`() {
        coEvery { repository.myCafes() } returns emptyList()

        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        assertEquals(emptyList<Any>(), viewModel.state.value.cafes.dataOrNull)
        assertNull(viewModel.state.value.selectedCafeId)
    }

    @Test
    fun `asking for a code before choosing a cafe does nothing`() {
        val viewModel = StaffQrViewModel(repository)

        viewModel.refreshToken()

        coVerify(exactly = 0) { repository.qrToken(any()) }
    }

    @Test
    fun `a refused token is reported and leaves no stale code on screen`() {
        coEvery { repository.myCafes() } returns listOf(cafe("c1"))
        coEvery { repository.qrToken(any()) } throws AppException("Ovaj kafic nije vas.")
        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        viewModel.refreshToken()

        assertEquals("Ovaj kafic nije vas.", viewModel.state.value.error)
        assertNull(viewModel.state.value.qrBitmap)
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun `a failing list becomes an error message`() {
        coEvery { repository.myCafes() } throws AppException("Server nije dostupan.")

        val viewModel = StaffQrViewModel(repository)
        viewModel.load()

        assertEquals(UiState.Error("Server nije dostupan."), viewModel.state.value.cafes)
    }
}
