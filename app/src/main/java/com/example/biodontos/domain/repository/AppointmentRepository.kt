package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Appointment

interface AppointmentRepository {
    suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit>
    suspend fun createAppointment(appointment: Appointment): Result<Appointment>
    suspend fun getAppointmentsByPatientId(patientId: String): Result<List<Appointment>>
}
