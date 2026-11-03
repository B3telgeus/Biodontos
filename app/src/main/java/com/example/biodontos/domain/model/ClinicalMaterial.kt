package com.example.biodontos.domain.model

import java.time.Instant
import java.time.LocalDate

private fun parseManufacturingDate(dateStr: String): LocalDate {
    return try {
        LocalDate.parse(dateStr.take(10))
    } catch (_: Exception) {
        LocalDate.now()
    }
}

private fun parseExpirationDate(dateStr: String): LocalDate {
    return try {
        LocalDate.parse(dateStr.take(10))
    } catch (_: Exception) {
        LocalDate.now().plusYears(1)
    }
}

data class ClinicalMaterial(
    val id: MaterialId = MaterialId.generate(),
    val doctorId: DoctorId? = null,
    val name: String,
    val invimaRegistration: String,
    val batchLotNumber: String,
    val manufacturingDate: LocalDate,
    val expirationDate: LocalDate,
    val isActive: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    constructor(
        id: String = java.util.UUID.randomUUID().toString(),
        doctorId: String? = null,
        name: String,
        invimaRegistration: String,
        batchLotNumber: String,
        manufacturingDate: String,
        expirationDate: String,
        isActive: Boolean = true,
        createdAt: Instant? = null,
        updatedAt: Instant? = null
    ) : this(
        id = MaterialId(id.ifBlank { java.util.UUID.randomUUID().toString() }),
        doctorId = doctorId?.ifBlank { null }?.let { DoctorId(it) },
        name = name,
        invimaRegistration = invimaRegistration,
        batchLotNumber = batchLotNumber,
        manufacturingDate = parseManufacturingDate(manufacturingDate),
        expirationDate = parseExpirationDate(expirationDate),
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    val rawId: String get() = id.value
    val rawManufacturingDate: String get() = manufacturingDate.toString()
    val rawExpirationDate: String get() = expirationDate.toString()

    fun isExpired(referenceDate: LocalDate = LocalDate.now()): Boolean {
        return expirationDate.isBefore(referenceDate)
    }

    fun isNearExpiration(referenceDate: LocalDate = LocalDate.now(), thresholdDays: Long = 30): Boolean {
        return !isExpired(referenceDate) && !expirationDate.isAfter(referenceDate.plusDays(thresholdDays))
    }
}
