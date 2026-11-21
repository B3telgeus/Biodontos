# Biodontos — Playbook de Pruebas, ADB y Estabilidad Compose

Guía operativa para ejecutar pruebas Compose en host (Robolectric), previews multipantalla (`@BiodontosPreviews`), Gradle Managed Devices (GMD), suites instrumentadas vía ADB con Test Orchestrator y control de estabilidad del compilador Compose.

---

## 1. Prerrequisitos del entorno

- **Credenciales placeholder / locales**: `app/build.gradle.kts` exige `SUPABASE_URL` y `SUPABASE_KEY` en `.env`, `local.properties` o variables de entorno. En CI se inyectan placeholders seguros; en local copia `.env.example` a `.env`.
- **Java / Gradle Toolchain**: el daemon de Gradle usa **JDK 21** (`gradle/gradle-daemon-jvm.properties`) y el bytecode compila con **Java 17** (`compileOptions` / `jvmTarget = "17"`).
- **Platform-Tools (`adb`)**: mantén un único `adb` en `$PATH` (idealmente `$ANDROID_HOME/platform-tools`) para evitar colisiones de versión en el servidor TCP `5037`. Nunca borres `~/.android/adbkey*` para resolver un estado `unauthorized`.

---

## 2. Pruebas Unitarias y Compose en Host (Robolectric)

La infraestructura base vive en [`app/src/test/java/com/example/biodontos/ui/ComposeTestSupport.kt`](../app/src/test/java/com/example/biodontos/ui/ComposeTestSupport.kt) y [`ComposeInfraSmokeTest.kt`](../app/src/test/java/com/example/biodontos/ui/ComposeInfraSmokeTest.kt), con `unitTests.isIncludeAndroidResources = true` habilitado en [`app/build.gradle.kts`](../app/build.gradle.kts).

```powershell
# Toda la suite JVM + Robolectric Compose (Windows)
.\gradlew.bat :app:testDebugUnitTest

# Una clase puntual
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.biodontos.ui.ComposeInfraSmokeTest"
```

```bash
# Linux / macOS
./gradlew :app:testDebugUnitTest
```

### Reglas para pruebas Compose en `src/test/`
- Usa `@RunWith(AndroidJUnit4::class)` y `@Config(sdk = [34])`.
- Usa `composeTestRule.setBiodontosContent { ... }` para envolver el árbol en `DentalTheme`.
- En listas perezosas (`LazyColumn`, `LazyRow`, `LazyVerticalGrid`), realiza `performScrollToKey(...)` / `performScrollToIndex(...)` sobre el contenedor con `testTag` y valida visibilidad con `assertIsDisplayed()` (no `assertExists()`).
- **No uses `Thread.sleep`** en `src/test/` (usa `mainClock` o `waitUntil*`).
- **No llames `enableAccessibilityChecks()` en `src/test/`** (Robolectric no ejecuta servicios reales de accesibilidad — b/332778271). Reserva las comprobaciones reales de accesibilidad y RenderThread para `src/androidTest/` y GMD.

---

## 3. Previews con `@BiodontosPreviews` y Capturas en CI

La anotación multiprevisualización [`BiodontosPreviews`](../app/src/main/java/com/example/biodontos/core/designsystem/BiodontosPreviews.kt) genera 4 variantes por defecto:
- `phone` (fondo claro estándar)
- `dark` (`UI_MODE_NIGHT_YES`)
- `font 1.5x` (`fontScale = 1.5f`)
- `compact` (`widthDp = 320`)

Detekt ya excluye `Preview` y `BiodontosPreviews` en `UnusedPrivateMember` (`config/detekt/detekt.yml`).

### Buenas prácticas de Previews
- Eleva el estado: los `@Preview` deben invocar `*Content(state = ..., onEvent = {})` con datos deterministas, nunca un `ViewModel` real.
- Usa `LocalInspectionMode.current` en componentes con carga remota (ej. avatares Coil) para mostrar placeholder estable.
- Nombra las funciones terminando en `*Preview` (o ubícalas bajo `presentation.previews`) para que el script de estabilidad las excluya automáticamente del baseline.

### Captura automatizada en CI
El workflow [`.github/workflows/preview-screenshots.yml`](../.github/workflows/preview-screenshots.yml) arranca un emulador `pixel_6` (API 31, KVM), instala `app-debug.apk`, inicia `com.example.biodontos/.MainActivity`, limpia `.hotswan/preview-captures/` y:
1. Si el plugin **HotSwan** (`com.github.skydoves.compose.hotswan.compiler`) está habilitado en Gradle, ejecuta `./gradlew :app:captureAllPreviews`.
2. Mientras HotSwan permanezca en modo opt-in, ejecuta un respaldo determinista por ADB (System UI Demo Mode + modo claro/oscuro vía `cmd uimode night` + `adb exec-out screencap -p`) generando `.hotswan/preview-captures/index.html` y publicándolo en GitHub Pages (`deploy`).

