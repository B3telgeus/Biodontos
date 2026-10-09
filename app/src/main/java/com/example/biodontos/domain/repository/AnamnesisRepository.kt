package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Anamnesis

interface AnamnesisRepository {
    suspend fun getAnamnesisByPatientId(patientId: String): Result<Anamnesis?>
    suspend fun saveAnamnesis(anamnesis: Anamnesis): Result<Anamnesis>
}
