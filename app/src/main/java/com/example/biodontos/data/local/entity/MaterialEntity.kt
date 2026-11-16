package com.example.biodontos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val doctorId: String? = null,
    val name: String,
    val invimaRegistration: String,
    val batchLotNumber: String,
    val manufacturingDate: String,
    val expirationDate: String,
    val isActive: Boolean = true
)
