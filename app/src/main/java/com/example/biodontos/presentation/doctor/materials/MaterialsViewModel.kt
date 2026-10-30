package com.example.biodontos.presentation.doctor.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.usecase.materials.GetMaterialsUseCase
import com.example.biodontos.domain.usecase.materials.SaveMaterialUseCase
import com.example.biodontos.domain.usecase.materials.SetMaterialActiveUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MaterialsViewModel(
    private val getMaterialsUseCase: GetMaterialsUseCase,
    private val saveMaterialUseCase: SaveMaterialUseCase,
    private val setMaterialActiveUseCase: SetMaterialActiveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialsUiState(isLoading = true))
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MaterialsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadMaterials()
    }

    fun onEvent(event: MaterialsUiEvent) {
        when (event) {
            is MaterialsUiEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is MaterialsUiEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            is MaterialsUiEvent.Refresh -> {
                loadMaterials()
            }
            is MaterialsUiEvent.OpenCreateForm -> {
                _uiState.update {
                    it.copy(
                        isFormOpen = true,
                        formState = MaterialFormState()
                    )
                }
            }
            is MaterialsUiEvent.OpenEditForm -> {
                val mat = event.material
                _uiState.update {
                    it.copy(
                        isFormOpen = true,
                        formState = MaterialFormState(
                            id = mat.rawId,
                            name = mat.name,
                            invimaRegistration = mat.invimaRegistration,
                            batchLotNumber = mat.batchLotNumber,
                            manufacturingDate = mat.manufacturingDate,
                            expirationDate = mat.expirationDate,
                            isActive = mat.isActive
                        )
                    )
                }
            }
            is MaterialsUiEvent.DismissForm -> {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        formState = MaterialFormState()
                    )
                }
            }
            is MaterialsUiEvent.FormNameChanged -> {
                _uiState.update { state ->
                    val clean = event.name
                    state.copy(
                        formState = state.formState.copy(
                            name = clean,
                            nameError = if (clean.isBlank()) "El nombre comercial es obligatorio" else null
                        )
                    )
                }
            }
            is MaterialsUiEvent.FormInvimaChanged -> {
                _uiState.update { state ->
                    val clean = event.invima
                    state.copy(
                        formState = state.formState.copy(
                            invimaRegistration = clean,
                            invimaError = if (clean.isBlank()) "El registro INVIMA es obligatorio" else null
                        )
                    )
                }
            }
            is MaterialsUiEvent.FormLotChanged -> {
                _uiState.update { state ->
                    val clean = event.lot
                    state.copy(
                        formState = state.formState.copy(
                            batchLotNumber = clean,
                            lotError = if (clean.isBlank()) "El número de lote es obligatorio" else null
                        )
                    )
                }
            }
            is MaterialsUiEvent.FormManufacturingDateChanged -> {
                _uiState.update { state ->
                    val dateError = validateDates(event.date, state.formState.expirationDate)
                    state.copy(
                        formState = state.formState.copy(
                            manufacturingDate = event.date,
                            dateError = dateError
                        )
                    )
                }
            }
            is MaterialsUiEvent.FormExpirationDateChanged -> {
                _uiState.update { state ->
                    val dateError = validateDates(state.formState.manufacturingDate, event.date)
                    state.copy(
                        formState = state.formState.copy(
                            expirationDate = event.date,
                            dateError = dateError
                        )
                    )
                }
            }
            is MaterialsUiEvent.SaveMaterial -> {
                saveCurrentMaterial()
            }
            is MaterialsUiEvent.RequestDeactivate -> {
                _uiState.update { it.copy(materialToDeactivate = event.material) }
            }
            is MaterialsUiEvent.DismissDeactivateDialog -> {
                _uiState.update { it.copy(materialToDeactivate = null) }
            }
            is MaterialsUiEvent.ConfirmDeactivate -> {
                confirmDeactivateMaterial()
            }
            is MaterialsUiEvent.ReactivateMaterial -> {
                reactivateMaterial(event.material)
            }
        }
    }

    fun loadMaterials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getMaterialsUseCase(includeInactive = true).fold(
                ifLeft = { err ->
                    val errorMsg = when (err) {
                        is com.example.biodontos.domain.model.MaterialError -> err.userMessage
                        is com.example.biodontos.domain.model.DomainError.NetworkOrPersistenceFailure -> err.reason
                        else -> "No se pudo cargar el catálogo de materiales."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                },
                ifRight = { list ->
                    _uiState.update {
                        it.copy(
                            materials = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            )
        }
    }

    private fun saveCurrentMaterial() {
        val form = _uiState.value.formState
        val nameErr = if (form.name.isBlank()) "El nombre comercial es obligatorio" else null
        val invimaErr = if (form.invimaRegistration.isBlank()) "El registro INVIMA es obligatorio" else null
        val lotErr = if (form.batchLotNumber.isBlank()) "El número de lote es obligatorio" else null
        val dateErr = validateDates(form.manufacturingDate, form.expirationDate)

        if (nameErr != null || invimaErr != null || lotErr != null || dateErr != null) {
            _uiState.update {
                it.copy(
                    formState = it.formState.copy(
                        nameError = nameErr,
                        invimaError = invimaErr,
                        lotError = lotErr,
                        dateError = dateErr
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(formState = it.formState.copy(isSaving = true)) }

            val material = ClinicalMaterial(
                id = form.id?.let { MaterialId(it) } ?: MaterialId.generate(),
                name = form.name.trim(),
                invimaRegistration = form.invimaRegistration.trim(),
                batchLotNumber = form.batchLotNumber.trim(),
                manufacturingDate = form.manufacturingDate,
                expirationDate = form.expirationDate,
                isActive = form.isActive
            )

            saveMaterialUseCase(material).fold(
                ifLeft = { err ->
                    _uiState.update { it.copy(formState = it.formState.copy(isSaving = false)) }
                    _effects.send(MaterialsUiEffect.ShowError(err.userMessage))
                },
                ifRight = {
                    _uiState.update {
                        it.copy(
                            isFormOpen = false,
                            formState = MaterialFormState()
                        )
                    }
                    val msg = if (form.isEditing) {
                        "Material clínico actualizado exitosamente."
                    } else {
                        "Material clínico registrado exitosamente."
                    }
                    _effects.send(MaterialsUiEffect.ShowToast(msg))
                    loadMaterials()
                }
            )
        }
    }

    private fun confirmDeactivateMaterial() {
        val target = _uiState.value.materialToDeactivate ?: return
        viewModelScope.launch {
            setMaterialActiveUseCase(target.id, isActive = false).fold(
                ifLeft = { err ->
                    _uiState.update { it.copy(materialToDeactivate = null) }
                    _effects.send(MaterialsUiEffect.ShowError(err.userMessage))
                },
                ifRight = {
                    _uiState.update { it.copy(materialToDeactivate = null) }
                    _effects.send(
                        MaterialsUiEffect.ShowToast("Material '${target.name}' desactivado correctamente.")
                    )
                    loadMaterials()
                }
            )
        }
    }

    private fun reactivateMaterial(material: ClinicalMaterial) {
        viewModelScope.launch {
            setMaterialActiveUseCase(material.id, isActive = true).fold(
                ifLeft = { err ->
                    _effects.send(MaterialsUiEffect.ShowError(err.userMessage))
                },
                ifRight = {
                    _effects.send(
                        MaterialsUiEffect.ShowToast("Material '${material.name}' reactivado exitosamente.")
                    )
                    loadMaterials()
                }
            )
        }
    }

    private fun validateDates(mfg: LocalDate, exp: LocalDate): String? {
        val today = LocalDate.now()
        if (mfg.isAfter(today)) {
            return "La fecha de elaboración no puede ser una fecha futura."
        }
        if (!exp.isAfter(mfg)) {
            return "La fecha de vencimiento debe ser posterior a la fecha de elaboración."
        }
        return null
    }
}
