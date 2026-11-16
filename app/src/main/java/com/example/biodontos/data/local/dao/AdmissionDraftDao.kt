package com.example.biodontos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.biodontos.data.local.entity.AdmissionDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdmissionDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: AdmissionDraftEntity)

    @Query("SELECT * FROM admission_drafts WHERE doctorId = :doctorId AND patientId = :patientId")
    fun getDraft(doctorId: String, patientId: String): Flow<AdmissionDraftEntity?>

    @Query("DELETE FROM admission_drafts WHERE doctorId = :doctorId AND patientId = :patientId")
    suspend fun deleteDraft(doctorId: String, patientId: String)
}
