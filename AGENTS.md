# AGENTS.md — Biodontos

Android dental-clinic app (Kotlin 2.0.21, Jetpack Compose/Material 3). One Gradle module: `:app`. Package `com.example.biodontos`. UI copy and product docs are Spanish; keep new user-facing strings Spanish.

## Build & verify

JDK 17 is required. `JAVA_HOME` is often unset in a fresh shell; Android Studio's JBR works:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

- Full JVM suite: `./gradlew testDebugUnitTest` (passes on `master` as of this file).
- Single test: `./gradlew testDebugUnitTest --tests "com.example.biodontos.presentation.auth.login.LoginViewModelTest"`
- Static analysis: `./gradlew detekt` — uses `config/detekt/detekt.yml` (many rules disabled). First run needs network because `detekt-cli` is not in the local Gradle cache.
- Android lint: `./gradlew lintDebug` — `lint.abortOnError = false`, so a green build does **not** mean lint is clean; read `app/build/reports/lint-results-debug.*`.
- Coverage gate: `./gradlew koverVerify` — 80% minimum with broad presentation/data excludes in `app/build.gradle.kts`.
- Instrumented tests: `./gradlew connectedAndroidTest` (needs emulator/device; current coverage is minimal).
- Gradle configuration cache is enabled. Fix custom tasks that break it instead of disabling it.

## Architecture & wiring

- `settings.gradle.kts` includes only `:app`. `domain`, `data`, `presentation`, and `core` are source packages, not Gradle modules. Don't propose multi-module changes unless asked.
- `com.example.biodontos.domain` must stay pure Kotlin: no `android.*` / `androidx.*` imports. Verify with:
  `powershell -ExecutionPolicy Bypass -File .agents/skills/quality-detekt-verify/scripts/verify-domain-purity.ps1`
- DI is manual. `DefaultAppContainer` (`core/di/AppContainer.kt`) is created in `BiodontosApp.onCreate`; ViewModels are built by per-feature factory objects that read `application.container`. Adding a use case means updating the `AppContainer` interface + implementation **and** the relevant factory.
- Entry flow: `MainActivity` → `BiodontosNavGraph` → type-safe `@Serializable` routes in `presentation/navigation/Screen.kt`. Doctor screens live in `presentation/doctor/**`, patient screens in `presentation/patientportal/**`; shared clinical screens in `presentation/patients`, `odontogram`, and `admission`.
- Feature convention: `XScreen.kt`, `XState.kt`, `XViewModel.kt`, `XViewModelFactory.kt` in the same package. Most ViewModels expose `state: StateFlow<...>` and a `Channel`-backed `events`/`effects` flow; follow the nearest existing screen.
- `DoctorPortalShell` / `PatientPortalShell` own window insets. Nested `Scaffold`s must set `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. Bottom actions must use `BiodontosBottomActionBar` / `Modifier.bottomActionInsets()`; forms use `BiodontosFormColumn` / `imePadding()`.
- Data access is Supabase PostgREST only (`core/network/SupabaseClient.kt`; DTOs in `data/dto`, mappers `toDomain()`/`toDto()` in `data/mapper`). Room and WorkManager are declared in Gradle but there is no database, DAO, or worker yet; there is also no Open Dental client. Don't assume offline-first or Open Dental integration exists.
- Repositories return `Result<T>` and rethrow `CancellationException`; newer scheduling/history use cases return Arrow `Either<DomainError, T>`. Match the neighboring pattern instead of converting a whole feature.
- Time-dependent logic uses `ClinicClock` / an injected `now` (`domain/scheduling`); don't call `LocalDateTime.now()` inside domain logic.
- Surface failures to the UI with `Throwable?.toUserFriendlyMessage()` (`core/util/ErrorMapper.kt`), not raw exception text.
- `SUPABASE_URL` / `SUPABASE_KEY` come from `local.properties` or root `.env`, parsed in `app/build.gradle.kts` and exposed via BuildConfig (committed fallbacks exist). `.env` is gitignored; never add secrets to source.

## Clinical & privacy invariants

- FDI: permanent `11..18 / 21..28 / 31..38 / 41..48`, deciduous `51..55 / 61..65 / 71..75 / 81..85`; `Tooth.init` validates.
- Periodontogram: 6 sites per tooth; `CAL = PD + GR`; `PD >= 4mm` is the deep-pocket alert; mobility `0..3`, furcation `0..4`.
- No `!!` in production or test code. No PHI (names, IDs, diagnoses, phones) in logs, Timber, or Crashlytics.
- Signed consent gates clinical writes. The admission flow uses `InformedConsentRepository` / `consentSigned`; `CheckConsentLockUseCase` exists but is not wired into writes yet — don't add a write path that bypasses consent.
- Critical/destructive actions (cancel, no-show, deletion, extraction, prescriptions) require `TwoStepConfirmationDialog`.

## Database changes

- No migration runner or Supabase CLI config. SQL is applied manually in the Supabase SQL editor: root `MIGRATION_*.sql` / `FIX_*.sql` plus `supabase/migrations/`.
- Ship schema + RLS policies together, then keep DTOs, mappers, and domain models in sync. Constraint/trigger names surface in `ErrorMapper`; update it when adding DB errors.

## Docs & skills

- `GEMINI.md` and `.agents/rules/dental_architecture_rules.md` hold the long-form clinical/architecture rules. `.agents/skills/*` provides the feature-scaffold, privacy-mapper, use-case, clinical-sync, and quality-gate workflows — load the matching skill before large features.
- Some docs are stale: `GEMINI.md` shows `@Immutable` in `domain` and DTOs in `data/remote/dto`; `app/action-plan.md` uses package `com.dental.clinic` and SDK 36. The real code keeps `domain` framework-free, DTOs in `data/dto`, and builds with `compileSdk/targetSdk 35`, `minSdk 26`.
- Tests use JUnit 4, MockK, Turbine, and `MainDispatcherRule` (default `UnconfinedTestDispatcher`) in `app/src/test`. Add tests for new mappers, use cases, and ViewModels.
