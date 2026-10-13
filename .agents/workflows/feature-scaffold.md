# WORKFLOW: /feature-scaffold (Generación E2E de Funcionalidad Odontológica)

Este workflow automatiza la creación de una nueva característica clínica de extremo a extremo, desde las reglas de negocio puras hasta la interfaz en Jetpack Compose y sus tests unitarios.

---

## Pipeline de Ejecución por Capas

```text
┌────────────────────────────────────────────────────────┐
│  Paso 1: Dominio Puro (:domain)                         │
│  Activar skill: dental-usecase-generator                │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  Paso 2: Datos e Infraestructura (:data)               │
│  Activar skill: dental-patient-privacy-mapper          │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  Paso 3: Inyección de Dependencias (:core:di)          │
│  Registrar repositorios y casos de uso en AppContainer │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  Paso 4: Presentación y Canvas Compose (:presentation)  │
│  Activar skill: dental-odontogram-compose              │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  Paso 5: Testing Automatizado                          │
│  Generar pruebas con MockK, Turbine y runTest          │
└────────────────────────────────────────────────────────┘
```

---

## 1. Paso 1 — Capa de Dominio (`:domain`)
- **Acción:** Activar la skill `dental-usecase-generator`.
- **Reglas del Paso:**
  - Crear o actualizar entidades en `domain/model/`.
  - Usar Value Classes para identificadores anatómicos (`@JvmInline value class ToothFdi(val value: Int)`).
  - Validar rangos FDI (permanentes 11..48, deciduos 51..85).
  - Implementar caso de uso con `suspend operator fun invoke(...)`.
  - Retornar `Either<DomainError, Output>` o `Result<Output>`.
  - Integrar verificación obligatoria de consentimiento informado si la acción es clínica o invasiva.

---

## 2. Paso 2 — Capa de Datos e Infraestructura (`:data`)
- **Acción:** Activar la skill `dental-patient-privacy-mapper`.
- **Reglas del Paso:**
  - Definir DTOs con `@SerialName("PascalCase")` para compatibilidad con Open Dental / Supabase.
  - Implementar funciones mapper `toDomain()` y `toDto()`.
  - Asegurar cumplimiento HIPAA: prohibido imprimir PHI en logs; enmascarar identificadores de pacientes.
  - Definir Room Entity / DAO si la funcionalidad requiere persistencia local offline.
  - Implementar la interfaz del repositorio en `data/repository/`.

---

## 3. Paso 3 — Inyección de Dependencias (`:core:di`)
- **Acción:** Registrar dependencias en [AppContainer.kt](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/di/AppContainer.kt).
- **Reglas del Paso:**
  - Declarar la propiedad en `AppContainer`.
  - Proveer la instancia perezosa en `DefaultAppContainer` (`by lazy { ... }`).
  - No usar anotaciones `@HiltViewModel` o `@Inject` mientras el proyecto utilice DI manual.

---

## 4. Paso 4 — Capa de Presentación (`:presentation`)
- **Acción:** Activar la skill `dental-odontogram-compose`.
- **Reglas del Paso:**
  - Definir `UiState` inmutable con `@Immutable` y eventos `UiEvent`.
  - Crear el `ViewModel` exponiendo `StateFlow<UiState>` y `Channel<UiEffect>`.
  - Implementar el patrón dual de Composables:
    1. **Stateful Composable:** Consume `collectAsStateWithLifecycle()` y maneja eventos.
    2. **Stateless Composable (`@VisibleForTesting`):** Puro, recibe únicamente estado y lambdas, con soporte para `@Preview`.
  - Registrar la ruta tipada en `Screen.kt` (`@Serializable data class / object`) y añadir el nodo en `NavGraph.kt`.
  - Respetar zona táctil mínima de 48 × 48 dp y colores de `DentalTheme`.

---

## 5. Paso 5 — Testing Automatizado
- **Acción:** Crear suite de pruebas en `app/src/test/`.
- **Reglas del Paso:**
  - Testear Caso de Uso cubriendo flujo de éxito y fallos clínicos (`ConsentNotGranted`, `InvalidToothNumber`).
  - Testear ViewModel usando `StandardTestDispatcher`, `Turbine` y `MockK`.
  - Verificar que el pipeline compila y pasa todas las pruebas:
    ```powershell
    ./gradlew testDebugUnitTest
    ```
