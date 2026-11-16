package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdmissionClinicalUseCasesTest {

    private val evolutionRepository: EvolutionRepository = mockk()
    private val clinicalRecordRepository: ClinicalRecordRepository = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk()

    @Test
    fun `SaveAdmissionEvolutionAndCloseUseCase delegates atomic persistence to evolutionRepository and closes record when requested`() = runTest {
        val sampleEvo = Evolution(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "12345678-1234-1234-1234-123456789abc",
            date = "2026-10-25",
            procedureTime = "10:00:00",
            description = "Evolución inicial"
        )
        coEvery {
            evolutionRepository.saveEvolution(
                evolution = sampleEvo,
                closeRecord = true,
                closureSummary = "Resumen final",
                closureSignatureUrl = "sig_pat"
            )
        } returns Result.success(sampleEvo)

        val useCase = SaveAdmissionEvolutionAndCloseUseCase(evolutionRepository, clinicalRecordRepository)
        val result = useCase(
            evolution = sampleEvo,
            closeRecord = true,
            closureSummary = "Resumen final",
            closureSignatureUrl = "sig_pat"
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            evolutionRepository.saveEvolution(
                evolution = sampleEvo,
                closeRecord = true,
                closureSummary = "Resumen final",
                closureSignatureUrl = "sig_pat"
            )
        }
        coVerify(exactly = 1) {
            clinicalRecordRepository.closeRecord(
                recordId = "rec-1",
                doctorId = "12345678-1234-1234-1234-123456789abc",
                summary = "Resumen final",
                signatureUrl = "sig_pat"
            )
        }
    }

    @Test
    fun `SaveAdmissionEvolutionAndCloseUseCase does not close record when saveEvolution fails`() = runTest {
        val sampleEvo = Evolution(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "12345678-1234-1234-1234-123456789abc",
            date = "2026-10-25",
            procedureTime = "10:00:00",
            description = "Evolución inicial"
        )
        coEvery {
            evolutionRepository.saveEvolution(any(), any(), any(), any())
        } returns Result.failure(RuntimeException("RPC failure"))

        val useCase = SaveAdmissionEvolutionAndCloseUseCase(evolutionRepository, clinicalRecordRepository)
        val result = useCase(
            evolution = sampleEvo,
            closeRecord = true,
            closureSummary = "Resumen final",
            closureSignatureUrl = "sig_pat"
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { clinicalRecordRepository.closeRecord(any(), any(), any(), any()) }
    }

    @Test
    fun `ScheduleAdmissionFollowUpUseCase books requested slot or falls back when busy`() = runTest {
        coEvery {
            appointmentRepository.getDoctorBusySlots("doc-1", "2026-11-10")
        } returns Result.success(listOf("2026-11-10T10:00:00"))
        coEvery {
            appointmentRepository.createAppointment(any())
        } answers { Result.success(firstArg<Appointment>().copy(id = "appt-follow")) }

        val useCase = ScheduleAdmissionFollowUpUseCase(appointmentRepository)
        val note = useCase(
            patientId = "pat-1",
            doctorId = "doc-1",
            nextAppointmentDate = "2026-11-10",
            nextAppointmentTime = "10:00",
            nextAppointmentReason = "Control"
        )

        assertEquals(" Cita agendada para el 2026-11-10 a las 08:00.", note)
    }

    @Test
    fun `ScheduleAdmissionFollowUpUseCase returns empty string when date is blank`() = runTest {
        val useCase = ScheduleAdmissionFollowUpUseCase(appointmentRepository)
        val note = useCase(
            patientId = "pat-1",
            doctorId = "doc-1",
            nextAppointmentDate = "   ",
            nextAppointmentTime = "10:00",
            nextAppointmentReason = ""
        )
        assertEquals("", note)
    }
}
