<!-- Antigravity Rule: dental_architecture_rules.md -->
# REGLAS DE ARQUITECTURA Y CALIDAD CLÍNICA - BIODONTOS

Este archivo define las reglas y estándares de desarrollo para el proyecto odontológico **Biodontos** (Kotlin / Android / Jetpack Compose / Open Dental API).

---

## 1. Clean Architecture y Unidirectional Data Flow (UDF)
- **Separación de capas:** `data`, `domain`, `presentation`, `core`.
- **Modelos de Dominio puros:** POKOs inmutables, sin dependencias de frameworks ni anotaciones de serialización.
- **DTOs aislados:** Los DTOs pertenecen exclusivamente a `data/remote/dto` o `data/local/entity`. Mappers explícitos `toDomain()` y `toDto()`.
- **StateFlow atómico:** Exponer únicamente `StateFlow<UiState>` inmutable con `_uiState.asStateFlow()`.
- **Side-effects:** Canales desacoplados (`Channel<UiEffect>(Channel.BUFFERED).receiveAsFlow()`) para navegación o notificaciones no persistentes.
- **Compose lifecycle:** Consumir flujos en Compose con `collectAsStateWithLifecycle()`.
- **Optimización de recomposición:** Anotar estados y modelos de UI con `@Immutable` o `@Stable`.

---

## 2. Open Dental REST / FHIR API
- **Nombres de atributos:** Open Dental usa `PascalCase` en campos y rutas plural minúsculas (`/patients`, `/allergies`, `/appointments`). En Kotlin, usar `camelCase` estándar con `@SerialName("PascalCase")` (`kotlinx.serialization`).
- **Endpoints especializados obligatorios:** Prohibido el uso de `PUT` genérico destructivo en citas (`/appointments/{id}`). Utilizar endpoints transaccionales específicos:
  - `/appointments/{id}/Confirm`
  - `/appointments/{id}/Note`
- **Paginación obligatoria:** Incluir siempre parámetros `Limit` y `Offset` en llamadas masivas.
- **Tipado seguro de fechas e IDs:** Convertir fechas de Open Dental (`yyyy-MM-dd HH:mm:ss`) a `java.time.LocalDate` / `LocalDateTime`. Usar Value Classes (`@JvmInline value class PatientId(val value: Long)`).

---

## 3. Odontograma y Periodontograma
- **Nomenclatura FDI estándar:** Validar rango permanente (11..48) y pediátrico/temporal (51..85).
- **Superficies dentales:** Modelar individualmente Mesial, Distal, Oclusal/Incisal, Vestibular/Bucal, Lingual/Palatino con estados clínicos específicos (Sano, Caries, Resina, Amalgama, Sellante, Endodoncia, Ausente).
- **Periodontometría de 6 sitios:** Vestibular (MV, V, DV) y Lingual/Palatino (ML, L/P, DL).
  - Medir Pocket Depth (PD mm, alerta >= 4mm), Recesión Gingival (GR mm), Sangrado (BOP), Supuración, Movilidad (0..3).
  - Calcular Pérdida de Inserción Clínica: `CAL = PD + GR`.
- **Canvas desacoplado:** Renderizar sobre coordenadas normalizadas relativas independientes de la resolución del dispositivo. Detección táctil mediante hit-testing matemático vectorial.

---

## 4. Seguridad, Privacidad y Cumplimiento Clínico (HIPAA)
- **Cero datos de salud en logs:** Prohibido imprimir nombres de pacientes, historiales, teléfonos o diagnósticos en Logcat/Crashlytics.
- **Confirmación en acciones críticas:** Diálogos explícitos de 2 pasos para cancelación de citas, exodoncias o eliminación de registros.
- **Seguridad local:** Cifrado en reposo para datos de salud almacenados localmente.

---

## 5. Pruebas y Calidad de Código
- Prohibido el uso de `!!` (doble bang).
- Tests unitarios obligatorios con `Turbine` y `kotlinx-coroutines-test` para ViewModels y Mappers.
- Tests de validación de invariantes dentales FDI y fórmulas de periodoncia.
