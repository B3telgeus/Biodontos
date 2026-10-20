# AGENTS.md - CONTEXTO GENERAL Y ARQUITECTURA DE BIODONTOS

Bienvenido a **Biodontos**. Este archivo sirve como el mapa central y guía ejecutiva que todo agente, subagente u orquestador debe leer para entender la visión técnica, la arquitectura, el estado de desarrollo y los comandos operativos de la solución.

---

## 1. PROPÓSITO DEL PROYECTO
**Biodontos** es una plataforma odontológica integral diseñada para profesionales de la salud dental y pacientes. Ofrece gestión de historias clínicas, fichas de evolución, consentimiento informado, odontograma reactivo interactivo (FDI) y periodontometría, con soporte para sincronización en la nube (Supabase / Open Dental API) y capacidades offline-first.

---

## 2. STACK TÉCNICO Y PRINCIPIOS DE INGENIERÍA

* **Lenguaje:** **Kotlin 2.0+** puro en Dominio y Casos de Uso.
* **UI & Diseño:** **Jetpack Compose** (Compose BOM, Material 3, tema clínico personalizado [DentalTheme](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/designsystem/DentalTheme.kt)).
* **Navegación:** **Navigation Compose 2.8+ Type-Safe** mediante rutas declaradas con `@Serializable object / data class` en [Screen.kt](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/presentation/navigation/Screen.kt).
* **Gestión de Estado:** **MVI / MVVM reactivo unidireccional (UDF)**:
  - `StateFlow<UiState>` inmutable expuesto a la vista.
  - `Channel<UiEffect>.receiveAsFlow()` para eventos únicos (toasts, navegación, confirmaciones).
  - Consumo en UI con `collectAsStateWithLifecycle()`.
* **Inyección de Dependencias:**
  - *Estado actual:* **Inyección de Dependencias Manual** centralizada en [AppContainer](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/di/AppContainer.kt) y [DefaultAppContainer](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/di/AppContainer.kt), consumida mediante `ViewModelProvider.Factory`.
  - *Evolución hacia Hilt:* En la transición a arquitectura multimódulo, se prevé la adopción de **Hilt** (`@HiltAndroidApp`, `@HiltViewModel`) para facilitar la inyección entre módulos de features.
* **Manejo de Errores y Tipado Funcional:**
  - Uso de `Result<T>` y tipado funcional estilo Arrow-kt (`Either<DomainError, T>`).
  - Prohibido propagar excepciones genéricas no controladas a la capa de UI.
* **Red y Persistencia:**
  - **Supabase Kotlin SDK** (Auth, Postgrest, Realtime) + **Ktor Client** (OkHttp engine).
  - **Room Database** (KSP, Coroutines/Flow) para soporte local y caché offline.
  - **Open Dental REST / FHIR API** para integración con historias clínicas externas.
* **Testing:** **JUnit 4**, **MockK**, **kotlinx-coroutines-test** (`StandardTestDispatcher`, `runTest`), y **Compose UI Test**.

---

## 3. ESTRUCTURA DE CAPAS Y MODELO MULTIMÓDULO

El proyecto sigue los principios de **Clean Architecture** y está diseñado para una modularización limpia:

```text
Biodontos/
├── :domain                     # Reglas de negocio puras (Kotlin puro, POKOs, sin Android/UI)
│   ├── model/                  # Entidades (Patient, Tooth, UserSession, Allergy, PeriodontalSite)
│   ├── repository/             # Interfaces de contratos (AuthRepository, OdontogramRepository, etc.)
│   └── usecase/                # Casos de uso con `suspend operator fun invoke()`
│
├── :infrastructure / :data     # Implementaciones concretas de acceso a datos
│   ├── remote/                 # Clientes Supabase, Ktor, Open Dental DTOs (@SerialName)
│   ├── local/                  # Room Entities, DAOs y Database
│   └── repository/             # Implementaciones de repositorios y Mappers (toDomain / toDto)
│
├── :core                       # Módulos transversales compartidos
│   ├── designsystem/           # Tokens de diseño (DentalTheme, colores, tipografía, formas)
│   ├── di/                     # Contenedores de dependencias (AppContainer / módulos Hilt)
│   └── util/                   # Utilidades clínicas, formateadores de fechas, validadores
│
└── :presentation / :feature:*  # Módulos de interfaz de usuario y flujos de usuario
    ├── :feature:auth           # Onboarding, Selección de Rol, Login, Registro Doctor/Paciente
    ├── :feature:patients       # Lista de pacientes, registro, anamnesis y consentimiento
    ├── :feature:odontogram     # Renderizado interactivo Canvas FDI adulto y pediátrico
    └── :feature:appointments   # Gestión de citas y confirmaciones (Open Dental)
```

