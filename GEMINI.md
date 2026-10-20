# GUÍA DE REGLAS Y ESTÁNDARES DEL PROYECTO: BIODONTOS (KOTLIN / ANDROID / JETPACK COMPOSE)

Eres el **Asistente Experto en Desarrollo de Software** para la aplicación dental **Biodontos** en Kotlin (Android / Jetpack Compose).
Debes cumplir estrictamente con las siguientes **REGLAS de arquitectura, diseño, seguridad clínica y estilo** en cada solución, refactorización o archivo que generes.

---

## 1. ARQUITECTURA Y ESTILO DE CÓDIGO (CLEAN ARCHITECTURE & MVI/MVVM)

1. **Separación Estricta de Capas:**
   - **`data`**: Fuentes de datos, DAOs de Room, clientes de red (Ktor / Retrofit / Supabase), DTOs con anotaciones de serialización (`@SerialName`) y mappers hacia el dominio.
   - **`domain`**: Entidades y modelos de negocio puros (POKOs inmutables, sin anotaciones de serialización ni dependencias de Android/UI), UseCases con operador `invoke`, y contratos de repositorios (`interface`).
   - **`presentation`**: ViewModels, Composables, componentes gráficos (Canvas), estados inmutables (`UiState`) y eventos (`UiEvent`).
   - **`core`**: Extensiones, dispatchers, utilitarios de formateo clínico y clases base transversales.

2. **Flujo de Datos Unidireccional (UDF) y StateFlow:**
   - Los `ViewModel` deben exponer un único `StateFlow<UiState>` inmutable mediante `_uiState.asStateFlow()`.
   - Los eventos de UI únicos (navegación, alertas clínicas, Snackbars) deben enviarse a través de un canal o flujo desacoplado (ej. `Channel<UiEffect>(Channel.BUFFERED).receiveAsFlow()`).
   - En Compose, consumir los flujos usando `collectAsStateWithLifecycle()` para respetar el ciclo de vida de la pantalla.
   - Marcar los modelos de estado de Compose con `@Immutable` o `@Stable` para optimizar el árbol de recomposición.

3. **Inmutabilidad y Prevención de Errores en Kotlin:**
   - **Prohibido el uso de `!!` (doble bang):** Emplear safe calls (`?.`), elvis operator (`?:`), o funciones como `requireNotNull(value) { "Mensaje clínico explicativo" }`.
   - Utilizar Value Classes (`@JvmInline value class PatientId(val value: Long)`) para evitar el antipatrón de *Primitive Obsession* en identificadores clínicos (`PatientId`, `AppointmentId`, `ProcedureId`).
   - Parámetros nombrados obligatorios en constructores o funciones con más de 3 argumentos para evitar confusiones de datos anatómicos o métricas de sondaje.

---

## 2. MODELOS DE DATOS Y MAPEO DE API (OPEN DENTAL REST / FHIR)

1. **Convenciones de Casing y Mapeo:**
   - La API de Open Dental envía y recibe nombres en `PascalCase` y rutas de recursos en plural minúsculas (ej. `/patients`, `/allergies`, `/appointments`).
   - En Kotlin, los atributos de las `data class` DTO DEBEN escribirse en `camelCase` estándar y mapearse explícitamente con `@SerialName("PascalCase")` de `kotlinx.serialization`.
   - Nunca mezcles anotaciones de serialización en las entidades del paquete `domain.model`. Implementa funciones de extensión mapper (`Dto.toDomain()` y `Domain.toDto()`).

2. **Endpoints Especializados (Non-Standard CRUD):**
   - En citas (`Appointments`), **NUNCA** uses un `PUT` genérico que sobrescriba el recurso completo. Utiliza exclusivamente los endpoints transaccionales específicos:
     - `/appointments/{id}/Confirm` para confirmar o cambiar el estado de confirmación.
     - `/appointments/{id}/Note` para anexar o actualizar notas clínicas.
     - `/appointments/{id}/Pinboard` para mover a tablero.
   - Maneja operaciones clínicas destructivas o críticas con transaccionalidad e idempotencia.

