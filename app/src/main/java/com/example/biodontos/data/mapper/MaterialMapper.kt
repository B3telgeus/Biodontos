package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import java.time.Instant
import java.time.LocalDate

fun ClinicalMaterialDto.toDomain(): ClinicalMaterial = ClinicalMaterial(
    id = MaterialId(id?.ifBlank { null } ?: java.util.UUID.randomUUID().toString()),
    doctorId = doctorId?.ifBlank { null }?.let { DoctorId(it) },
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = runCatching { LocalDate.parse(manufacturingDate.take(10)) }.getOrDefault(LocalDate.now()),
    expirationDate = runCatching { LocalDate.parse(expirationDate.take(10)) }.getOrDefault(LocalDate.now().plusYears(1)),
    isActive = isActive,
    createdAt = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    updatedAt = updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
)

fun ClinicalMaterial.toDto(): ClinicalMaterialDto = ClinicalMaterialDto(
    id = id.value.ifBlank { null },
    doctorId = doctorId?.value?.ifBlank { null },
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = manufacturingDate.toString(),
    expirationDate = expirationDate.toString(),
    isActive = isActive,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString()
)
