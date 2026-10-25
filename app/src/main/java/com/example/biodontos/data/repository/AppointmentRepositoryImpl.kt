package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
private data class BusySlotDto(
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String? = null
)

@Serializable
private data class BusySlotsParams(
    @SerialName("p_doctor_id")
    val doctorId: String,
    @SerialName("p_date")
    val date: String
)

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
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getAppointmentsByDoctorId(doctorId: String): Result<List<Appointment>> {
        return try {
            val list = SupabaseClient.client.from("appointments")
                .select {
                    filter { eq("doctor_id", doctorId) }
                    order("start_time", order = Order.ASCENDING)
                }
                .decodeList<AppointmentDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getAppointmentsByDoctorAndDate(doctorId: String, date: String): Result<List<Appointment>> {
        return try {
            val list = SupabaseClient.client.from("appointments")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                        neq("status", "CANCELLED")
                    }
                }
                .decodeList<AppointmentDto>()
            val filtered = list
                .map { it.toDomain() }
                .filter { it.startTime.toString().startsWith(date) }
            Result.success(filtered)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getDoctorBusySlots(
        doctorId: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): Result<List<ClosedRange<LocalDateTime>>> {
        return try {
            val fromIso = from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val toIso = to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getDoctorBusySlots(doctorId: String, date: String): Result<List<String>> {
        return try {
            val list = SupabaseClient.client.postgrest.rpc(
                "get_doctor_busy_slots",
                BusySlotsParams(doctorId, date)
            ).decodeList<BusySlotDto>()
            Result.success(list.map { it.startTime })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Fallback defensivo si la migración de RPC aún no ha sido ejecutada en el backend
            try {
                val list = SupabaseClient.client.from("appointments")
                    .select {
                        filter {
                            eq("doctor_id", doctorId)
                            neq("status", "CANCELLED")
                        }
                    }
                    .decodeList<AppointmentDto>()
                val filtered = list
                    .filter { it.startTime.startsWith(date) }
                    .map { it.startTime }
                Result.success(filtered)
            } catch (fallbackEx: Exception) {
                if (fallbackEx is CancellationException) throw fallbackEx
                Result.failure(e)
            }
        }
    }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime
    ): Result<Unit> {
        return try {
            val startIso = newStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endIso = newEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: String,
        newEndTime: String?
    ): Result<Unit> {
        return try {
            SupabaseClient.client.from("appointments").update(
                {
                    set("start_time", newStartTime)
                    if (newEndTime != null) {
                        set("end_time", newEndTime)
                    }
                    set("status", "SCHEDULED")
                    set("confirmation_status", false)
                }
            ) {
                filter { eq("id", appointmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun cancelAppointment(appointmentId: String, reason: String?): Result<Unit> {
        return try {
            SupabaseClient.client.from("appointments").update(
                {
                    set("status", "CANCELLED")
                    set("confirmation_status", false)
                    if (!reason.isNullOrBlank()) {
                        set("reason", reason)
                    }
                }
            ) {
                filter { eq("id", appointmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
