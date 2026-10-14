package com.example.biodontos.presentation.odontogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.usecase.odontogram.GetOdontogramUseCase
import com.example.biodontos.domain.usecase.odontogram.UpdateToothUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OdontogramViewModel(
    private val patientId: String,
    private val getOdontogramUseCase: GetOdontogramUseCase,
    private val updateToothUseCase: UpdateToothUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OdontogramState())
    val state: StateFlow<OdontogramState> = _state.asStateFlow()

    init {
        loadOdontogram()
    }

    fun loadOdontogram() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getOdontogramUseCase(patientId)
                .onSuccess { teethList ->
                    val teethMap = teethList.associateBy { it.number }
                    _state.update { it.copy(teeth = teethMap, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun setDentitionType(dentitionType: com.example.biodontos.domain.model.DentitionType) {
        _state.update { it.copy(dentitionType = dentitionType) }
    }

    fun onSurfaceClick(toothNumber: Int, surface: ToothSurface) {
        _state.update { 
            it.copy(
                selectedTooth = toothNumber,
                selectedSurface = surface,
                showSelectionDialog = true,
                comment = ""
            )
        }
    }

    fun onDismissDialog() {
        _state.update { it.copy(showSelectionDialog = false) }
    }

    fun onCommentChange(comment: String) {
        _state.update { it.copy(comment = comment) }
    }

    fun updateTooth(condition: ToothCondition) {
        val toothNumber = _state.value.selectedTooth ?: return
        val surface = _state.value.selectedSurface ?: return
        val comment = _state.value.comment

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            updateToothUseCase(patientId, toothNumber, surface, condition, comment)
                .onSuccess {
                    _state.update { it.copy(isSaving = false, showSelectionDialog = false) }
                    loadOdontogram() // Refresh
                }
                .onFailure { error ->
                    _state.update { it.copy(isSaving = false, error = "Error al guardar: ${error.message}") }
                }
        }
    }
}