---

## 4. Gradle Managed Devices (GMD)

Los dispositivos virtuales reproducibles están declarados en `android.testOptions.managedDevices` dentro de [`app/build.gradle.kts`](../app/build.gradle.kts):

| Nombre | Dispositivo | API | Imagen (`systemImageSource`) | Uso recomendado |
|---|---|---|---|---|
| `pixel2api30` | Pixel 2 | 30 | `aosp-atd` | Gate CI rápido y liviano (ATD) |
| `pixel6api34` | Pixel 6 | 34 | `google-atd` | Validación moderna con Google APIs (ATD) |
| `ciMatrix` (grupo) | Ambos | 30 + 34 | ATD | Matriz completa |

```bash
# Ejecutar un dispositivo específico
./gradlew :app:pixel2api30DebugAndroidTest
./gradlew :app:pixel6api34DebugAndroidTest

# Ejecutar toda la matriz del grupo ciMatrix
./gradlew :app:ciMatrixGroupDebugAndroidTest

# Ejecutar todos los dispositivos declarados
./gradlew :app:allDevicesCheck
```

- Reportes HTML: `app/build/reports/androidTests/managedDevice/`
- Artefactos adicionales (`TestStorage`): `app/build/outputs/managed_device_android_test_additional_output/`

---

## 5. Flujo ADB, Wi-Fi, Logcat, Screenrecord y Test Orchestrator

Usa los scripts herméticos incluidos en [`tools/ci/run-instrumented-adb.sh`](../tools/ci/run-instrumented-adb.sh) (Linux/macOS/CI) y [`tools/ci/run-instrumented-adb.ps1`](../tools/ci/run-instrumented-adb.ps1) (Windows PowerShell).

### 5.1 Ejecución hermética de pruebas instrumentadas

```bash
# Un dispositivo específico, instalando APKs y filtrando por tamaño o clase
bash tools/ci/run-instrumented-adb.sh -s emulator-5554 --install --size medium
bash tools/ci/run-instrumented-adb.sh -s emulator-5554 --class com.example.biodontos.ExampleInstrumentedTest#useAppContext

# Fan-out paralelo con sharding automático across N dispositivos conectados
bash tools/ci/run-instrumented-adb.sh --parallel --install

# Con AndroidX Test Orchestrator (pm clear entre cada @Test)
bash tools/ci/run-instrumented-adb.sh -s emulator-5554 --orchestrator
```

```powershell
# Equivalente en Windows PowerShell
.\tools\ci\run-instrumented-adb.ps1 -Serial emulator-5554 -Install -Size medium
.\tools\ci\run-instrumented-adb.ps1 -Parallel -Install -Orchestrator
```

Lo que garantizan automáticamente ambos scripts:
1. `adb wait-for-device` + espera a `getprop sys.boot_completed == 1` (nota: `wait-for-device-online` no existe).
2. Reintentos acotados con `adb kill-server; adb start-server` ante errores transitorios de transporte.
3. Preámbulo hermético: `pm clear com.example.biodontos` (`am force-stop` por sí solo no borra Room/SharedPreferences), escalas de animación a `0` y `logcat -c`.
4. Ejecución de `am instrument -w -r` envuelta en `timeout` (nunca `adb -t`, que es `transport_id`).
5. Evaluación de éxito/fallo leyendo **exclusivamente** `INSTRUMENTATION_STATUS_CODE` (`-1` error y `-2` fallo hacen fallar el run; `-3` ignored y `-4` assumption failure son skips). El `$?` de `am instrument` siempre es `0` aunque fallen pruebas.
6. Captura en fallo bajo `artifacts/<serial>/`: `fail.png` (`adb exec-out screencap -p`), `logcat.txt`, `crash.txt`, `events.txt`, `dumpsys-activity.txt` y datos privados vía `adb exec-out run-as com.example.biodontos tar cf - .`.
7. Limpieza garantizada en `trap cleanup EXIT` / `finally` restaurando animaciones a `1` y limpiando forwards/reverses.

### 5.2 Test Orchestrator

En [`app/build.gradle.kts`](../app/build.gradle.kts) el Orchestrator se declara con `androidTestUtil(libs.androidx.test.orchestrator)` (nunca `androidTestImplementation`, porque es un APK separado).
Al invocarlo directamente por ADB, el **runner posicional** es el Orchestrator y `AndroidJUnitRunner` va en `-e targetInstrumentation`:

```bash
adb shell am instrument -w -r \
  -e clearPackageData true \
  -e targetInstrumentation com.example.biodontos.test/androidx.test.runner.AndroidJUnitRunner \
  androidx.test.orchestrator/.AndroidTestOrchestrator
```

### 5.3 Depuración inalámbrica (Wi-Fi ADB, Android 11+)