3. **Paginación y Fechas Clínicas:**
   - Toda consulta masiva debe recibir parámetros de paginación explícitos: `Limit` (con valor por defecto razonable) y `Offset`.
   - Las fechas de Open Dental vienen frecuentemente como cadenas `"yyyy-MM-dd HH:mm:ss"` o `"yyyy-MM-dd"`. En la capa de dominio DEBEN transformarse siempre a tipos seguros (`java.time.LocalDate`, `java.time.LocalDateTime` o `java.time.Instant`).

---

## 3. ODONTOGRAMA Y RENDERIZADO GRÁFICO (FDI & PERIODONTOMETRÍA)

1. **Nomenclatura FDI Internacional (ISO 3950):**
   - **Dentición Permanente:** Cuadrantes 1 al 4 (11 a 18, 21 a 28, 31 a 38, 41 a 48).
   - **Dentición Temporal / Decidua (Pediatría):** Cuadrantes 5 al 8 (51 a 55, 61 a 65, 71 a 75, 81 a 85).
   - El modelo de datos debe validar el rango de cada diente mediante validación defensiva en el `init`.

2. **Superficies Anatómicas Dentales:**
   - Manejar la interacción y renderizado por superficies individuales:
     - **Mesial (M)** y **Distal (D)**.
     - **Oclusal (O)** en piezas posteriores (premolares/molares) / **Incisal (I)** en piezas anteriores (incisivos/caninos).
     - **Vestibular / Bucal (V)** en posteriores o **Labial (L)** en anteriores.
     - **Palatino (P)** en maxilar superior / **Lingual (L)** en mandíbula inferior.
   - Estados clínicos normalizados: Sano, Caries activa, Obturación (Amalgama / Resina), Sellador, Corona / Prótesis fija, Endodoncia, Diente ausente / Exodoncia.

3. **Periodontometría de 6 Puntos (Sondaje Periodontal):**
   - Cada pieza dental evaluada debe soportar 6 sitios de medición estándar:
     - Vestibular: Mesiovestibular (MV), Vestibular medio (V), Distovestibular (DV).
     - Lingual/Palatino: Mesiolingual/palatino (ML/MP), Lingual/palatino medio (L/P), Distolingual/palatino (DL/DP).
   - Parámetros por sitio en milímetros (mm):
     - Profundidad de Sondaje (Pocket Depth - PD): Alerta clínica visual (rojo) si `PD >= 4mm`.
     - Recesión Gingival / Margen Gingival (GR/GM).
     - Pérdida de Inserción Clínica calculada: $\text{CAL} = \text{PD} + \text{GR}$.
     - Marcadores booleanos: Sangrado al Sondaje (BOP) y Supuración (SUP).
     - Movilidad dental (Grados 0, I, II, III) y Afectación de furca (Grados I a IV).

4. **Renderizado en Compose Canvas:**
   - Desacoplar completamente las métricas clínicas (mm, códigos FDI) de las coordenadas de dibujo. El renderizador debe operar en coordenadas relativas o normalizadas (0.0f a 1.0f) y proyectarse a los píxeles del `DrawScope`.
   - Detección de toques (*hit-testing*) basada en polígonos o regiones vectoriales matemáticas, no en componentes flotantes pesados para evitar lag con 32-52 piezas simultáneas.

---

## 4. CRITERIOS CLÍNICOS, UX Y SEGURIDAD SANITARIA (HIPAA / PRIVACIDAD)

1. **UX Clínica Sin Fricción:**
   - Evitar popups y modales innecesarios durante la inspección rutinaria para no interrumpir el flujo del odontólogo con guantes.
   - **Confirmación Obligatoria:** Exigir confirmación explícita (diálogos de 2 pasos con resumen de impacto) solo para acciones clínicas críticas: cambio de estado de cita a "Cancelada/No asistió", eliminación de registros, prescripción de medicamentos o registro de exodoncias.
   - Soporte para feedback háptico (`HapticFeedbackType.LongPress` / `TextHandleMove`) al seleccionar superficies dentales o marcar hallazgos.

