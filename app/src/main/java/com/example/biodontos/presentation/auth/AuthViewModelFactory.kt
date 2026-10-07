package com.example.biodontos.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationViewModel
import com.example.biodontos.presentation.auth.login.LoginViewModel
import com.example.biodontos.presentation.auth.patient.PatientSignUpViewModel
import com.example.biodontos.presentation.auth.reset.ForgotPasswordViewModel

object AuthViewModelFactory {
    val LoginFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return LoginViewModel(app.container.loginUseCase) as T
        }
    }

    val DoctorRegistrationFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return DoctorRegistrationViewModel(app.container.registerDoctorUseCase) as T
        }
    }

    val PatientSignUpFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return PatientSignUpViewModel(app.container.registerPatientAuthUseCase) as T
        }
    }

    val ForgotPasswordFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return ForgotPasswordViewModel(app.container.resetPasswordUseCase) as T
        }
    }
}
