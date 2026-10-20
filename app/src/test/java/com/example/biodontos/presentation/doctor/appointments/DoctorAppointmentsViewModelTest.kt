package com.example.biodontos.presentation.doctor.appointments

import app.cash.turbine.test
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.domain.usecase.doctor.GetDoctorAvailabilityUseCase
import com.example.biodontos.domain.usecase.doctor.SaveDoctorAvailabilityUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorAppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase = mockk()
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase = mockk()
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase = mockk()
    private val getDoctorAvailabilityUseCase: GetDoctorAvailabilityUseCase = mockk()
    private val saveDoctorAvailabilityUseCase: SaveDoctorAvailabilityUseCase = mockk()
    private val bookAppointmentUseCase: BookAppointmentUseCase = mockk()
    private val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase = mockk()
    private val cancelAppointmentUseCase: CancelAppointmentUseCase = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Andrés",
        lastName = "Castro",
        specialty = "Ortodoncia",
        licenseNumber = "12345",
        email = "andres@test.com"
    )

    private val testPatients = listOf(
        Patient(id = "pat-1", dniCedula = "111", firstName = "Luis", firstLastName = "Camilo")
    )

    // Martes 20 de Octubre de 2026
    private val todayDate = LocalDate.of(2026, 10, 20)

    private val testAppointments = listOf(
        Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 20, 9, 0),
            isConfirmed = true,
            reason = "Control",
            status = AppointmentStatus.CONFIRMED
        ),
        Appointment(
            id = "appt-2",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 25, 11, 0),
            isConfirmed = false,
            reason = "Endodoncia",
            status = AppointmentStatus.PENDING
        )
    )

    private val testAvailabilities = listOf(
        WeeklyAvailability(
            doctorId = DoctorId("doc-1"),
            dayOfWeek = DayOfWeek.TUESDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(12, 0),
            slotMinutes = 30,
            isActive = true
        )
    )

    @Before
    fun setUp() {
        every { clinicClock.now() } returns LocalDateTime.of(2026, 10, 20, 8, 15)
        coEvery { getCurrentDoctorUseCase() } returns Result.success(testDoctor)
        coEvery { patientRepository.getPatients() } returns Result.success(testPatients)
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(testAppointments)
        coEvery { getDoctorAvailabilityUseCase("doc-1") } returns Result.success(testAvailabilities)
    }

    private fun createViewModel() = DoctorAppointmentsViewModel(
        getCurrentDoctorUseCase = getCurrentDoctorUseCase,
        getDoctorAppointmentsUseCase = getDoctorAppointmentsUseCase,
        toggleAppointmentConfirmationUseCase = toggleAppointmentConfirmationUseCase,
        getDoctorAvailabilityUseCase = getDoctorAvailabilityUseCase,
        saveDoctorAvailabilityUseCase = saveDoctorAvailabilityUseCase,
        bookAppointmentUseCase = bookAppointmentUseCase,
        rescheduleAppointmentUseCase = rescheduleAppointmentUseCase,
        cancelAppointmentUseCase = cancelAppointmentUseCase,
        patientRepository = patientRepository,
        clinicClock = clinicClock
    )

    @Test
    fun `al inicializar carga citas y calcula slots del cronograma correctamente`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.appointments.size)
        assertEquals("doc-1", state.doctorId)
        assertEquals("Dr. Andrés Castro", state.doctorName)

        // Verificamos que hay slots calculados para el día (8:00 a 12:00 cada 30 min = 8 slots)
        assertFalse(state.isDayOff)
        assertEquals(8, state.timelineSlots.size)

        // El slot de 08:00 a 08:30 debe ser Past (ahora es 08:15)
        val firstSlot = state.timelineSlots.first()
        assertTrue(firstSlot.status is DoctorSlotStatus.Past)

        // El slot de 09:00 a 09:30 debe ser Occupied con appt-1
        val occupiedSlot = state.timelineSlots.find { it.startTime.hour == 9 && it.startTime.minute == 0 }
        assertNotNull(occupiedSlot)
        val status = occupiedSlot?.status
        assertTrue(status is DoctorSlotStatus.Occupied)
        assertEquals("appt-1", (status as DoctorSlotStatus.Occupied).appointment.appointmentId)

        // El slot de 10:00 a 10:30 debe ser Available
        val availableSlot = state.timelineSlots.find { it.startTime.hour == 10 && it.startTime.minute == 0 }
        assertNotNull(availableSlot)
        assertTrue(availableSlot?.status is DoctorSlotStatus.Available)
    }

    @Test
    fun `al filtrar citas devuelve el subconjunto correcto`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Filtro Hoy
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.TODAY))
        val todayItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, todayItems.size)
        assertEquals("appt-1", todayItems.first().appointmentId)

        // Filtro Confirmadas
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.CONFIRMED))
        val confirmedItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, confirmedItems.size)
        assertTrue(confirmedItems.first().isConfirmed)

        // Filtro Sin Confirmar
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.UNCONFIRMED))
        val unconfirmedItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, unconfirmedItems.size)
        assertFalse(unconfirmedItems.first().isConfirmed)
    }

    @Test
    fun `al conmutar confirmacion exitosamente actualiza isConfirmed y emite efecto`() = runTest {
        coEvery { toggleAppointmentConfirmationUseCase("appt-1", false) } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorAppointmentsUiEvent.ToggleConfirm("appt-1", true))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.appointments.first { it.appointmentId == "appt-1" }.isConfirmed)

            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
        }
    }

    @Test
    fun `al cambiar fecha seleccionada recalcula los slots`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Seleccionar domingo (día no laborable en testAvailabilities)
        val sunday = LocalDate.of(2026, 10, 25)
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectDate(sunday))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sunday, state.selectedDate)
        assertTrue(state.isDayOff)
        assertTrue(state.timelineSlots.isEmpty())
    }

    @Test
    fun `reserva rapida en slot libre ejecuta BookAppointmentUseCase y recarga citas`() = runTest {
        val createdAppt = Appointment(
            id = "appt-new",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 20, 10, 0),
            isConfirmed = false,
            status = AppointmentStatus.PENDING
        )
        coEvery { bookAppointmentUseCase("pat-1", "doc-1", any(), any(), any()) } returns Result.success(createdAppt)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val slot = viewModel.uiState.value.timelineSlots.first { it.status is DoctorSlotStatus.Available }

        viewModel.effects.test {
            viewModel.onEvent(DoctorAppointmentsUiEvent.OpenQuickBooking(slot))
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.bookingTargetSlot)

            viewModel.onEvent(DoctorAppointmentsUiEvent.ConfirmQuickBooking("pat-1", "Limpieza dental"))
            advanceUntilIdle()

            coVerify(exactly = 1) { bookAppointmentUseCase("pat-1", "doc-1", slot.startTime, slot.endTime, "Limpieza dental") }
            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            assertEquals("Cita reservada con éxito", (effect as DoctorAppointmentsUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `reagendamiento ejecuta RescheduleAppointmentUseCase y emite efecto`() = runTest {
        coEvery { rescheduleAppointmentUseCase("appt-1", any(), any()) } returns Unit.right()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val apptItem = viewModel.uiState.value.appointments.first { it.appointmentId == "appt-1" }
        val targetStartTime = LocalDateTime.of(2026, 10, 20, 10, 30)

        viewModel.effects.test {
            viewModel.onEvent(DoctorAppointmentsUiEvent.OpenReschedule(apptItem))
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.reschedulingAppointment)

            viewModel.onEvent(DoctorAppointmentsUiEvent.ConfirmReschedule(targetStartTime))
            advanceUntilIdle()

            coVerify(exactly = 1) { rescheduleAppointmentUseCase("appt-1", targetStartTime) }
            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            assertEquals("Cita reagendada exitosamente", (effect as DoctorAppointmentsUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `cancelacion ejecuta CancelAppointmentUseCase y emite efecto`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-1") } returns Unit.right()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val apptItem = viewModel.uiState.value.appointments.first { it.appointmentId == "appt-1" }

        viewModel.effects.test {
            viewModel.onEvent(DoctorAppointmentsUiEvent.RequestCancelAppointment(apptItem))
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.cancellingAppointment)

            viewModel.onEvent(DoctorAppointmentsUiEvent.ConfirmCancelAppointment)
            advanceUntilIdle()

            coVerify(exactly = 1) { cancelAppointmentUseCase("appt-1") }
            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            assertEquals("Cita cancelada correctamente", (effect as DoctorAppointmentsUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `guardar disponibilidad semanal ejecuta SaveDoctorAvailabilityUseCase y actualiza estado`() = runTest {
        coEvery { saveDoctorAvailabilityUseCase("doc-1", any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DoctorAppointmentsUiEvent.OpenAvailabilityEditor)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showAvailabilityDialog)

            viewModel.onEvent(DoctorAppointmentsUiEvent.SaveAvailability)
            advanceUntilIdle()

            coVerify(exactly = 1) { saveDoctorAvailabilityUseCase("doc-1", any()) }
            assertFalse(viewModel.uiState.value.showAvailabilityDialog)
            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            assertEquals("Horario semanal actualizado exitosamente", (effect as DoctorAppointmentsUiEffect.ShowMessage).message)
        }
    }
}
