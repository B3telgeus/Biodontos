package com.example.biodontos.domain.usecase.patient

import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.notifications.ConfirmAttendance1ClickUseCase
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemainingPatientUseCasesTest {

    private val authRepository: AuthRepository = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()

    @Test
    fun `GetCurrentPatientUseCase returns patient when active session has PATIENT role`() = runTest {
        val session = UserSession("usr-pat-1", "pat@biodontos.com", UserRole.PATIENT, "María")
        val patient = Patient(id = "pat-1", userId = "usr-pat-1", dniCedula = "10987654", firstName = "María", lastName = "Gómez")

        coEvery { authRepository.getCurrentSession() } returns session
        coEvery { patientRepository.getPatientByUserId("usr-pat-1") } returns Result.success(patient)

        val useCase = GetCurrentPatientUseCase(authRepository, patientRepository)
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(patient, result.getOrNull())
    }

    @Test
    fun `GetCurrentPatientUseCase returns failure when user role is not PATIENT`() = runTest {
        val doctorSession = UserSession("usr-doc-1", "doc@biodontos.com", UserRole.DOCTOR, "Dr. Carlos")
        coEvery { authRepository.getCurrentSession() } returns doctorSession

        val useCase = GetCurrentPatientUseCase(authRepository, patientRepository)
        val result = useCase()

        assertTrue(result.isFailure)
    }

    @Test
    fun `SavePatientUseCase saves patient in repository`() = runTest {
        val patient = Patient(id = "pat-1", dniCedula = "10987654", firstName = "María", lastName = "Gómez")
        coEvery { patientRepository.savePatient(patient) } returns Result.success(Unit)

        val useCase = SavePatientUseCase(patientRepository)
        val result = useCase(patient)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `ConfirmAttendance1ClickUseCase updates confirmation status to true`() = runTest {
        coEvery { appointmentRepository.updateConfirmationStatus("appt-99", true) } returns Result.success(Unit)

        val useCase = ConfirmAttendance1ClickUseCase(appointmentRepository)
        val result = useCase("appt-99")

        assertTrue(result.isSuccess)
    }
}