2. **Privacidad y Protección de Datos de Salud (PHI):**
   - **Cero Datos de Pacientes en Logs:** Queda estrictamente prohibido registrar nombres, documentos de identidad, diagnósticos o números de teléfono en `Logcat`, Timber o Crashlytics. Usar solo IDs ofuscados o logs estructurales anónimos.
   - Almacenamiento local seguro: Room con cifrado si se guardan datos de pacientes en disco.
   - Cierre o bloqueo de sesión por inactividad configurable en entornos de consultorio compartido.

---

## 5. ESTÁNDARES DE TESTING

1. **Tests Unitarios Obligatorios:**
   - **Mappers:** Testear la conversión bidireccional entre DTOs y Modelos de Dominio cubriendo nulos y casos borde.
   - **ViewModels:** Usar `Turbine` y `StandardTestDispatcher` de `kotlinx-coroutines-test` para verificar estados `StateFlow` y eventos.
   - **Lógica Clínica:** Testear validaciones de rangos FDI, cálculo de CAL periodontal y transformaciones de coordenadas.

## 6. EDGE-TO-EDGE, SISTEMA DE INSETS Y PROTECCIÓN DE UI

1. **Obligatoriedad de Edge-to-Edge:**
   - La aplicación ejecuta `enableEdgeToEdge()` de forma obligatoria (Android 15+ / compileSdk 36). Ningún componente visual debe asumir márgenes fijos del sistema operativo.
   
2. **Prohibición en Scaffold.bottomBar sin Insets:**
   - Queda estrictamente PROHIBIDO renderizar cajas (`Box`), superficies (`Surface`) o filas (`Row`) propias directamente en el slot `Scaffold.bottomBar` sin aplicar `com.example.biodontos.core.designsystem.insets.BiodontosBottomActionBar` o `Modifier.bottomActionInsets()`. Los botones al pie ("Guardar y Continuar", "Confirmar Reserva", etc.) deben quedar siempre por encima de la barra de navegación del sistema y elevarse automáticamente con el teclado virtual (IME).

3. **Consumo de Insets en Shells y Scaffolds Anidados:**
   - Cuando una pantalla se renderice dentro de un contenedor shell (`DoctorPortalShell`, `PatientPortalShell`), el shell debe consumir los insets (`.padding(padding).consumeWindowInsets(padding)`).
   - Todo `Scaffold` anidado dentro de un shell DEBE declarar explícitamente `contentWindowInsets = WindowInsets(0, 0, 0, 0)` para prevenir paddings dobles indeseados.

4. **Formularios y Teclado Virtual (IME):**
   - En pantallas con formularios y campos de entrada de texto, siempre se debe encadenar `.consumeWindowInsets(padding).imePadding()` o utilizar `BiodontosFormColumn` para garantizar que el botón de confirmación o el último campo no queden tapados por el teclado en pantallas compactas o modales de diálogo.
   - En listas con FloatingActionButton (`ExtendedFloatingActionButton`), la lista (`LazyColumn`) debe definir `contentPadding` con al menos `bottom = 88.dp` para permitir que el último elemento sea accesible sin interferencia del botón flotante.

---

## FEW-SHOT EXAMPLES (EJEMPLOS DE REFERENCIA)

### EJEMPLO 1: Modelado DTO, Entidad de Dominio y API de Open Dental con Paginación

<!-- BAD EXAMPLE -->
❌ INCORRECTO:
```kotlin
// Malo: Expone detalles de API en la entidad, nombres incorrectos, ruta con mayúscula
data class Allergy(
    val PatNum: Long,
    val defDescription: String,
    val Reaction: String?
)

interface DentalApi {
    @POST("/Allergies")
    suspend fun saveAllergy(@Body allergy: Allergy): Allergy
}
```

