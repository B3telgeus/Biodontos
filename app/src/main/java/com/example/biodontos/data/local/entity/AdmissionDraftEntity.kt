package com.example.biodontos.data.local.entity

import androidx.room.Entity

@Entity(tableName = "admission_drafts", primaryKeys = ["doctorId", "patientId"])
data class AdmissionDraftEntity(
    val doctorId: String,
    val patientId: String,
    val currentStep: Int,
    val draftJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
