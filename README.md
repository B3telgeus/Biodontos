# Biodontos — Sistema de Gestión Clínica y Portal Odontológico

Aplicación móvil nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** orientada a la gestión clínica odontológica integral (historia clínica, anamnesis sistémica, odontograma FDI, periodontograma, consentimientos informados con firma digital, control de materiales clínicos con trazabilidad INVIMA, evoluciones y facturación) junto con un portal de autogestión para pacientes.

---

## 1. Requisitos del Entorno de Desarrollo

- **JDK**: **JDK 21** (configurado automáticamente mediante Gradle Foojay Toolchain Resolver en `gradle/gradle-daemon-jvm.properties`; compatible con Android Studio Ladybug / Meerkat con JBR 21+).
- **Android SDK**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (Android 8.0+).
- **Gradle**: `9.7.1` (ejecutado mediante el wrapper incluido `./gradlew` o `.\gradlew.bat`).
- **Kotlin / KSP**: Kotlin `2.0.21` con KSP `2.0.21-1.0.25`.

---

## 2. Configuración de Variables de Entorno (`.env` / `local.properties`)

Por seguridad, el proyecto **no incluye credenciales hardcodeadas** en `app/build.gradle.kts`. Si las variables `SUPABASE_URL` o `SUPABASE_KEY` no están definidas, la compilación fallará con un mensaje explícito.

1. Copia el archivo de plantilla `.env.example` a `.env` en la raíz del proyecto:
   ```bash
   cp .env.example .env
   ```
2. Edita `.env` (o agrega las claves en `local.properties`, ambos ignorados por Git) con las credenciales de tu proyecto Supabase:
   ```properties
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_KEY=tu-anon-public-key
   ```
3. Asegúrate de que `local.properties` apunte a tu Android SDK local (Android Studio lo genera automáticamente):
   ```properties
   sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
   ```

---

## 3. Migraciones de Base de Datos (Supabase PostgreSQL)

Todas las migraciones SQL se encuentran versionadas en [`supabase/migrations/`](supabase/migrations/) y deben ejecutarse en el siguiente **orden cronológico estricto** (vía `supabase db push` o desde el SQL Editor de Supabase):

1. **[`20261001000000_base_schema.sql`](supabase/migrations/20261001000000_base_schema.sql)**
   - Crea las 15 tablas clínicas base (`patients`, `doctors`, `clinical_records`, `anamnesis`, `odontogram_teeth`, `periodontal_examinations`, `informed_consents`, `evolutions`, `treatments`, `payments`, `appointments`, `clinical_audit_logs`, etc.).
   - Configura Row Level Security (RLS), funciones auxiliares seguras (`is_doctor()`) y el trigger `on_auth_user_created` con `SECURITY DEFINER SET search_path = public, pg_temp`.
2. **[`20261013_patient_portal.sql`](supabase/migrations/20261013_patient_portal.sql)**
   - Extiende el esquema para el portal de pacientes, disponibilidad semanal (`doctor_availability`), restricciones de no solapamiento de citas y políticas RLS del portal.
3. **[`20261020000000_materials_and_atomic_evolutions.sql`](supabase/migrations/20261020000000_materials_and_atomic_evolutions.sql)**
   - Crea el catálogo `clinical_materials` (con validaciones INVIMA y lote) y `evolution_materials_used`.
   - Configura el bucket de Supabase Storage `doctor-avatars` y sus políticas RLS.
   - Define la función RPC transaccional `save_evolution_with_materials` (`SECURITY INVOKER SET search_path = public, pg_temp`) que persiste de forma atómica la evolución clínica, los insumos utilizados y el cierre opcional de la historia clínica.

Consulta [`supabase/migrations/README.md`](supabase/migrations/README.md) para más detalles sobre la aplicación y verificación de migraciones.

---

## 4. Arquitectura y Convenciones Técnicas

El proyecto sigue **Clean Architecture** dividida en cuatro capas bajo `com.example.biodontos`:

