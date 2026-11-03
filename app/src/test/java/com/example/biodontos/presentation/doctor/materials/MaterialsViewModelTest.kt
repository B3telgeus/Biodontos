package com.example.biodontos.presentation.doctor.materials

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.usecase.materials.GetMaterialsUseCase
import com.example.biodontos.domain.usecase.materials.SaveMaterialUseCase
import com.example.biodontos.domain.usecase.materials.SetMaterialActiveUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMaterialsUseCase: GetMaterialsUseCase = mockk()
    private val saveMaterialUseCase: SaveMaterialUseCase = mockk()
    private val setMaterialActiveUseCase: SetMaterialActiveUseCase = mockk()

    private val today = LocalDate.now()

    // 1. Activo, vigente (> 30 días para vencer)
    private val materialValid = ClinicalMaterial(
        id = MaterialId("mat-valid"),
        name = "Resina Filtek Z350 XT",
        invimaRegistration = "INVIMA 2020DM-0012345",
        batchLotNumber = "LOT-2024-X89",
        manufacturingDate = today.minusMonths(6),
        expirationDate = today.plusMonths(12),
        isActive = true
    )

    // 2. Activo, próximo a vencer (<= 30 días)
    private val materialNearExp = ClinicalMaterial(
        id = MaterialId("mat-near-exp"),
        name = "Ácido Grabador Scotchbond",
        invimaRegistration = "INVIMA 2018DM-0009876",
        batchLotNumber = "LOT-2023-A12",
        manufacturingDate = today.minusMonths(11),
        expirationDate = today.plusDays(15),
        isActive = true
    )

    // 3. Activo, vencido
    private val materialExpired = ClinicalMaterial(
        id = MaterialId("mat-expired"),
        name = "Adhesivo Dental Single Bond",
        invimaRegistration = "INVIMA 2017DM-0005432",
        batchLotNumber = "LOT-2022-V99",
        manufacturingDate = today.minusYears(2),
        expirationDate = today.minusDays(5),
        isActive = true
    )

    // 4. Inactivo (desactivado lógicamente)
    private val materialInactive = ClinicalMaterial(
        id = MaterialId("mat-inactive"),
        name = "Cemento Ionómero de Vidrio",
        invimaRegistration = "INVIMA 2019DM-0003322",
        batchLotNumber = "LOT-2023-C44",
        manufacturingDate = today.minusMonths(8),
        expirationDate = today.plusMonths(6),
        isActive = false
    )

    private val sampleCatalog = listOf(materialValid, materialNearExp, materialExpired, materialInactive)

    @Before
    fun setUp() {
        coEvery { getMaterialsUseCase(includeInactive = true) } returns sampleCatalog.right()
    }

    @Test
    fun `initialization loads materials and computes summary metrics accurately`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(4, state.materials.size)
        assertEquals(3, state.activeCount)
        assertEquals(1, state.inactiveCount)
        assertEquals(1, state.nearExpirationCount)
        assertEquals(1, state.expiredCount)
        assertEquals(MaterialTabFilter.ACTIVE, state.selectedTab)
        assertEquals(3, state.filteredMaterials.size)
    }

    @Test
    fun `initialization failure updates state with error message`() = runTest {
        coEvery { getMaterialsUseCase(includeInactive = true) } returns
            DomainError.NetworkOrPersistenceFailure("Fallo de conexión a la base de datos").left()

        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fallo de conexión a la base de datos", state.errorMessage)
        assertTrue(state.materials.isEmpty())
        assertEquals(0, state.activeCount)
    }

    @Test
    fun `search query filters materials case-insensitively by name, invima or lot`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Búsqueda por nombre comercial
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("resina"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Resina Filtek Z350 XT", viewModel.uiState.value.filteredMaterials.first().name)

        // Búsqueda por INVIMA
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("2018DM"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Ácido Grabador Scotchbond", viewModel.uiState.value.filteredMaterials.first().name)

        // Búsqueda por número de lote
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("v99"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Adhesivo Dental Single Bond", viewModel.uiState.value.filteredMaterials.first().name)

        // Búsqueda sin coincidencias
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Inexistente"))
        assertTrue(viewModel.uiState.value.filteredMaterials.isEmpty())

        // Limpiar búsqueda
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged(""))
        assertEquals(3, viewModel.uiState.value.filteredMaterials.size)
    }

    @Test
    fun `tab filter switches between active and inactive materials correctly`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Pestaña Inactivos
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.INACTIVE))
        assertEquals(MaterialTabFilter.INACTIVE, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Cemento Ionómero de Vidrio", viewModel.uiState.value.filteredMaterials.first().name)
        assertFalse(viewModel.uiState.value.filteredMaterials.first().isActive)

        // Regresar a pestaña Activos
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.ACTIVE))
        assertEquals(MaterialTabFilter.ACTIVE, viewModel.uiState.value.selectedTab)
        assertEquals(3, viewModel.uiState.value.filteredMaterials.size)
        assertTrue(viewModel.uiState.value.filteredMaterials.all { it.isActive })
    }

    @Test
    fun `search query and tab filter combine predictably`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Pestaña Inactivos + Búsqueda "Cemento"
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.INACTIVE))
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Cemento"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)

        // Pestaña Activos + Búsqueda "Cemento" (no hay cementos activos)
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.ACTIVE))
        assertTrue(viewModel.uiState.value.filteredMaterials.isEmpty())
    }

    @Test
    fun `refresh event triggers catalog reload`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.Refresh)

        coVerify(atLeast = 2) { getMaterialsUseCase(includeInactive = true) }
    }

    @Test
    fun `open create and edit form initializes formState appropriately`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Abrir formulario de nuevo material
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)
        assertTrue(viewModel.uiState.value.isFormOpen)
        assertFalse(viewModel.uiState.value.formState.isEditing)
        assertNull(viewModel.uiState.value.formState.id)
        assertEquals("", viewModel.uiState.value.formState.name)

        // Cerrar formulario
        viewModel.onEvent(MaterialsUiEvent.DismissForm)
        assertFalse(viewModel.uiState.value.isFormOpen)

        // Abrir formulario de edición de material existente
        viewModel.onEvent(MaterialsUiEvent.OpenEditForm(materialValid))
        assertTrue(viewModel.uiState.value.isFormOpen)
        assertTrue(viewModel.uiState.value.formState.isEditing)
        assertEquals(materialValid.rawId, viewModel.uiState.value.formState.id)
        assertEquals(materialValid.name, viewModel.uiState.value.formState.name)
        assertEquals(materialValid.invimaRegistration, viewModel.uiState.value.formState.invimaRegistration)
        assertEquals(materialValid.batchLotNumber, viewModel.uiState.value.formState.batchLotNumber)
    }

    @Test
    fun `form field changes update state and validate dates and blanks in real-time`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)

        // Validación de campos en blanco
        viewModel.onEvent(MaterialsUiEvent.FormNameChanged(""))
        assertNotNull(viewModel.uiState.value.formState.nameError)

        viewModel.onEvent(MaterialsUiEvent.FormNameChanged("Resina A2"))
        assertNull(viewModel.uiState.value.formState.nameError)

        viewModel.onEvent(MaterialsUiEvent.FormInvimaChanged(""))
        assertNotNull(viewModel.uiState.value.formState.invimaError)

        viewModel.onEvent(MaterialsUiEvent.FormInvimaChanged("INVIMA 2024DM-001"))
        assertNull(viewModel.uiState.value.formState.invimaError)

        viewModel.onEvent(MaterialsUiEvent.FormLotChanged(""))
        assertNotNull(viewModel.uiState.value.formState.lotError)

        viewModel.onEvent(MaterialsUiEvent.FormLotChanged("LOT-100"))
        assertNull(viewModel.uiState.value.formState.lotError)

        // Fecha de elaboración en el futuro debe generar error
        val futureDate = today.plusDays(5)
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(futureDate))
        assertNotNull(viewModel.uiState.value.formState.dateError)
        assertTrue(viewModel.uiState.value.formState.dateError?.contains("futura") == true)

        // Fecha de elaboración válida pero vencimiento anterior o igual a elaboración
        val validMfg = today.minusMonths(2)
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(validMfg))
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(validMfg.minusDays(1)))
        assertNotNull(viewModel.uiState.value.formState.dateError)
        assertTrue(viewModel.uiState.value.formState.dateError?.contains("posterior") == true)

        // Fechas correctas
        val validExp = today.plusMonths(12)
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(validExp))
        assertNull(viewModel.uiState.value.formState.dateError)
        assertTrue(viewModel.uiState.value.formState.isValid)
    }

    @Test
    fun `save material aborts without calling usecase if fields are invalid`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)

        // Intentar guardar formulario con campos en blanco
        viewModel.onEvent(MaterialsUiEvent.SaveMaterial)

        coVerify(exactly = 0) { saveMaterialUseCase(any()) }
        assertNotNull(viewModel.uiState.value.formState.nameError)
        assertNotNull(viewModel.uiState.value.formState.invimaError)
        assertNotNull(viewModel.uiState.value.formState.lotError)
    }

    @Test
    fun `save material success saves entity, closes form and emits toast effect`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)
        viewModel.onEvent(MaterialsUiEvent.FormNameChanged("Sellante Clinpro"))
        viewModel.onEvent(MaterialsUiEvent.FormInvimaChanged("INVIMA 2021DM-00999"))
        viewModel.onEvent(MaterialsUiEvent.FormLotChanged("LOT-2024-S01"))
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(today.minusMonths(1)))
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(today.plusYears(1)))

        val saved = ClinicalMaterial(
            name = "Sellante Clinpro",
            invimaRegistration = "INVIMA 2021DM-00999",
            batchLotNumber = "LOT-2024-S01",
            manufacturingDate = today.minusMonths(1),
            expirationDate = today.plusYears(1)
        )
        coEvery { saveMaterialUseCase(any()) } returns saved.right()

        viewModel.effects.test {
            viewModel.onEvent(MaterialsUiEvent.SaveMaterial)

            val effect = awaitItem()
            assertTrue(effect is MaterialsUiEffect.ShowToast)
            assertTrue((effect as MaterialsUiEffect.ShowToast).message.contains("exitosamente"))
            assertFalse(viewModel.uiState.value.isFormOpen)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { saveMaterialUseCase(any()) }
    }

    @Test
    fun `save material failure keeps form open and emits error effect`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)
        viewModel.onEvent(MaterialsUiEvent.FormNameChanged("Resina Z350"))
        viewModel.onEvent(MaterialsUiEvent.FormInvimaChanged("INVIMA 2020DM-0012345"))
        viewModel.onEvent(MaterialsUiEvent.FormLotChanged("LOT-2024-X89"))
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(today.minusMonths(2)))
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(today.plusMonths(10)))

        coEvery { saveMaterialUseCase(any()) } returns MaterialError.DuplicateMaterial.left()

        viewModel.effects.test {
            viewModel.onEvent(MaterialsUiEvent.SaveMaterial)

            val effect = awaitItem()
            assertTrue(effect is MaterialsUiEffect.ShowError)
            assertEquals(MaterialError.DuplicateMaterial.userMessage, (effect as MaterialsUiEffect.ShowError).message)
            assertTrue(viewModel.uiState.value.isFormOpen)
            assertFalse(viewModel.uiState.value.formState.isSaving)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deactivation flow with two-step dialog deactivates material and notifies user`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Paso 1: Solicitar desactivación
        viewModel.onEvent(MaterialsUiEvent.RequestDeactivate(materialValid))
        assertEquals(materialValid, viewModel.uiState.value.materialToDeactivate)

        // Cancelar diálogo
        viewModel.onEvent(MaterialsUiEvent.DismissDeactivateDialog)
        assertNull(viewModel.uiState.value.materialToDeactivate)

        // Solicitar de nuevo y confirmar
        viewModel.onEvent(MaterialsUiEvent.RequestDeactivate(materialValid))
        coEvery { setMaterialActiveUseCase(materialValid.id, isActive = false) } returns Unit.right()

        viewModel.effects.test {
            viewModel.onEvent(MaterialsUiEvent.ConfirmDeactivate)

            val effect = awaitItem()
            assertTrue(effect is MaterialsUiEffect.ShowToast)
            assertTrue((effect as MaterialsUiEffect.ShowToast).message.contains("desactivado"))
            assertNull(viewModel.uiState.value.materialToDeactivate)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setMaterialActiveUseCase(materialValid.id, isActive = false) }
    }

    @Test
    fun `reactivate material calls usecase with true and notifies user`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        coEvery { setMaterialActiveUseCase(materialInactive.id, isActive = true) } returns Unit.right()

        viewModel.effects.test {
            viewModel.onEvent(MaterialsUiEvent.ReactivateMaterial(materialInactive))

            val effect = awaitItem()
            assertTrue(effect is MaterialsUiEffect.ShowToast)
            assertTrue((effect as MaterialsUiEffect.ShowToast).message.contains("reactivado"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setMaterialActiveUseCase(materialInactive.id, isActive = true) }
    }

    // =========================================================================
    // ADVERSARIAL CHALLENGE SUITE (CHALLENGER M3)
    // =========================================================================

    @Test
    fun `search filter boundary - mixed case and whitespace-padded queries trim and match accurately`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Mixed case and padded: "   fIlTeK   "
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("   fIlTeK   "))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Resina Filtek Z350 XT", viewModel.uiState.value.filteredMaterials.first().name)

        // Mixed case INVIMA with tabs and newlines: "\t 2020dm-0012345 \n"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("\t 2020dm-0012345 \n"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Resina Filtek Z350 XT", viewModel.uiState.value.filteredMaterials.first().name)

        // Mixed case lot number: "  x89  "
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("  x89  "))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Resina Filtek Z350 XT", viewModel.uiState.value.filteredMaterials.first().name)

        // Pure whitespace query returns all active materials
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("     "))
        assertEquals(3, viewModel.uiState.value.filteredMaterials.size)
    }

    @Test
    fun `search filter boundary - partial matches across name, invima, and lot number simultaneously`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Partial match on lot: "2024" appears in "LOT-2024-X89"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("2024"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Resina Filtek Z350 XT", viewModel.uiState.value.filteredMaterials.first().name)

        // Partial match on name: "Scotch" appears in "Ácido Grabador Scotchbond"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Scotch"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Ácido Grabador Scotchbond", viewModel.uiState.value.filteredMaterials.first().name)

        // Partial match on INVIMA: "0005432" appears in "INVIMA 2017DM-0005432"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("0005432"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Adhesivo Dental Single Bond", viewModel.uiState.value.filteredMaterials.first().name)

        // Common substring across multiple active items: "201" appears in 2018DM and 2017DM
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("201"))
        assertEquals(2, viewModel.uiState.value.filteredMaterials.size)
    }

    @Test
    fun `search filter boundary - accented characters exact matching vs unaccented limitation`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Exact accent uppercase: "Ácido"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Ácido"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Ácido Grabador Scotchbond", viewModel.uiState.value.filteredMaterials.first().name)

        // Exact accent lowercase: "ácido"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("ácido"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Ácido Grabador Scotchbond", viewModel.uiState.value.filteredMaterials.first().name)

        // Unaccented "acido" vs "Ácido Grabador":
        // Documents that standard String.contains() without diacritic normalization does not match
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("acido"))
        val unaccentedMatchCount = viewModel.uiState.value.filteredMaterials.size
        assertEquals(0, unaccentedMatchCount)
    }

    @Test
    fun `tab filter inactive and search combinations filter strictly within active tab scope`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Switch to INACTIVE tab
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.INACTIVE))
        assertEquals(MaterialTabFilter.INACTIVE, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Cemento Ionómero de Vidrio", viewModel.uiState.value.filteredMaterials.first().name)

        // Search for inactive item substring "Vidrio"
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Vidrio"))
        assertEquals(1, viewModel.uiState.value.filteredMaterials.size)
        assertEquals("Cemento Ionómero de Vidrio", viewModel.uiState.value.filteredMaterials.first().name)

        // Search for active item "Filtek" while on INACTIVE tab: must return 0
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Filtek"))
        assertTrue(viewModel.uiState.value.filteredMaterials.isEmpty())

        // Search for inactive item "Vidrio" while on ACTIVE tab: must return 0
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.ACTIVE))
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("Vidrio"))
        assertTrue(viewModel.uiState.value.filteredMaterials.isEmpty())
    }

    @Test
    fun `kpi counts remain invariant under search filtering and tab switching`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Initial metrics
        val initialActive = viewModel.uiState.value.activeCount
        val initialInactive = viewModel.uiState.value.inactiveCount
        val initialNearExp = viewModel.uiState.value.nearExpirationCount
        val initialExpired = viewModel.uiState.value.expiredCount

        assertEquals(3, initialActive)
        assertEquals(1, initialInactive)
        assertEquals(1, initialNearExp)
        assertEquals(1, initialExpired)

        // Apply restrictive search with 0 matches
        viewModel.onEvent(MaterialsUiEvent.SearchQueryChanged("NoMatchQueryXYZ"))
        assertTrue(viewModel.uiState.value.filteredMaterials.isEmpty())

        // KPI metrics must NOT be altered by search filter
        assertEquals(initialActive, viewModel.uiState.value.activeCount)
        assertEquals(initialInactive, viewModel.uiState.value.inactiveCount)
        assertEquals(initialNearExp, viewModel.uiState.value.nearExpirationCount)
        assertEquals(initialExpired, viewModel.uiState.value.expiredCount)

        // Switch tab to INACTIVE
        viewModel.onEvent(MaterialsUiEvent.TabSelected(MaterialTabFilter.INACTIVE))
        assertEquals(initialActive, viewModel.uiState.value.activeCount)
        assertEquals(initialInactive, viewModel.uiState.value.inactiveCount)
    }

    @Test
    fun `two-step dialog flow - confirming deactivation with no material selected is a safe no-op`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        assertNull(viewModel.uiState.value.materialToDeactivate)

        // Invoke ConfirmDeactivate without prior RequestDeactivate
        viewModel.onEvent(MaterialsUiEvent.ConfirmDeactivate)

        coVerify(exactly = 0) { setMaterialActiveUseCase(any<MaterialId>(), any<Boolean>()) }
        assertNull(viewModel.uiState.value.materialToDeactivate)
    }

    @Test
    fun `date validation in form state rejects future manufacturing dates`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)

        // Future manufacturing date (tomorrow)
        val futureDate = today.plusDays(1)
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(futureDate))

        val state = viewModel.uiState.value.formState
        assertNotNull(state.dateError)
        assertTrue(state.dateError?.contains("futura") == true)
        assertFalse(state.isValid)

        // Populate required fields and attempt to save: must be blocked
        viewModel.onEvent(MaterialsUiEvent.FormNameChanged("Ionómero R"))
        viewModel.onEvent(MaterialsUiEvent.FormInvimaChanged("INVIMA 2024DM-001"))
        viewModel.onEvent(MaterialsUiEvent.FormLotChanged("LOT-99"))

        viewModel.onEvent(MaterialsUiEvent.SaveMaterial)
        coVerify(exactly = 0) { saveMaterialUseCase(any()) }
    }

    @Test
    fun `date validation in form state rejects expiration date equal to or before manufacturing date`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)
        viewModel.onEvent(MaterialsUiEvent.OpenCreateForm)

        val mfgDate = today.minusMonths(2)
        viewModel.onEvent(MaterialsUiEvent.FormManufacturingDateChanged(mfgDate))

        // Expiration equal to manufacturing date
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(mfgDate))
        assertNotNull(viewModel.uiState.value.formState.dateError)
        assertTrue(viewModel.uiState.value.formState.dateError?.contains("posterior") == true)
        assertFalse(viewModel.uiState.value.formState.isValid)

        viewModel.onEvent(MaterialsUiEvent.SaveMaterial)
        coVerify(exactly = 0) { saveMaterialUseCase(any()) }

        // Expiration before manufacturing date
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(mfgDate.minusDays(1)))
        assertNotNull(viewModel.uiState.value.formState.dateError)
        assertTrue(viewModel.uiState.value.formState.dateError?.contains("posterior") == true)
        assertFalse(viewModel.uiState.value.formState.isValid)

        viewModel.onEvent(MaterialsUiEvent.SaveMaterial)
        coVerify(exactly = 0) { saveMaterialUseCase(any()) }

        // Expiration after manufacturing date: error clears
        viewModel.onEvent(MaterialsUiEvent.FormExpirationDateChanged(mfgDate.plusDays(1)))
        assertNull(viewModel.uiState.value.formState.dateError)
    }

    @Test
    fun `editing existing material retains rawId and emits updated toast upon successful save`() = runTest {
        val viewModel = MaterialsViewModel(getMaterialsUseCase, saveMaterialUseCase, setMaterialActiveUseCase)

        // Open edit form with materialValid
        viewModel.onEvent(MaterialsUiEvent.OpenEditForm(materialValid))
        val formState = viewModel.uiState.value.formState
        assertTrue(formState.isEditing)
        assertEquals(materialValid.rawId, formState.id)
        assertEquals(materialValid.name, formState.name)

        // Modify lot
        viewModel.onEvent(MaterialsUiEvent.FormLotChanged("LOT-2024-MODIFIED"))

        val updated = materialValid.copy(batchLotNumber = "LOT-2024-MODIFIED")
        coEvery { saveMaterialUseCase(any()) } returns updated.right()

        viewModel.effects.test {
            viewModel.onEvent(MaterialsUiEvent.SaveMaterial)

            val effect = awaitItem()
            assertTrue(effect is MaterialsUiEffect.ShowToast)
            assertTrue((effect as MaterialsUiEffect.ShowToast).message.contains("actualizado exitosamente"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            saveMaterialUseCase(match { it.id == materialValid.id && it.batchLotNumber == "LOT-2024-MODIFIED" })
        }
    }
}

