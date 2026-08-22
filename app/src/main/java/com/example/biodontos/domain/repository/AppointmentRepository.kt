package com.example.biodontos.domain.repository

interface AppointmentRepository {
    suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit>
}