<!-- GOOD EXAMPLE -->
✅ CORRECTO:
```kotlin
// data/remote/dto/AllergyDto.kt
package com.example.biodontos.data.remote.dto

import com.example.biodontos.domain.model.Allergy
import com.example.biodontos.domain.model.PatientId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllergyDto(
    @SerialName("AllergyNum")
    val allergyNum: Long? = null,
    @SerialName("PatNum")
    val patientNumber: Long,
    @SerialName("defDescription")
    val defDescription: String,
    @SerialName("Reaction")
    val reaction: String? = null,
    @SerialName("DateAdverseReaction")
    val dateAdverseReaction: String? = null
) {
    fun toDomain(): Allergy = Allergy(
        id = allergyNum,
        patientId = PatientId(patientNumber),
        description = defDescription,
        reaction = reaction,
        adverseReactionDate = dateAdverseReaction
    )
}

// data/remote/api/OpenDentalAllergyApi.kt
package com.example.biodontos.data.remote.api

import com.example.biodontos.data.remote.dto.AllergyDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OpenDentalAllergyApi {
    @GET("allergies")
    suspend fun getAllergies(
        @Query("PatNum") patientNumber: Long,
        @Query("Limit") limit: Int = 100,
        @Query("Offset") offset: Int = 0
    ): List<AllergyDto>

    @POST("allergies")
    suspend fun createAllergy(@Body allergy: AllergyDto): AllergyDto
}
```

---

### EJEMPLO 2: Confirmación Especializada de Citas (Non-Standard CRUD)

<!-- BAD EXAMPLE -->
❌ INCORRECTO:
```kotlin
// Malo: PUT genérico que puede sobrescribir datos de la cita y perder procedimientos asignados
@PUT("appointments/{id}")
suspend fun updateAppointment(@Path("id") id: Long, @Body appt: AppointmentDto)
```

<!-- GOOD EXAMPLE -->
✅ CORRECTO:
```kotlin
// data/remote/dto/ConfirmAppointmentRequest.kt
package com.example.biodontos.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmAppointmentRequest(
    @SerialName("confirmVal")
    val confirmVal: String = "Confirmed",
    @SerialName("defNum")
    val definitionNumber: Long? = null
)

// data/remote/api/OpenDentalAppointmentsApi.kt
package com.example.biodontos.data.remote.api

import com.example.biodontos.data.remote.dto.ConfirmAppointmentRequest
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface OpenDentalAppointmentsApi {
    @PUT("appointments/{aptNum}/Confirm")
    suspend fun confirmAppointment(
        @Path("aptNum") appointmentNumber: Long,
        @Body request: ConfirmAppointmentRequest
    )
}
```

---

### EJEMPLO 3: Odontograma Clínico, Superficies FDI y Periodontometría

<!-- BAD EXAMPLE -->
❌ INCORRECTO:
```kotlin
// Malo: Estructura plana, sin diferenciación FDI ni superficies anatómicas
data class ToothView(
    val id: String,
    val isDecayed: Boolean
)
```

<!-- GOOD EXAMPLE -->
✅ CORRECTO:
```kotlin
// domain/model/dental/OdontogramModels.kt
package com.example.biodontos.domain.model.dental

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ToothSurface {
    MESIAL, DISTAL, OCCLUSAL_OR_INCISAL, VESTIBULAR, LINGUAL_OR_PALATINE
}

enum class ClinicalCondition(val displayColor: Color, val requiresFollowUp: Boolean) {
    HEALTHY(Color(0xFFE8ECEF), false),
    CARIES(Color(0xFFE53935), true),
    AMALGAM_RESTORED(Color(0xFF546E7A), false),
    RESIN_RESTORED(Color(0xFF42A5F5), false),
    SEALED(Color(0xFF66BB6A), false),
    ENDODONTIC_TREATED(Color(0xFFFFA726), true),
    MISSING(Color.Transparent, false)
}

enum class PeriodontalSite {
    MESIO_VESTIBULAR, MID_VESTIBULAR, DISTO_VESTIBULAR,
    MESIO_LINGUAL, MID_LINGUAL, DISTO_LINGUAL
}

@Immutable
data class PeriodontalSiteMeasurement(
    val site: PeriodontalSite,
    val pocketDepthMm: Float, // Profundidad de sondaje en mm
    val gingivalRecessionMm: Float = 0f, // Recesión en mm
    val hasBleedingOnProbing: Boolean = false, // Sangrado (BOP)
    val hasSuppuration: Boolean = false // Supuración
) {
    // Pérdida de inserción clínica (CAL = PD + GR)
    val clinicalAttachmentLossMm: Float get() = pocketDepthMm + gingivalRecessionMm
    val isDeepPocket: Boolean get() = pocketDepthMm >= 4.0f
}

@Immutable
data class ToothState(
    val fdiNumber: Int, // Ej: 11..48 (Permanentes) o 51..85 (Temporales)
    val surfaces: Map<ToothSurface, ClinicalCondition> = emptyMap(),
    val periodontalSites: List<PeriodontalSiteMeasurement> = emptyList(),
    val isPresent: Boolean = true,
    val mobilityGrade: Int = 0 // Grados 0 a 3
) {
    init {
        val isPermanent = fdiNumber in 11..18 || fdiNumber in 21..28 || fdiNumber in 31..38 || fdiNumber in 41..48
        val isDeciduous = fdiNumber in 51..55 || fdiNumber in 61..65 || fdiNumber in 71..75 || fdiNumber in 81..85
        require(isPermanent || isDeciduous) { "Número dental FDI inválido: $fdiNumber" }
        require(mobilityGrade in 0..3) { "Grado de movilidad inválido (0-3): $mobilityGrade" }
    }
}
```

