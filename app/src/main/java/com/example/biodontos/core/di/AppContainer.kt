package com.example.biodontos.core.di

import com.example.biodontos.data.repository.OdontogramRepositoryImpl
import com.example.biodontos.data.repository.PatientRepositoryImpl
import com.example.biodontos.domain.repository.OdontogramRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.odontogram.GetOdontogramUseCase
import com.example.biodontos.domain.usecase.odontogram.UpdateToothUseCase
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase

interface AppContainer {
    val patientRepository: PatientRepository
    val odontogramRepository: OdontogramRepository
    val getPatientsUseCase: GetPatientsUseCase
    val savePatientUseCase: SavePatientUseCase
    val getOdontogramUseCase: GetOdontogramUseCase
    val updateToothUseCase: UpdateToothUseCase
}

class DefaultAppContainer : AppContainer {
    override val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl()
    }

    override val odontogramRepository: OdontogramRepository by lazy {
        OdontogramRepositoryImpl()
    }

    override val getPatientsUseCase: GetPatientsUseCase by lazy {
        GetPatientsUseCase(patientRepository)
    }

    override val savePatientUseCase: SavePatientUseCase by lazy {
        SavePatientUseCase(patientRepository)
    }

    override val getOdontogramUseCase: GetOdontogramUseCase by lazy {
        GetOdontogramUseCase(odontogramRepository)
    }

    override val updateToothUseCase: UpdateToothUseCase by lazy {
        UpdateToothUseCase(odontogramRepository)
    }
}
