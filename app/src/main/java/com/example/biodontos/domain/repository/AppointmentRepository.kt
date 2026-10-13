package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Appointment

interface AppointmentRepository {
    suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit>
    suspend fun createAppointment(appointment: Appointment): Result<Appointment>
    suspend fun getAppointmentsByPatientId(patientId: String): Result<List<Appointment>>
    suspend fun getAppointmentById(appointmentId: String): Result<Appointment?>
    suspend fun getDoctorBusySlots(
        doctorId: String,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime
    ): Result<List<ClosedRange<java.time.LocalDateTime>>>
    suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: java.time.LocalDateTime,
        newEndTime: java.time.LocalDateTime
    ): Result<Unit>
    suspend fun cancelAppointment(appointmentId: String): Result<Unit>
}
