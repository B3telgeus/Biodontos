package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId

interface MaterialRepository {
    suspend fun getMaterials(
        includeInactive: Boolean = false,
        doctorId: DoctorId? = null
    ): Result<List<ClinicalMaterial>>

    suspend fun getMaterialById(id: MaterialId): Result<ClinicalMaterial?>

    suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial>

    suspend fun setMaterialActive(id: MaterialId, isActive: Boolean): Result<Unit>

    suspend fun getMaterialById(id: String): Result<ClinicalMaterial?> =
        getMaterialById(MaterialId(id))

    suspend fun setMaterialActive(id: String, isActive: Boolean): Result<Unit> =
        setMaterialActive(MaterialId(id), isActive)
}
