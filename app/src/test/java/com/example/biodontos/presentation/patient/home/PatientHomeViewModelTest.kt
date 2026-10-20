package com.example.biodontos.presentation.patient.home

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.appointment.*
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentSessionUseCase: GetCurrentSessionUseCase = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)

    private val getPatientAppointmentsUseCase by lazy { GetPatientAppointmentsUseCase(appointmentRepository) }
    private val getDoctorBusySlotsUseCase by lazy { GetDoctorBusySlotsUseCase(appointmentRepository) }
    private val bookAppointmentUseCase by lazy { BookAppointmentUseCase(appointmentRepository) }
    private val rescheduleAppointmentUseCase by lazy { RescheduleAppointmentUseCase(appointmentRepository) }
    private val cancelAppointmentUseCase by lazy { CancelAppointmentUseCase(appointmentRepository) }

    private val dummySession = UserSession(
        id = "patient-uid-1",
        email = "maria@test.com",
        role = UserRole.PATIENT,
        displayName = "María Pérez"
    )

    private val dummyPatient = Patient(
        id = "patient-uid-1",
        userId = "patient-uid-1",
        dniCedula = "12345678",
        firstName = "María",
        firstLastName = "Pérez",
        phone = "3001234567",
        email = "maria@test.com",
        consentSigned = true
    )

    private val dummyDoctor = Doctor(
        id = "doc-101",
        firstName = "Roberto",
        lastName = "Suárez",
        specialty = "Ortodoncia",
        email = "roberto@biodontos.com"
    )

    private val scheduledAppointment = Appointment(
        id = "apt-1",
        patientId = "patient-uid-1",
        doctorId = "doc-101",
        startTime = "2026-10-25 09:00:00",
        status = AppointmentStatus.SCHEDULED,
        reason = "Control de ortodoncia"
    )

    private val cancelledAppointment = Appointment(
        id = "apt-2",
        patientId = "patient-uid-1",
        doctorId = "doc-101",
        startTime = "2026-09-10 14:00:00",
        status = AppointmentStatus.CANCELLED,
        reason = "Incompatibilidad de horario"
    )

    @Before
    fun setup() {
        coEvery { getCurrentSessionUseCase() } returns dummySession
        coEvery { patientRepository.getPatientByUserId("patient-uid-1") } returns Result.success(dummyPatient)
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(dummyDoctor))
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-uid-1") } returns Result.success(
            listOf(scheduledAppointment, cancelledAppointment)
        )
        coEvery { appointmentRepository.getAppointmentsByDoctorAndDate(any(), any()) } returns Result.success(emptyList())
        coEvery { appointmentRepository.getDoctorBusySlots(any(), any()) } returns Result.success(emptyList())
    }

    private fun createViewModel(): PatientHomeViewModel {
        return PatientHomeViewModel(
            getCurrentSessionUseCase = getCurrentSessionUseCase,
            patientRepository = patientRepository,
            doctorRepository = doctorRepository,
            getPatientAppointmentsUseCase = getPatientAppointmentsUseCase,
            getDoctorBusySlotsUseCase = getDoctorBusySlotsUseCase,
            bookAppointmentUseCase = bookAppointmentUseCase,
            rescheduleAppointmentUseCase = rescheduleAppointmentUseCase,
            cancelAppointmentUseCase = cancelAppointmentUseCase,
            logoutUseCase = logoutUseCase
        )
    }

    @Test
    fun `initialization loads patient profile and segments appointments correctly`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("patient-uid-1", state.patientId)
        assertEquals("María Pérez", state.patientName)
        assertEquals(1, state.doctors.size)
        assertEquals("Dr(a). Roberto Suárez", state.doctors.first().fullName)

        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("apt-1", state.upcomingAppointments.first().id)
        assertNotNull(state.nextAppointment)
        assertEquals("apt-1", state.nextAppointment?.id)

        assertEquals(1, state.historyAppointments.size)
        assertEquals("apt-2", state.historyAppointments.first().id)
        assertEquals(AppointmentStatus.CANCELLED, state.historyAppointments.first().status)
    }

    @Test
    fun `booking flow computes available slots and creates appointment successfully`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { appointmentRepository.createAppointment(any()) } returns Result.success(
            Appointment(
                id = "apt-new",
                patientId = "patient-uid-1",
                doctorId = "doc-101",
                startTime = "2026-10-26 10:00:00",
                status = AppointmentStatus.SCHEDULED,
                reason = "Limpieza Dental"
            )
        )

        // Abrir modal de agendamiento
        viewModel.onEvent(PatientHomeEvent.OpenBookingDialog)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.bookingState)
        assertEquals("doc-101", viewModel.state.value.bookingState?.selectedDoctorId)

        // Seleccionar franja y motivo
        viewModel.onEvent(PatientHomeEvent.SelectBookingSlot("10:00 AM"))
        viewModel.onEvent(PatientHomeEvent.UpdateBookingReason("Limpieza Dental"))

        // Confirmar agendamiento
        viewModel.onEvent(PatientHomeEvent.ConfirmBooking)
        advanceUntilIdle()

        coVerify { appointmentRepository.createAppointment(any()) }
        assertNull(viewModel.state.value.bookingState)
        assertNotNull(viewModel.state.value.successDialogState)
        assertEquals("¡Solicitud de Cita Enviada!", viewModel.state.value.successDialogState?.title)
    }

    @Test
    fun `reschedule flow updates start time and presents success summary`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { appointmentRepository.rescheduleAppointment("apt-1", any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(PatientHomeEvent.OpenRescheduleDialog(scheduledAppointment))
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.rescheduleState)

        viewModel.onEvent(PatientHomeEvent.SelectRescheduleDate("2026-10-28"))
        viewModel.onEvent(PatientHomeEvent.SelectRescheduleSlot("02:00 PM"))

        viewModel.onEvent(PatientHomeEvent.ConfirmReschedule)
        advanceUntilIdle()

        coVerify {
            appointmentRepository.rescheduleAppointment(
                appointmentId = "apt-1",
                newStartTime = "2026-10-28T14:00:00-05:00",
                newEndTime = null
            )
        }
        assertNull(viewModel.state.value.rescheduleState)
        assertNotNull(viewModel.state.value.successDialogState)
        assertEquals("¡Cita Reagendada con Éxito!", viewModel.state.value.successDialogState?.title)
    }

    @Test
    fun `cancellation flow sends clinical cancellation with reason`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { appointmentRepository.cancelAppointment("apt-1", "Incompatibilidad de horario") } returns Result.success(Unit)

        viewModel.onEvent(PatientHomeEvent.OpenCancelDialog(scheduledAppointment))
        assertNotNull(viewModel.state.value.cancelConfirmState)

        viewModel.onEvent(PatientHomeEvent.SelectCancelChip("Incompatibilidad de horario"))
        viewModel.onEvent(PatientHomeEvent.ConfirmCancellation)
        advanceUntilIdle()

        coVerify { appointmentRepository.cancelAppointment("apt-1", "Incompatibilidad de horario") }
        assertNull(viewModel.state.value.cancelConfirmState)
    }

    @Test
    fun `logout executes LogoutUseCase and emits NavigateToLogin effect`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PatientHomeEvent.Logout)
        advanceUntilIdle()

        coVerify { logoutUseCase() }
        val effect = viewModel.effects.first()
        assertEquals(PatientHomeEffect.NavigateToLogin, effect)
    }
}
