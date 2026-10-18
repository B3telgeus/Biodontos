package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Doctor

interface DoctorRepository {
    suspend fun getDoctors(): Result<List<Doctor>>
    suspend fun getDoctorById(id: String): Result<Doctor?>
    suspend fun getDoctorByUserId(userId: String): Result<Doctor?>
}
