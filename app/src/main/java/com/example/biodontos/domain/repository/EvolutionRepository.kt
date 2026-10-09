package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Evolution

interface EvolutionRepository {
    suspend fun getEvolutionsByRecordId(recordId: String): Result<List<Evolution>>
    suspend fun saveEvolution(evolution: Evolution): Result<Evolution>
}
