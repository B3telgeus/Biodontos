package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.WeeklyAvailability

interface DoctorRepository {
    suspend fun getDoctors(): Result<List<Doctor>>
    suspend fun getDoctorById(id: String): Result<Doctor?>
    suspend fun getDoctorByUserId(userId: String): Result<Doctor?>
    suspend fun getDoctorAvailability(doctorId: String): Result<List<WeeklyAvailability>>
    suspend fun saveDoctorAvailability(
        doctorId: String,
        availabilities: List<WeeklyAvailability>
    ): Result<Unit>
    suspend fun searchDoctors(
        query: String = "",
        specialty: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Doctor>>
}
