package com.example.biodontos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val dniCedula: String,
    val firstName: String,
    val firstLastName: String,
    val secondLastName: String? = null,
    val phone: String,
    val email: String? = null,
    val birthDate: String? = null,
    val birthPlace: String? = null,
    val eps: String? = null,
    val bloodGroup: String? = null,
    val rhFactor: String? = null,
    val consentSigned: Boolean = false
)
