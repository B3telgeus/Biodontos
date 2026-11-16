package com.example.biodontos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.biodontos.data.local.dao.AdmissionDraftDao
import com.example.biodontos.data.local.dao.MaterialDao
import com.example.biodontos.data.local.dao.PatientDao
import com.example.biodontos.data.local.entity.AdmissionDraftEntity
import com.example.biodontos.data.local.entity.MaterialEntity
import com.example.biodontos.data.local.entity.PatientEntity

@Database(
    entities = [
        PatientEntity::class,
        MaterialEntity::class,
        AdmissionDraftEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BiodontosDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun materialDao(): MaterialDao
    abstract fun admissionDraftDao(): AdmissionDraftDao

    companion object {
        @Volatile
        private var INSTANCE: BiodontosDatabase? = null

        fun getInstance(context: Context): BiodontosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BiodontosDatabase::class.java,
                    "biodontos_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
