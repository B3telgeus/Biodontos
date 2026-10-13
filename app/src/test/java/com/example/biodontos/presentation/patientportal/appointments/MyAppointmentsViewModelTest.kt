package com.example.biodontos.presentation.patientportal.appointments

import app.cash.turbine.test
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
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

    private val pastAppt = Appointment(
        id = "appt-past",
        patientId = "patient-1",
        doctorId = "doc-1",
        startTime = testNow.minusDays(5),
        status = AppointmentStatus.COMPLETED,
        reason = "Limpieza"
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
        coEvery { getCurrentPatientUseCase() } returns Result.success(testPatient)
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(testDoctor))
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(upcomingAppt, pastAppt)
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
    fun `initial load partitions upcoming and past appointments correctly`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("appt-future", state.upcomingAppointments[0].id)
        assertEquals(1, state.pastAppointments.size)
        assertEquals("appt-past", state.pastAppointments[0].id)
    }

    @Test
    fun `select tab updates selectedTab in uiState`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.PAST))
        assertEquals(AppointmentTab.PAST, viewModel.uiState.value.selectedTab)
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
    fun `confirm cancel calls usecase and updates state`() = runTest {
        coEvery { cancelAppointmentUseCase("appt-future") } returns Unit.right()
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(pastAppt)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(upcomingAppt))

        viewModel.effects.test {
            viewModel.onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment)
            val toast = awaitItem()
            assert(toast is MyAppointmentsUiEffect.ShowToast)
        }

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.appointmentToCancel)
    }
}