En Android 11+ prefiere `adb pair` (TLS) sobre el antiguo `adb tcpip 5555`. Ten presente que el **puerto de emparejamiento** es distinto al **puerto de conexión**:

```bash
# Opción directa con el helper:
bash tools/ci/run-instrumented-adb.sh wifi-pair 192.168.1.42:42999 123456

# O manual paso a paso:
adb mdns check
adb pair 192.168.1.42:42999        # ingresa código de 6 dígitos
adb mdns services                  # busca el puerto en _adb-tls-connect._tcp
adb connect 192.168.1.42:39555     # usa el puerto de conexión, NO el de pairing
```

> Nota: desactiva "Depuración inalámbrica" cuando termines y evita usarla en redes Wi-Fi públicas o corporativas con aislamiento P2P.

### 5.4 Port Forwarding: `forward` vs `reverse` (orden opuesto)

```bash
# adb forward LOCAL REMOTE  -> Host accede a un puerto/socket del dispositivo
bash tools/ci/run-instrumented-adb.sh forward -s emulator-5554 tcp:9222 localabstract:chrome_devtools_remote

# adb reverse REMOTE LOCAL  -> Dispositivo accede a un servicio local en tu PC (orden inverso!)
bash tools/ci/run-instrumented-adb.sh reverse -s emulator-5554 tcp:9000 tcp:8080
```

### 5.5 Logcat, Screencap y Screenrecord

```bash
# Limpiar buffer, reproducir y capturar logs filtrados en el dispositivo (no pipear a grep)
adb logcat -c
adb logcat "ActivityManager:I Biodontos:D *:S"
adb logcat -d -v threadtime > artifacts/logcat.txt
adb logcat -d -b crash -v threadtime > artifacts/crash.txt
adb logcat -d -b events -v descriptive > artifacts/events.txt
adb logcat -t 100                  # últimas 100 líneas y sale (-T 100 sigue streameando)

# Captura PNG binaria segura (evita corrupción LF->CRLF en Windows)
adb exec-out screencap -p > screen.png

# Grabación MP4 (límite duro 180s, sin audio; detener con SIGINT para finalizar el MP4)
adb shell screenrecord --bugreport --time-limit 180 /sdcard/repro.mp4 &
# ... cuando termines:
adb shell pkill -INT screenrecord
adb pull /sdcard/repro.mp4 ./repro.mp4

# Extraer base de datos o preferencias de build debuggable sin "Permission denied"
adb exec-out run-as com.example.biodontos tar cf - . | tar xf - -C ./pulled-app-data
```

---

## 6. Compose Stability Baseline y Puertas de Calidad

El archivo [`stability_config.conf`](../stability_config.conf) declara como estables los tipos externos usados por Biodontos (`java.time.*`, `kotlinx.collections.immutable.*`, `kotlinx.datetime.*`, `arrow.core.*`, `io.github.jan.supabase.gotrue.user.*`, `android.net.Uri`).

Los scripts [`tools/ci/check-compose-stability.sh`](../tools/ci/check-compose-stability.sh) y [`tools/ci/check-compose-stability.ps1`](../tools/ci/check-compose-stability.ps1) leen `app/build/compose_compiler/app_debug-composables.txt` y `app_debug-module.json`, excluyendo `@Preview` (`*Preview` y `presentation.previews`).

### Flujo de trabajo obligatorio

```bash
# 1. Compilar siempre primero (el análisis lee la salida del compilador Compose)
./gradlew :app:compileDebugKotlin

# 2. Verificar contra el baseline comprometido (app/stability/app.stability)
bash tools/ci/check-compose-stability.sh
# En Windows:
.\tools\ci\check-compose-stability.ps1
```

Símbolos del reporte:
- `+`: composable añadido.
- `-`: composable eliminado.
- `~`: **regresión** (perdió `skippable` o un parámetro degradó de `stable` a `unstable`) — retorna código de salida `1`.

### Actualización deliberada del baseline

Solo actualiza el baseline cuando el cambio sea intencional y esté justificado en el mensaje de commit (nunca para silenciar una regresión accidental):

```bash
./gradlew :app:compileDebugKotlin
bash tools/ci/check-compose-stability.sh --update-baseline
# O en Windows:
.\tools\ci\check-compose-stability.ps1 -UpdateBaseline

git diff app/stability/app.stability
git add app/stability/app.stability
git commit -m "Update stability baseline: <motivo técnico>"
```

### Puertas adicionales de CI ([`.github/workflows/android-ci.yml`](../.github/workflows/android-ci.yml))
- `./gradlew :app:detekt :app:lintDebug` (incluye `UnsafeCallOnNullableType` y `UnusedPrivateMember`).
- `./gradlew :app:koverVerify :app:koverXmlReport` (cobertura mínima `>= 80%`).
- `./gradlew :app:pixel2api30DebugAndroidTest` (en `push` y `workflow_dispatch`).
