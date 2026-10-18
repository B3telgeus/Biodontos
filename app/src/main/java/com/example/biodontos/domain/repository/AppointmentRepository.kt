package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Appointment

interface AppointmentRepository {
    suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit>
    suspend fun createAppointment(appointment: Appointment): Result<Appointment>
    suspend fun getAppointmentsByPatientId(patientId: String): Result<List<Appointment>>
    suspend fun getAppointmentsByDoctorId(doctorId: String): Result<List<Appointment>>
    suspend fun getAppointmentsByDoctorAndDate(doctorId: String, date: String): Result<List<Appointment>>
    suspend fun getDoctorBusySlots(doctorId: String, date: String): Result<List<String>>
    suspend fun rescheduleAppointment(appointmentId: String, newStartTime: String, newEndTime: String? = null): Result<Unit>
    suspend fun cancelAppointment(appointmentId: String, reason: String? = null): Result<Unit>
}

