package com.example.biodontos.core.di

import com.example.biodontos.data.repository.PatientRepositoryImpl
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase

interface AppContainer {
    val patientRepository: PatientRepository
    val getPatientsUseCase: GetPatientsUseCase
    val savePatientUseCase: SavePatientUseCase
}

class DefaultAppContainer : AppContainer {
    override val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl()
    }

    override val getPatientsUseCase: GetPatientsUseCase by lazy {
        GetPatientsUseCase(patientRepository)
    }

    override val savePatientUseCase: SavePatientUseCase by lazy {
        SavePatientUseCase(patientRepository)
    }
}