---

## 4. PROGRESO ACTUAL DEL PROYECTO Y FASES

| Fase | Módulo / Funcionalidad | Estado | Detalles Técnicos Clave |
| :--- | :--- | :---: | :--- |
| **Fase 1** | **Infraestructura Base & Clean Architecture** | ✅ Completada | Configuración de Gradle KTS, Supabase BOM, Room, Ktor, estructura Clean y [AppContainer](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/di/AppContainer.kt). |
| **Fase 2** | **Gestión de Pacientes & Fichas Clínicas** | ✅ Completada | Registro de pacientes, validaciones defensivas de DNI/Nombres, consentimiento informado legal (`CheckConsentLockUseCase`), persistencia en Supabase. |
| **Fase 3** | **Odontograma Interactivo (Adulto)** | ✅ Completada | Renderizado sobre Jetpack Compose Canvas, mapeo por superficies dentales (M, D, O, V, L), estados patológicos y suite de pruebas unitarias. |
| **Fase 4** | **Autenticación y Onboarding** | ✅ Completada | Flujos de Login, Onboarding, selección Doctor/Paciente, recuperación de clave, integración con Supabase Auth y pruebas de ViewModels con MockK. |
| **Fase 5** | **Periodontometría & Odontograma Infantil** | 🔄 En curso / Próxima | Sondaje de 6 sitios (MV, V, DV, ML, L, DL), cálculo de CAL, dentición temporal FDI (51..85). |
| **Fase 6** | **Integración Open Dental & Offline Sync** | 🔄 En curso / Próxima | Endpoints especializados (`/Confirm`, `/Note`), sincronización en segundo plano con WorkManager. |

---

## 5. COMANDOS PRINCIPALES DE GRADLE Y OPERACIÓN

El agente debe utilizar estos comandos para compilar, verificar y validar el código:

* **Compilación del proyecto:**
  ```powershell
  ./gradlew assembleDebug
  ```
* **Ejecución de Pruebas Unitarias:**
  ```powershell
  ./gradlew test
  # O ejecutar específicamente los tests unitarios de depuración:
  ./gradlew testDebugUnitTest
  ```
* **Ejecución de una suite de pruebas específica:**
  ```powershell
  ./gradlew testDebugUnitTest --tests "com.example.biodontos.presentation.auth.login.LoginViewModelTest"
  ```
* **Pruebas Instrumentadas / UI de Compose (con emulador/dispositivo conectado):**
  ```powershell
  ./gradlew connectedAndroidTest
  ```
* **Análisis Estático y Calidad de Código:**
  ```powershell
  ./gradlew lint
  # Cuando detekt esté habilitado en el pipeline:
  ./gradlew detekt
  ```
* **Limpieza y Reconstrucción:**
  ```powershell
  ./gradlew clean build
  ```

---

## 6. REGLAS INNEGOCIABLES DE CÓDIGO Y SEGURIDAD CLÍNICA

1. **Cero `!!` (Doble Bang):** Manejo seguro con Elvis operator (`?:`), safe calls (`?.`) o `requireNotNull()` con mensaje descriptivo.
2. **Privacidad Sanitaria (HIPAA / GDPR Salud):** Prohibido registrar PHI (Protected Health Information: nombres, diagnósticos, identificadores) en logs de consola, Timber o Crashlytics.
3. **Consentimiento Informado Bloqueante:** Ninguna intervención clínica o registro de evolución puede guardarse si el consentimiento informado no ha sido verificado y aprobado.
4. **Respeto a las reglas de Open Dental y Odontograma:** Seguir estrictamente las directrices anatómicas FDI y los endpoints especializados definidos en [GEMINI.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/GEMINI.md).
5. **Edge-to-Edge y Consciencia de Insets (Android 15+):** Prohibido renderizar botones o acciones al pie sin insets (`BiodontosBottomActionBar`, `Modifier.bottomActionInsets()`). Ningún botón de confirmación, guardado o navegación puede quedar debajo de la barra de navegación del sistema o del teclado.
