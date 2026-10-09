package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class AppointmentRepositoryImpl : AppointmentRepository {

    override suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit> {
        return try {
            SupabaseClient.client.from("appointments").update(
                {
                    set("confirmation_status", isConfirmed)
                }
            ) {
                filter { eq("id", appointmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createAppointment(appointment: Appointment): Result<Appointment> {
        return try {
            val dto = appointment.toDto()
            val savedDto = SupabaseClient.client.from("appointments").insert(dto) {
                select()
            }.decodeSingle<AppointmentDto>()
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppointmentsByPatientId(patientId: String): Result<List<Appointment>> {
        return try {
            val list = SupabaseClient.client.from("appointments")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("start_time", order = Order.ASCENDING)
                }
                .decodeList<AppointmentDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
