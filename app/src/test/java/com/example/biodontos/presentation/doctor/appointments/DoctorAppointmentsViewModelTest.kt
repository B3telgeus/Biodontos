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
import org.junit.Assert.assertNull
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
    fun `al inicializar carga citas, disponibilidad y pacientes en paralelo y calcula slots`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.appointments.size)
        assertEquals("doc-1", state.doctorId)
        assertEquals("Dr. Andrés Castro", state.doctorName)

        // Verificamos que patientId y patientName se poblaron desde el repositorio de pacientes
        val firstAppt = state.appointments.first()
        assertEquals("pat-1", firstAppt.patientId)
        assertEquals("Luis Camilo", firstAppt.patientName)

        // Verificamos que se ejecutaron las llamadas en paralelo
        coVerify(exactly = 1) { getCurrentDoctorUseCase() }
        coVerify(exactly = 1) { patientRepository.getPatients() }
        coVerify(exactly = 1) { getDoctorAppointmentsUseCase("doc-1") }
        coVerify(exactly = 1) { getDoctorAvailabilityUseCase("doc-1") }

        // Verificamos cálculo de slots (8:00 a 12:00 cada 30 min = 8 slots)
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
        val occupiedAppt = (status as DoctorSlotStatus.Occupied).appointment
        assertEquals("appt-1", occupiedAppt.appointmentId)
        assertEquals("pat-1", occupiedAppt.patientId)

        // El slot de 10:00 a 10:30 debe ser Available
        val availableSlot = state.timelineSlots.find { it.startTime.hour == 10 && it.startTime.minute == 0 }
        assertNotNull(availableSlot)
        assertTrue(availableSlot?.status is DoctorSlotStatus.Available)
    }

    @Test
    fun `al fallar la identificacion del doctor expone errorMessage y no se queda cargando`() = runTest {
        coEvery { getCurrentDoctorUseCase() } returns Result.failure(RuntimeException("Sesión expirada"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals("No se pudo identificar la sesión del profesional", state.errorMessage)
    }

    @Test
    fun `al fallar getDoctorAppointmentsUseCase expone errorMessage amigable en el estado`() = runTest {
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.failure(RuntimeException("Timeout de red"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `al presionar Refresh se limpia errorMessage y se recarga todo`() = runTest {
        // Inicialmente falla
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.failure(RuntimeException("Error inicial"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Al refrescar tiene éxito
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(testAppointments)
        viewModel.onEvent(DoctorAppointmentsUiEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals(2, state.appointments.size)
    }

    @Test
    fun `al filtrar citas devuelve el subconjunto correcto`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Filtro Todas
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.ALL))
        val allItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(2, allItems.size)

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
    fun `al fallar conmutar confirmacion revierte el estado y emite efecto de error`() = runTest {
        coEvery { toggleAppointmentConfirmationUseCase("appt-1", false) } returns Result.failure(RuntimeException("Error en base de datos"))

        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorAppointmentsUiEvent.ToggleConfirm("appt-1", true))
            advanceUntilIdle()

            // Debe haberse revertido a true
            assertTrue(viewModel.uiState.value.appointments.first { it.appointmentId == "appt-1" }.isConfirmed)

            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            assertEquals("No se pudo actualizar el estado de confirmación", (effect as DoctorAppointmentsUiEffect.ShowMessage).message)
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

    @Test
    fun `al intentar guardar disponibilidad con hora fin anterior o igual a inicio no guarda y emite mensaje de error explicativo`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Configuramos Martes con hora fin anterior a hora inicio (ej. 17:00 a 08:00)
        viewModel.onEvent(
            DoctorAppointmentsUiEvent.UpdateDayAvailability(
                day = DayOfWeek.TUESDAY,
                isActive = true,
                startTime = LocalTime.of(17, 0),
                endTime = LocalTime.of(8, 0),
                slotMinutes = 30
            )
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DoctorAppointmentsUiEvent.SaveAvailability)
            advanceUntilIdle()

            // No debe haber llamado al usecase de guardar
            coVerify(exactly = 0) { saveDoctorAvailabilityUseCase(any(), any()) }

            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
            val msg = (effect as DoctorAppointmentsUiEffect.ShowMessage).message
            assertTrue(msg.contains("La hora de fin debe ser posterior a la de inicio"))
        }
    }

    @Test
    fun `al copiar disponibilidad de lunes a dias habiles replica la configuracion de lunes a viernes`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Configuramos Lunes con horario específico
        viewModel.onEvent(
            DoctorAppointmentsUiEvent.UpdateDayAvailability(
                day = DayOfWeek.MONDAY,
                isActive = true,
                startTime = LocalTime.of(7, 0),
                endTime = LocalTime.of(15, 0),
                slotMinutes = 45
            )
        )
        advanceUntilIdle()

        // Copiamos a días hábiles
        viewModel.onEvent(DoctorAppointmentsUiEvent.CopyWeekdayAvailability(DayOfWeek.MONDAY))
        advanceUntilIdle()

        val weekly = viewModel.uiState.value.weeklyAvailability
        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

        weekdays.forEach { day ->
            val dayConfig = weekly.first { it.dayOfWeek == day }
            assertTrue(dayConfig.isActive)
            assertEquals(LocalTime.of(7, 0), dayConfig.startTime)
            assertEquals(LocalTime.of(15, 0), dayConfig.endTime)
            assertEquals(45, dayConfig.slotMinutes)
        }
    }

    @Test
    fun `al actualizar disponibilidad de un dia especifico modifica solo ese dia`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val originalTuesday = viewModel.uiState.value.weeklyAvailability.first { it.dayOfWeek == DayOfWeek.TUESDAY }

        viewModel.onEvent(
            DoctorAppointmentsUiEvent.UpdateDayAvailability(
                day = DayOfWeek.WEDNESDAY,
                isActive = true,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(14, 0),
                slotMinutes = 60
            )
        )
        advanceUntilIdle()

        val updatedWednesday = viewModel.uiState.value.weeklyAvailability.first { it.dayOfWeek == DayOfWeek.WEDNESDAY }
        assertEquals(LocalTime.of(10, 0), updatedWednesday.startTime)
        assertEquals(LocalTime.of(14, 0), updatedWednesday.endTime)
        assertEquals(60, updatedWednesday.slotMinutes)

        // Martes permanece inalterado
        val currentTuesday = viewModel.uiState.value.weeklyAvailability.first { it.dayOfWeek == DayOfWeek.TUESDAY }
        assertEquals(originalTuesday.startTime, currentTuesday.startTime)
        assertEquals(originalTuesday.endTime, currentTuesday.endTime)
        assertEquals(originalTuesday.slotMinutes, currentTuesday.slotMinutes)
    }
}
