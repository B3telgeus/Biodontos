package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import kotlinx.coroutines.CancellationException

class ScheduleAdmissionFollowUpUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(
        patientId: String,
        doctorId: String,
        nextAppointmentDate: String,
        nextAppointmentTime: String,
        nextAppointmentReason: String
    ): String {
        if (nextAppointmentDate.isBlank()) return ""

        return try {
            var targetTime = nextAppointmentTime.ifBlank { "10:00" }.trim()
            if (targetTime.length == 5) {
                targetTime = "$targetTime:00"
            }

            val busySlots = appointmentRepository.getDoctorBusySlots(doctorId, nextAppointmentDate)
                .getOrDefault(emptyList())

            val requestedIso = "${nextAppointmentDate}T$targetTime"
            val isBusy = busySlots.any { busy ->
                busy.startsWith("${nextAppointmentDate}T${targetTime.take(5)}") || busy == requestedIso
            }

            if (isBusy) {
                val availableTime = CANDIDATE_TIMES.firstOrNull { candidate ->
                    val iso = "${nextAppointmentDate}T$candidate"
                    busySlots.none { busy -> busy.startsWith(iso.take(16)) }
                }
                availableTime?.let { targetTime = it }
            }

            val appointmentToTry = Appointment(
                patientId = patientId,
                doctorId = doctorId,
                startTime = Appointment.parseDateTime("${nextAppointmentDate}T$targetTime"),
                reason = nextAppointmentReason.ifBlank { DEFAULT_FOLLOW_UP_REASON },
                status = AppointmentStatus.SCHEDULED,
                isConfirmed = false
            )

            val primaryResult = appointmentRepository.createAppointment(appointmentToTry)
            if (primaryResult.isSuccess) {
                " Cita agendada para el $nextAppointmentDate a las ${targetTime.take(5)}."
            } else {
                val fallbackTime = CANDIDATE_TIMES.firstNotNullOfOrNull { candidate ->
                    if (candidate == targetTime) {
                        null
                    } else {
                        val fallbackAppt = appointmentToTry.copy(
                            startTime = Appointment.parseDateTime("${nextAppointmentDate}T$candidate")
                        )
                        val fallbackResult = appointmentRepository.createAppointment(fallbackAppt)
                        candidate.takeIf { fallbackResult.isSuccess }
                    }
                }
                if (fallbackTime != null) {
                    " Cita agendada para el $nextAppointmentDate a las ${fallbackTime.take(5)}."
                } else {
                    " (Nota: No se pudo agendar la próxima cita automáticamente por conflicto de horario)."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            " (Nota: No se pudo agendar la próxima cita automáticamente por conflicto de horario)."
        }
    }

    companion object {
        private const val DEFAULT_FOLLOW_UP_REASON = "Control y seguimiento post-tratamiento"
        private val CANDIDATE_TIMES = listOf(
            "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
            "11:00:00", "11:30:00", "12:00:00", "14:00:00", "14:30:00", "15:00:00",
            "15:30:00", "16:00:00", "16:30:00", "17:00:00"
        )
    }
}
