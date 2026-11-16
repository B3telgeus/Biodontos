package com.example.biodontos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.biodontos.data.local.entity.MaterialEntity

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(materials: List<MaterialEntity>)

    @Query("SELECT * FROM materials WHERE isActive = 1")
    suspend fun getActiveMaterials(): List<MaterialEntity>
}
