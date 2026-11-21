package com.example.biodontos.fakes

import androidx.test.filters.SmallTest
import app.cash.turbine.test
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.model.Patient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@SmallTest
class InMemoryFakesVerificationTest {

    @Test
    fun fakePatientRepository_crudAndReactiveStateFlowEmitInOrder() = runTest {
        val repository = FakePatientRepository()
        val initialPatient = Patient(
            id = "",
            userId = "usr-1",
            dniCedula = "1020304050",
            firstName = "Valentina",
            firstLastName = "Restrepo",
            phone = "3104567890",
            consentSigned = false
        )

        repository.patientsFlow.test {
            assertThat(awaitItem()).isEmpty()

            val saveResult = repository.savePatient(initialPatient)
            assertThat(saveResult.isSuccess).isTrue()

            val afterSave = awaitItem()
            assertThat(afterSave).hasSize(1)
            val savedId = afterSave.first().id
            assertThat(savedId).isNotEmpty()

            val updated = afterSave.first().copy(consentSigned = true)
            val upsertResult = repository.createOrUpdatePatient(updated)
            assertThat(upsertResult.getOrThrow().consentSigned).isTrue()

            val afterUpdate = awaitItem()
            assertThat(afterUpdate).hasSize(1)
            assertThat(afterUpdate.first().consentSigned).isTrue()

            cancelAndIgnoreRemainingEvents()
        }

        val allPatients = repository.getPatients().getOrThrow()
        assertThat(allPatients).hasSize(1)
        val storedId = allPatients.first().id
        assertThat(repository.getPatientById(storedId).getOrThrow()?.fullName).isEqualTo("Valentina Restrepo")
        assertThat(repository.getPatientByUserId("usr-1").getOrThrow()?.id).isEqualTo(storedId)

        repository.failNextWith(IllegalStateException("Error simulado"))
        assertThat(repository.getPatients().isFailure).isTrue()
        assertThat(repository.getPatients().isSuccess).isTrue()
    }

    @Test
    fun fakeAppointmentRepository_crudRescheduleCancelAndBusySlotsWorkReactively() = runTest {
        val repository = FakeAppointmentRepository()
        val slotStart = LocalDateTime.of(2026, 11, 20, 9, 0)
        val slotEnd = LocalDateTime.of(2026, 11, 20, 9, 30)
        val appointment = Appointment(
            id = "apt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = slotStart,
            endTime = slotEnd,
            status = AppointmentStatus.PENDING,
            isConfirmed = false,
            reason = "Valoración inicial"
        )

        repository.appointmentsFlow.test {
            assertThat(awaitItem()).isEmpty()

            val created = repository.createAppointment(appointment).getOrThrow()
            assertThat(created.id).isEqualTo("apt-1")
            assertThat(awaitItem()).hasSize(1)

            val conflictResult = repository.createAppointment(
                appointment.copy(id = "apt-conflict", patientId = "pat-2")
            )
            assertThat(conflictResult.isFailure).isTrue()

            repository.updateConfirmationStatus("apt-1", isConfirmed = true).getOrThrow()
            assertThat(awaitItem().first().isConfirmed).isTrue()

            val busyIso = repository.getDoctorBusySlots("doc-1", "2026-11-20").getOrThrow()
            assertThat(busyIso).containsExactly("2026-11-20T09:00:00")

            val newStart = LocalDateTime.of(2026, 11, 21, 10, 30)
            val newEnd = LocalDateTime.of(2026, 11, 21, 11, 0)
            repository.rescheduleAppointment("apt-1", newStart, newEnd).getOrThrow()
            val afterReschedule = awaitItem().first()
            assertThat(afterReschedule.status).isEqualTo(AppointmentStatus.SCHEDULED)
            assertThat(afterReschedule.isConfirmed).isFalse()
            assertThat(afterReschedule.startTime).isEqualTo(newStart)

            val busyRange = repository.getDoctorBusySlots(
                doctorId = "doc-1",
                from = LocalDateTime.of(2026, 11, 21, 0, 0),
                to = LocalDateTime.of(2026, 11, 21, 23, 59)
            ).getOrThrow()
            assertThat(busyRange).containsExactly(newStart..newEnd)

            repository.cancelAppointment("apt-1", "Paciente reprograma").getOrThrow()
            val afterCancel = awaitItem().first()
            assertThat(afterCancel.status).isEqualTo(AppointmentStatus.CANCELLED)
            assertThat(afterCancel.reason).isEqualTo("Paciente reprograma")

            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repository.getDoctorBusySlots("doc-1", "2026-11-21").getOrThrow()).isEmpty()
        assertThat(repository.getAppointmentsByPatientId("pat-1").getOrThrow()).hasSize(1)
        assertThat(repository.getAppointmentsByDoctorId("doc-1").getOrThrow()).hasSize(1)
        assertThat(repository.getAppointmentsByDoctorAndDate("doc-1", "2026-11-21").getOrThrow()).isEmpty()
        assertThat(repository.getAppointmentById("apt-1").getOrThrow()?.status)
            .isEqualTo(AppointmentStatus.CANCELLED)
    }

    @Test
    fun fakeMaterialRepository_crudDuplicateGuardAndActivationToggleWorkReactively() = runTest {
        val repository = FakeMaterialRepository()
        val doctorId = DoctorId("doc-1")
        val refDate = LocalDate.of(2026, 11, 20)
        val resin = ClinicalMaterial(
            id = MaterialId("mat-1"),
            doctorId = doctorId,
            name = "Resina Filtek Z350",
            invimaRegistration = "INVIMA 2022DM-0014512",
            batchLotNumber = "LOT-2026-01",
            manufacturingDate = refDate.minusMonths(3),
            expirationDate = refDate.plusYears(1),
            isActive = true
        )

        repository.materialsFlow.test {
            assertThat(awaitItem()).isEmpty()

            repository.saveMaterial(resin).getOrThrow()
            assertThat(awaitItem()).hasSize(1)

            val duplicateResult = repository.saveMaterial(
                resin.copy(id = MaterialId("mat-2"), name = "Resina Duplicada")
            )
            assertThat(duplicateResult.isFailure).isTrue()

            repository.setMaterialActive(MaterialId("mat-1"), isActive = false).getOrThrow()
            assertThat(awaitItem().first().isActive).isFalse()

            assertThat(repository.getMaterials(includeInactive = false, doctorId = doctorId).getOrThrow())
                .isEmpty()
            assertThat(repository.getMaterials(includeInactive = true, doctorId = doctorId).getOrThrow())
                .hasSize(1)

            repository.setMaterialActive("mat-1", isActive = true).getOrThrow()
            assertThat(awaitItem().first().isActive).isTrue()

            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repository.getMaterialById("mat-1").getOrThrow()?.name).isEqualTo("Resina Filtek Z350")
    }
}