- **`domain/`**: Modelos inmutables, interfaces de repositorio, políticas de programación (`AppointmentModificationPolicy`, `SlotCalculator`) y casos de uso con un único `suspend operator fun invoke`:
  - `domain/usecase/appointments/`: Casos de uso unificados de agendamiento (`BookAppointmentUseCase` con validación de 2 horas de anticipación vía `ClinicClock`, `CancelAppointmentUseCase` con política de ventana de modificación y motivo opcional, `RescheduleAppointmentUseCase`, `GetAvailableSlotsUseCase`, `GetDoctorAppointmentsUseCase`, `GetPatientAppointmentsUseCase`, `GetDoctorBusySlotsUseCase`, `ConfirmAppointmentUseCase`, `ToggleAppointmentConfirmationUseCase`).
  - `domain/usecase/clinical/`: Orquestación clínica desacoplada del ViewModel (`SaveAdmissionEvolutionAndCloseUseCase` delegando atomicidad al RPC `save_evolution_with_materials`, `ScheduleAdmissionFollowUpUseCase`, `CheckConsentLockUseCase`).
- **`data/`**:
  - **Remoto (Supabase)**: Implementaciones de repositorio con inyección por constructor de `SupabaseClient` y `CoroutineDispatcher`, preservando siempre la propagación de `CancellationException`.
  - **Imágenes y Storage**: `AvatarRepositoryImpl` e `ImageProcessor` procesan imágenes fuera del hilo principal (`inJustDecodeBounds` + `inSampleSize` + orientación EXIF) y las suben al bucket `doctor-avatars` con invalidación de caché, manteniendo soporte retrocompatible para avatares Base64 legados.
  - **Local (Room + DataStore)**: `BiodontosDatabase` (esquema exportado en `app/schemas/`), `PatientDao`, `MaterialDao`, `AdmissionDraftDao` (auto-guardado debounced de borradores de admisión aislados por `(doctorId, patientId)`) y `SessionLocalCleaner` (purga local completa al cerrar sesión).
- **`core/`**:
  - **Errores tipados (`ErrorMapper.kt`)**: Jerarquía sellada `AppError` (`Throwable?.toAppError(): AppError`) con `@StringRes messageRes` que clasifica códigos SQLSTATE/PostgREST (`23505`, `23503`, `23514`, `22P02`, `42501`, `PGRST200`), errores HTTP y timeouts sin exponer nombres de tablas ni scripts SQL al usuario final.
  - **Internacionalización (`UiText.kt` + `res/values/strings.xml`)**: Todos los textos de interfaz y descripciones de accesibilidad (`contentDescription`) están externalizados en `strings.xml`.
- **`presentation/`**: Pantallas en Jetpack Compose con estado inmutable expuesto vía `StateFlow`, `SavedStateHandle` en flujos multipaso (almacenando únicamente IDs y pasos de navegación, nunca firmas ni Base64 en el `Bundle`), limpieza completa del backstack al autenticar o cerrar sesión (`popUpTo(navController.graph.id) { inclusive = true }`), semántica de encabezados (`heading()`) y contraste verificado bajo WCAG AA.

---

## 5. Comandos de Compilación, Pruebas y Calidad

Ejecuta los siguientes comandos desde la raíz del proyecto (`Biodontos-master`):

```powershell
# 1. Ejecutar toda la suite de pruebas unitarias JVM y Robolectric
.\gradlew.bat testDebugUnitTest

# 2. Ejecutar análisis estático de código con Detekt
.\gradlew.bat detekt

# 3. Verificar cobertura de pruebas con Kover (umbral mínimo >= 80%)
.\gradlew.bat koverVerify

# 4. Compilar el APK de depuración (Debug)
.\gradlew.bat assembleDebug

# 5. Ejecutar verificación integral en un solo comando
.\gradlew.bat testDebugUnitTest detekt koverVerify assembleDebug
```
