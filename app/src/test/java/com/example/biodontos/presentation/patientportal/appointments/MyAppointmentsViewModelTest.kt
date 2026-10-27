package com.example.biodontos.presentation.patientportal.appointments

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MyAppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentPatientUseCase: GetCurrentPatientUseCase = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private val cancelAppointmentUseCase: CancelAppointmentUseCase = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testNow = LocalDateTime.of(2026, 10, 15, 10, 0)

    private val testPatient = Patient(
        id = "patient-1",
        userId = "user-1",
        dniCedula = "10203040",
        firstName = "Daniel",
        firstLastName = "Gómez"
    )

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Carolina",
        lastName = "Duque",
        specialty = "Ortodoncia",
        licenseNumber = "123456",
        email = "carolina@test.com"
    )

    private val upcomingAppt = Appointment(
        id = "appt-future",
        patientId = "patient-1",
        doctorId = "doc-1",
        startTime = testNow.plusDays(2),
        status = AppointmentStatus.CONFIRMED,
        reason = "Control de ortodoncia"
    )

    private val completedAppt = Appointment(
        id = "appt-completed",
        patientId = "patient-1",
        doctorId = "doc-1",
        startTime = testNow.minusDays(5),
        status = AppointmentStatus.COMPLETED,
        reason = "Limpieza"
    )

    private val cancelledAppt = Appointment(
        id = "appt-cancelled",
        patientId = "patient-1",
        doctorId = "doc-1",
        startTime = testNow.plusDays(1),
        status = AppointmentStatus.CANCELLED,
        reason = "Consulta cancelada"
    )

    private val noDoctorAppt = Appointment(
        id = "appt-no-doctor",
        patientId = "patient-1",
        doctorId = null,
        startTime = testNow.plusDays(3),
        status = AppointmentStatus.CONFIRMED,
        reason = "Valoración general"
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
        coEvery { getCurrentPatientUseCase() } returns Result.success(testPatient)
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(testDoctor))
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(upcomingAppt, completedAppt, cancelledAppt)
        )
    }

    private fun createViewModel(): MyAppointmentsViewModel {
        return MyAppointmentsViewModel(
            getCurrentPatientUseCase = getCurrentPatientUseCase,
            appointmentRepository = appointmentRepository,
            doctorRepository = doctorRepository,
            cancelAppointmentUseCase = cancelAppointmentUseCase,
            clinicClock = clinicClock
        )
    }

    @Test
    fun `initial load partitions upcoming, completed, and cancelled appointments correctly with badge counts`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("appt-future", state.upcomingAppointments[0].id)
        assertEquals(1, state.upcomingCount)

        assertEquals(1, state.completedAppointments.size)
        assertEquals("appt-completed", state.completedAppointments[0].id)
        assertEquals(1, state.completedCount)

        assertEquals(1, state.cancelledAppointments.size)
        assertEquals("appt-cancelled", state.cancelledAppointments[0].id)
        assertEquals(1, state.cancelledCount)

        assertEquals(2, state.pastAppointments.size)
    }

    @Test
    fun `select tab updates selectedTab in uiState for all three tabs`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.COMPLETED))
        assertEquals(AppointmentTab.COMPLETED, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.CANCELLED))
        assertEquals(AppointmentTab.CANCELLED, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.UPCOMING))
        assertEquals(AppointmentTab.UPCOMING, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `when appointment repository fails, errorMessage is populated and loading stops`() = runTest {
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.failure(
            RuntimeException("Error al sincronizar citas con el servidor")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Error al sincronizar citas con el servidor", state.errorMessage)
        assertTrue(state.upcomingAppointments.isEmpty())
        assertTrue(state.completedAppointments.isEmpty())
        assertTrue(state.cancelledAppointments.isEmpty())
    }

    @Test
    fun `when patient session is invalid, errorMessage is populated and loading stops`() = runTest {
        coEvery { getCurrentPatientUseCase() } returns Result.failure(
            RuntimeException("Sesión de usuario no disponible")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No se pudo identificar la sesión del paciente", state.errorMessage)
    }

    @Test
    fun `when refresh event is triggered after failure, appointments reload successfully`() = runTest {
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.failure(
            RuntimeException("Error de red temporal")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Error de red temporal", viewModel.uiState.value.errorMessage)

        // Ahora simulamos recuperación exitosa del servicio
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(upcomingAppt, completedAppt, cancelledAppt)
        )

        viewModel.onEvent(MyAppointmentsUiEvent.Refresh)
        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value
        assertFalse(refreshedState.isLoading)
        assertNull(refreshedState.errorMessage)
        assertEquals(1, refreshedState.upcomingAppointments.size)
        assertEquals(1, refreshedState.completedAppointments.size)
        assertEquals(1, refreshedState.cancelledAppointments.size)
    }

    @Test
    fun `reschedule appointment with valid doctorId emits NavigateToReschedule`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.RescheduleAppointment(upcomingAppt))
            val effect = awaitItem()
            assertEquals(MyAppointmentsUiEffect.NavigateToReschedule("doc-1", "appt-future"), effect)
        }
    }

    @Test
    fun `reschedule appointment with null or blank doctorId emits toast and NavigateToDoctorSearch fallback`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.RescheduleAppointment(noDoctorAppt))
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertEquals("Selecciona un especialista para reagendar tu cita", (toast as MyAppointmentsUiEffect.ShowToast).message)

            val nav = awaitItem()
            assertEquals(MyAppointmentsUiEffect.NavigateToDoctorSearch, nav)
        }
    }

    @Test
    fun `book new appointment emits NavigateToDoctorSearch`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.BookNewAppointment)
            val effect = awaitItem()
            assertEquals(MyAppointmentsUiEffect.NavigateToDoctorSearch, effect)
        }
    }

    @Test
    fun `request cancel and dismiss cancel update appointmentToCancel`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))
        assertEquals(upcomingAppt, viewModel.uiState.value.appointmentToCancel)

        viewModel.onEvent(MyAppointmentsUiEvent.DismissCancelDialog)
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }

    @Test
    fun `confirm cancel success calls usecase, emits success toast and reloads`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns Unit.right()
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(completedAppt, cancelledAppt)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertEquals("Cita cancelada con éxito", (toast as MyAppointmentsUiEffect.ShowToast).message)
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }

    @Test
    fun `confirm cancel with ModificationWindowClosed emits dynamic hours toast message`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns AppointmentError.ModificationWindowClosed(minHoursBefore = 4).left()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertTrue((toast as MyAppointmentsUiEffect.ShowToast).message.contains("4 horas"))
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }

    @Test
    fun `confirm cancel with AlreadyCancelled emits appropriate toast message`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns AppointmentError.AlreadyCancelled.left()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertTrue((toast as MyAppointmentsUiEffect.ShowToast).message.contains("ya se encuentra cancelada"))
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }

    @Test
    fun `confirm cancel with NotOwner emits permissions toast message`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns AppointmentError.NotOwner.left()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertTrue((toast as MyAppointmentsUiEffect.ShowToast).message.contains("permisos"))
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }

    @Test
    fun `confirm cancel with AppointmentNotFound emits not found toast message`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns AppointmentError.AppointmentNotFound.left()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assertTrue(toast is MyAppointmentsUiEffect.ShowToast)
            assertTrue((toast as MyAppointmentsUiEffect.ShowToast).message.contains("No se encontró"))
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }
}
