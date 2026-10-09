package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.ClinicalMaterial

interface MaterialRepository {
    suspend fun getMaterials(): Result<List<ClinicalMaterial>>
    suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial>
}