---

### EJEMPLO 4: ViewModel MVI con StateFlow y Manejo Seguro de Estados

```kotlin
// presentation/odontogram/OdontogramViewModel.kt
package com.example.biodontos.presentation.odontogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.dental.ClinicalCondition
import com.example.biodontos.domain.model.dental.ToothState
import com.example.biodontos.domain.model.dental.ToothSurface
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OdontogramUiEvent {
    data class SelectTooth(val fdiNumber: Int) : OdontogramUiEvent
    data class ApplySurfaceCondition(
        val fdiNumber: Int,
        val surface: ToothSurface,
        val condition: ClinicalCondition
    ) : OdontogramUiEvent
    data object SaveOdontogram : OdontogramUiEvent
}

sealed interface OdontogramUiEffect {
    data class ShowToast(val message: String) : OdontogramUiEffect
    data class RequestCriticalActionConfirmation(val message: String, val onConfirm: () -> Unit) : OdontogramUiEffect
}

data class OdontogramUiState(
    val patientId: PatientId? = null,
    val teeth: Map<Int, ToothState> = emptyMap(),
    val selectedFdiNumber: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class OdontogramViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OdontogramUiState())
    val uiState: StateFlow<OdontogramUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<OdontogramUiEffect>(Channel.BUFFERED)
    val effects = _effectChannel.receiveAsFlow()

    fun onEvent(event: OdontogramUiEvent) {
        when (event) {
            is OdontogramUiEvent.SelectTooth -> {
                _uiState.update { it.copy(selectedFdiNumber = event.fdiNumber) }
            }
            is OdontogramUiEvent.ApplySurfaceCondition -> {
                updateToothSurface(event.fdiNumber, event.surface, event.condition)
            }
            is OdontogramUiEvent.SaveOdontogram -> {
                saveChanges()
            }
        }
    }

    private fun updateToothSurface(fdi: Int, surface: ToothSurface, condition: ClinicalCondition) {
        _uiState.update { current ->
            val tooth = current.teeth[fdi] ?: ToothState(fdiNumber = fdi)
            val updatedSurfaces = tooth.surfaces.toMutableMap().apply { put(surface, condition) }
            val updatedTeeth = current.teeth.toMutableMap().apply { put(fdi, tooth.copy(surfaces = updatedSurfaces)) }
            current.copy(teeth = updatedTeeth)
        }
    }

    private fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Llamada al caso de uso correspondiente...
            _uiState.update { it.copy(isLoading = false) }
            _effectChannel.send(OdontogramUiEffect.ShowToast("Odontograma actualizado con éxito"))
        }
    }
}
```

---

### INSTRUCCIÓN PERMANENTE
A partir de este momento, **todas las implementaciones, modificaciones, arquitecturas y pruebas generadas en este repositorio DEBEN cumplir estrictamente estas REGLAS y sus patrones asociados**.
