package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.WeeklyAvailability

interface DoctorRepository {
    suspend fun getDoctors(): Result<List<Doctor>>
    suspend fun getDoctorById(id: String): Result<Doctor?>
    suspend fun getDoctorAvailability(doctorId: String): Result<List<WeeklyAvailability>>
}
