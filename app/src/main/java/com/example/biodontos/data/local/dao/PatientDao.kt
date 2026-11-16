package com.example.biodontos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.biodontos.data.local.entity.PatientEntity

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<PatientEntity>)

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: String): PatientEntity?

    @Query(
        """SELECT * FROM patients WHERE (:query = '' OR dniCedula LIKE '%' || :query || '%' OR firstName LIKE '%' || :query || '%' OR firstLastName LIKE '%' || :query || '%' OR (secondLastName IS NOT NULL AND secondLastName LIKE '%' || :query || '%'))"""
    )
    suspend fun searchPatients(query: String): List<PatientEntity>

    @Delete
    suspend fun delete(patient: PatientEntity)
}
