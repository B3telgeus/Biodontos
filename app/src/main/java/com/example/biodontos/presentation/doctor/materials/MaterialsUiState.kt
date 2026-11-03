package com.example.biodontos.presentation.doctor.materials

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.ClinicalMaterial
import java.time.LocalDate

enum class MaterialTabFilter(val label: String) {
    ACTIVE("Activos"),
    INACTIVE("Inactivos")
}

@Immutable
data class MaterialFormState(
    val id: String? = null,
    val name: String = "",
    val invimaRegistration: String = "",
    val batchLotNumber: String = "",
    val manufacturingDate: LocalDate = LocalDate.now(),
    val expirationDate: LocalDate = LocalDate.now().plusYears(1),
    val isActive: Boolean = true,
    val nameError: String? = null,
    val invimaError: String? = null,
    val lotError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false
) {
    val isEditing: Boolean get() = id != null
    val isValid: Boolean
        get() = name.isNotBlank() &&
            invimaRegistration.isNotBlank() &&
            batchLotNumber.isNotBlank() &&
            expirationDate.isAfter(manufacturingDate) &&
            !manufacturingDate.isAfter(LocalDate.now())
}

@Immutable
data class MaterialsUiState(
    val materials: List<ClinicalMaterial> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedTab: MaterialTabFilter = MaterialTabFilter.ACTIVE,
    val isFormOpen: Boolean = false,
    val formState: MaterialFormState = MaterialFormState(),
    val materialToDeactivate: ClinicalMaterial? = null
) {
    val filteredMaterials: List<ClinicalMaterial>
        get() {
            val tabFiltered = when (selectedTab) {
                MaterialTabFilter.ACTIVE -> materials.filter { it.isActive }
                MaterialTabFilter.INACTIVE -> materials.filter { !it.isActive }
            }
            if (searchQuery.isBlank()) return tabFiltered
            val query = searchQuery.trim().lowercase()
            return tabFiltered.filter { mat ->
                mat.name.lowercase().contains(query) ||
                    mat.invimaRegistration.lowercase().contains(query) ||
                    mat.batchLotNumber.lowercase().contains(query)
            }
        }

    val activeCount: Int get() = materials.count { it.isActive }
    val inactiveCount: Int get() = materials.count { !it.isActive }
    val expiredCount: Int get() = materials.count { it.isActive && it.isExpired() }
    val nearExpirationCount: Int get() = materials.count { it.isActive && it.isNearExpiration() }
}

sealed interface MaterialsUiEvent {
    data class SearchQueryChanged(val query: String) : MaterialsUiEvent
    data class TabSelected(val tab: MaterialTabFilter) : MaterialsUiEvent
    data object Refresh : MaterialsUiEvent

    // Form Events
    data object OpenCreateForm : MaterialsUiEvent
    data class OpenEditForm(val material: ClinicalMaterial) : MaterialsUiEvent
    data object DismissForm : MaterialsUiEvent
    data class FormNameChanged(val name: String) : MaterialsUiEvent
    data class FormInvimaChanged(val invima: String) : MaterialsUiEvent
    data class FormLotChanged(val lot: String) : MaterialsUiEvent
    data class FormManufacturingDateChanged(val date: LocalDate) : MaterialsUiEvent
    data class FormExpirationDateChanged(val date: LocalDate) : MaterialsUiEvent
    data object SaveMaterial : MaterialsUiEvent

    // Deactivation / Reactivation Events
    data class RequestDeactivate(val material: ClinicalMaterial) : MaterialsUiEvent
    data object DismissDeactivateDialog : MaterialsUiEvent
    data object ConfirmDeactivate : MaterialsUiEvent
    data class ReactivateMaterial(val material: ClinicalMaterial) : MaterialsUiEvent
}

sealed interface MaterialsUiEffect {
    data class ShowToast(val message: String) : MaterialsUiEffect
    data class ShowError(val message: String) : MaterialsUiEffect
}
