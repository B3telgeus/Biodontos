package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient as AppSupabaseClient
import com.example.biodontos.data.dto.DoctorAvailabilityDto
import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.data.dto.toDto
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoctorRepositoryImpl(
    private val supabaseClient: SupabaseClient = AppSupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    internal var availabilityQuery: (suspend (doctorId: String) -> List<DoctorAvailabilityDto>)? = null
) : DoctorRepository {

    override suspend fun getDoctors(): Result<List<Doctor>> {
        return withContext(ioDispatcher) {
            try {
                val list = supabaseClient.postgrest.from("doctors")
                    .select {
                        order("first_name", order = Order.ASCENDING)
                    }
                    .decodeList<DoctorDto>()
                Result.success(list.map { it.toDomain() })
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun getDoctorById(id: String): Result<Doctor?> {
        return withContext(ioDispatcher) {
            try {
                val dto = supabaseClient.postgrest.from("doctors")
                    .select {
                        filter {
                            or {
                                eq("id", id)
                                eq("user_id", id)
                            }
                        }
                    }
                    .decodeSingleOrNull<DoctorDto>()
                Result.success(dto?.toDomain())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun getDoctorByUserId(userId: String): Result<Doctor?> {
        return withContext(ioDispatcher) {
            try {
                val dto = supabaseClient.postgrest.from("doctors")
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<DoctorDto>()
                Result.success(dto?.toDomain())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun getDoctorAvailability(doctorId: String): Result<List<WeeklyAvailability>> {
        return withContext(ioDispatcher) {
            try {
                val list = availabilityQuery?.invoke(doctorId)
                    ?: supabaseClient.postgrest.from("doctor_availability")
                        .select {
                            filter {
                                eq("doctor_id", doctorId)
                            }
                            order("day_of_week", order = Order.ASCENDING)
                        }
                        .decodeList<DoctorAvailabilityDto>()

                if (list.isEmpty()) {
                    Result.success(emptyList())
                } else {
                    Result.success(list.map { it.toDomain() })
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun saveDoctorAvailability(
        doctorId: String,
        availabilities: List<WeeklyAvailability>
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                supabaseClient.postgrest.from("doctor_availability")
                    .delete {
                        filter {
                            eq("doctor_id", doctorId)
                        }
                    }

                val dtos = availabilities.map { it.toDto() }
                if (dtos.isNotEmpty()) {
                    supabaseClient.postgrest.from("doctor_availability").insert(dtos)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun searchDoctors(
        query: String,
        specialty: String?,
        limit: Int,
        offset: Int
    ): Result<List<Doctor>> {
        return withContext(ioDispatcher) {
            try {
                val list = supabaseClient.postgrest.from("doctors")
                    .select {
                        order("first_name", order = Order.ASCENDING)
                        range(offset.toLong(), (offset + limit - 1).toLong())
                        filter {
                            if (!specialty.isNullOrBlank() && specialty != "Todas" && specialty != "Todos") {
                                eq("specialty", specialty)
                            }
                            if (query.isNotBlank()) {
                                ilike("first_name", "%${query.trim()}%")
                            }
                        }
                    }
                    .decodeList<DoctorDto>()
                Result.success(list.map { it.toDomain() })
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDoctorProfile(doctor: Doctor): Result<Doctor> {
        return withContext(ioDispatcher) {
            try {
                executeDoctorUpdate(doctor, includeOptionalFields = true)
                Result.success(doctor)
            } catch (e: Exception) {
                handleUpdateProfileError(doctor, e)
            }
        }
    }

    private suspend fun executeDoctorUpdate(doctor: Doctor, includeOptionalFields: Boolean) {
        val docId = doctor.id.value
        val targetUserId = doctor.userId.takeUnless { it.isNullOrBlank() } ?: docId

        supabaseClient.postgrest.from("doctors").update(
            {
                set("first_name", doctor.firstName.trim())
                set("last_name", doctor.lastName.trim())
                set("specialty", doctor.specialty.trim())
                set("license_number", doctor.licenseNumber.trim())
                set("phone", doctor.phone?.trim()?.ifEmpty { null })
                if (includeOptionalFields) {
                    set("bio", doctor.bio?.trim()?.ifEmpty { null })
                    set("photo_url", doctor.photoUrl?.trim()?.ifEmpty { null })
                    set("clinic_address", doctor.clinicAddress?.trim()?.ifEmpty { null })
                    set("services", doctor.services?.trim()?.ifEmpty { null })
                }
            }
        ) {
            filter {
                or {
                    eq("id", docId)
                    eq("user_id", targetUserId)
                }
            }
        }
    }

    private suspend fun handleUpdateProfileError(doctor: Doctor, e: Exception): Result<Doctor> {
        if (e is CancellationException) throw e
        val isMissingColumn = e is RestException && (e.error == "42703" || e.error == "PGRST204")
        if (!isMissingColumn) {
            return Result.failure(e)
        }
        return try {
            executeDoctorUpdate(doctor, includeOptionalFields = false)
            Result.success(doctor.copy(bio = null, photoUrl = null, clinicAddress = null, services = null))
        } catch (fallbackEx: Exception) {
            if (fallbackEx is CancellationException) throw fallbackEx
            Result.failure(fallbackEx)
        }
    }
}
