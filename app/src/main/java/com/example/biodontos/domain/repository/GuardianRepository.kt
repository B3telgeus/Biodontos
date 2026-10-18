package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Guardian

interface GuardianRepository {
    suspend fun getGuardianById(id: String): Result<Guardian?>
    suspend fun saveGuardian(guardian: Guardian): Result<Guardian>
}
