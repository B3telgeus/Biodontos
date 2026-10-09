package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.PeriodontalExamination

interface PeriodontalRepository {
    suspend fun getExaminationByRecordId(recordId: String): Result<PeriodontalExamination?>
    suspend fun saveExamination(examination: PeriodontalExamination): Result<PeriodontalExamination>
}
