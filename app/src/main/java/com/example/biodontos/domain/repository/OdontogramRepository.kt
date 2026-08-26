package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface

interface OdontogramRepository {
    suspend fun getOdontogram(patientId: String): Result<List<Tooth>>
    suspend fun updateToothState(
        patientId: String,
        toothNumber: Int,
        surface: ToothSurface,
        condition: ToothCondition,
        comment: String?
    ): Result<Unit>
}
