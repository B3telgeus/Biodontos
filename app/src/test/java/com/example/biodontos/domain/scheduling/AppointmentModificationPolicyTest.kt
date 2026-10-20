package com.example.biodontos.domain.scheduling

import arrow.core.Either
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AppointmentModificationPolicyTest {

    private val policy = AppointmentModificationPolicy(minHoursBefore = 2)

    @Test
    fun `canModify returns Right when appointment is more than 2 hours away`() {
        val now = LocalDateTime.of(2026, 10, 19, 8, 0)
        val appointment = Appointment(
            id = "app-1",
            patientId = "pat-1",
            startTime = LocalDateTime.of(2026, 10, 19, 12, 0), // 4 horas después
            status = AppointmentStatus.CONFIRMED
        )

        val result = policy.canModify(appointment, now)
        assertTrue(result is Either.Right)
    }

    @Test
    fun `canModify returns ModificationWindowClosed when less than 2 hours remain`() {
        val now = LocalDateTime.of(2026, 10, 19, 8, 0)
        val appointment = Appointment(
            id = "app-1",
            patientId = "pat-1",
            startTime = LocalDateTime.of(2026, 10, 19, 9, 30), // 1.5 horas después
            status = AppointmentStatus.CONFIRMED
        )

        val result = policy.canModify(appointment, now)
        assertTrue(result is Either.Left)
        assertEquals(AppointmentError.ModificationWindowClosed(2), (result as Either.Left).value)
    }

    @Test
    fun `canModify returns AlreadyCancelled when appointment is already cancelled`() {
        val now = LocalDateTime.of(2026, 10, 19, 8, 0)
        val appointment = Appointment(
            id = "app-1",
            patientId = "pat-1",
            startTime = LocalDateTime.of(2026, 10, 20, 12, 0),
            status = AppointmentStatus.CANCELLED
        )

        val result = policy.canModify(appointment, now)
        assertTrue(result is Either.Left)
        assertEquals(AppointmentError.AlreadyCancelled, (result as Either.Left).value)
    }
}
