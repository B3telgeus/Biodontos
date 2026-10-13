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

    override suspend fun getAppointmentById(appointmentId: String): Result<Appointment?> {
        return try {
            val dto = SupabaseClient.client.from("appointments")
                .select { filter { eq("id", appointmentId) } }
                .decodeSingleOrNull<AppointmentDto>()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDoctorBusySlots(
        doctorId: String,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime
    ): Result<List<ClosedRange<java.time.LocalDateTime>>> {
        return try {
            val fromIso = from.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val toIso = to.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val list = SupabaseClient.client.from("appointments")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                        neq("status", "CANCELLED")
                        gte("start_time", fromIso)
                        lte("start_time", toIso)
                    }
                }
                .decodeList<AppointmentDto>()
            val intervals = list.map {
                val start = Appointment.parseDateTime(it.startTime)
                val end = it.endTime?.let { endStr -> Appointment.parseDateTime(endStr) } ?: start.plusMinutes(30)
                start..end
            }
            Result.success(intervals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: java.time.LocalDateTime,
        newEndTime: java.time.LocalDateTime
    ): Result<Unit> {
        return try {
            val startIso = newStartTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endIso = newEndTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            SupabaseClient.client.from("appointments").update(
                {
                    set("start_time", startIso)
                    set("end_time", endIso)
                    set("confirmation_status", false)
                    set("status", "SCHEDULED")
                }
            ) {
                filter { eq("id", appointmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return try {
            SupabaseClient.client.from("appointments").update(
                {
                    set("status", "CANCELLED")
                    set("confirmation_status", false)
                }
            ) {
                filter { eq("id", appointmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
