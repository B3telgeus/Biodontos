package com.example.biodontos.presentation.odontogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp

class OdontogramViewModelFactory(private val patientId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
        return OdontogramViewModel(
            patientId = patientId,
            getOdontogramUseCase = application.container.getOdontogramUseCase,
            updateToothUseCase = application.container.updateToothUseCase
        ) as T
    }
}
