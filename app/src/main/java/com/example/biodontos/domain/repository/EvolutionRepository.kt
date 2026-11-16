package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Evolution

interface EvolutionRepository {
    suspend fun getEvolutionsByRecordId(recordId: String): Result<List<Evolution>>
    suspend fun saveEvolution(
        evolution: Evolution,
        closeRecord: Boolean = false,
        closureSummary: String? = null,
        closureSignatureUrl: String? = null
    ): Result<Evolution>
    suspend fun voidEvolution(evolutionId: String, reason: String): Result<Unit>
    suspend fun saveClarification(
        originalEvolution: Evolution,
        clarificationText: String,
        doctorId: String,
        doctorName: String? = null
    ): Result<Evolution>
}
