package com.example.biodontos.core.di

import com.example.biodontos.data.repository.AuthRepositoryImpl
import com.example.biodontos.data.repository.OdontogramRepositoryImpl
import com.example.biodontos.data.repository.PatientRepositoryImpl
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.OdontogramRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.auth.*
import com.example.biodontos.domain.usecase.odontogram.GetOdontogramUseCase
import com.example.biodontos.domain.usecase.odontogram.UpdateToothUseCase
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase

interface AppContainer {
    val authRepository: AuthRepository
    val patientRepository: PatientRepository
    val odontogramRepository: OdontogramRepository
    
    val loginUseCase: LoginUseCase
    val registerPatientAuthUseCase: RegisterPatientAuthUseCase
    val registerDoctorUseCase: RegisterDoctorUseCase
    val resetPasswordUseCase: ResetPasswordUseCase
    val getCurrentSessionUseCase: GetCurrentSessionUseCase
    val logoutUseCase: LogoutUseCase
    
    val getPatientsUseCase: GetPatientsUseCase
    val savePatientUseCase: SavePatientUseCase
    val getOdontogramUseCase: GetOdontogramUseCase
    val updateToothUseCase: UpdateToothUseCase
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    override val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl()
    }

    override val odontogramRepository: OdontogramRepository by lazy {
        OdontogramRepositoryImpl()
    }

    override val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    override val registerPatientAuthUseCase: RegisterPatientAuthUseCase by lazy {
        RegisterPatientAuthUseCase(authRepository)
    }

    override val registerDoctorUseCase: RegisterDoctorUseCase by lazy {
        RegisterDoctorUseCase(authRepository)
    }

    override val resetPasswordUseCase: ResetPasswordUseCase by lazy {
        ResetPasswordUseCase(authRepository)
    }

    override val getCurrentSessionUseCase: GetCurrentSessionUseCase by lazy {
        GetCurrentSessionUseCase(authRepository)
    }

    override val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
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
