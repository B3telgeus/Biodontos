ACTION_PLAN.md: Dental Clinic Tablet App (Kotlin & Jetpack Compose)
1. Visión y Objetivos Generales del Sistema
   Target Hardware: Tablets Android de 10'' a 13'' (Landscape prioritario).

Restricción de Sistema: Compatibilidad estricta y exclusiva a partir de Android 16 (API Level 36). No se admitirá código heredado (Legacy) para priorizar seguridad nativa médica.

Stack Principal: Kotlin 1.9+, Jetpack Compose, Material 3, Coroutines + Flow, Supabase-kt (PostgreSQL/Realtime), Ktor Client, Room, WorkManager, FCM (Firebase Cloud Messaging).

Arquitectura: Clean Architecture (Domain-Driven) + MVI/MVVM reactivo con StateFlow.

Objetivos Core de Rendimiento y UX
Agendamiento Asíncrono Ágil: Interfaz interactiva para buscar disponibilidad, reservar, cancelar o reprogramar citas en un tiempo total < 90 segundos.

Pre-Registro Digital Eficiente: Módulo de pre-ingreso asistido (Identificación y Anamnesis) con validación automática, reduciendo el procesamiento administrativo a < 3 minutos por paciente.

Notificaciones y Confirmación 1-Clic: Motor de recordatorios push (48h y 24h antes) con acción directa de confirmación desde la notificación, apuntando a una tasa de asistencia > 85%.

Almacenamiento y Carga Rápida (Offline-First): Sincronización híbrida (Room + Supabase) para que la historia clínica y las indicaciones post-consulta carguen en < 2 segundos.

Interactividad Clínica (Umbral de Doherty): El Odontograma debe responder a interacciones táctiles en < 100ms mediante UI Optimista, mientras sincroniza con la base de datos en < 400ms.

2. Arquitectura de Paquetes (Clean Architecture)
   El proyecto debe estructurarse bajo el siguiente árbol dentro de app/src/main/java/com/dental/clinic/:

Plaintext
com.dental.clinic/
│
├── core/
│   ├── network/                # Supabase Client Singleton, FCM Service
│   ├── designsystem/           # Tokens (Slate, Zafiro, Carmesí, Esmeralda), DentalTheme
│   ├── util/                   # Validadores clínicos, DateFormatters, Permisos Android 16
│   └── constants/              # FDI Constants, Enums (ToothSurface, ToothCondition)
│
├── data/
│   ├── dto/                    # Data Transfer Objects (Mapeo a Supabase)
│   ├── repository/             # Implementaciones (Remote via Supabase, Local via Room)
│   └── local/                  # Room Database, DataStore (Sesión y Caché)
│
├── domain/
│   ├── model/                  # Domain Models Inmutables (Patient, Tooth, Appointment)
│   ├── repository/             # Interfaces de los repositorios
│   └── usecase/
│       ├── appointments/       # BookAppointmentUseCase, CancelAppointmentUseCase
│       ├── preregistration/    # ValidateAnamnesisUseCase
│       ├── notifications/      # ConfirmAttendance1ClickUseCase
│       ├── clinical/           # SaveEvolutionUseCase, CheckConsentLockUseCase
│       └── billing/            # CalculateAccountBalanceUseCase
│
└── presentation/
├── common/                 # UI Components (Toasts, Modals, Banners)
├── navigation/             # NavHost (Adaptive TwoPaneLayout para Tablets)
├── appointments/           # Calendario y TimeSlot Picker (<90s)
├── preregistration/        # Stepper Form (Ley de Miller)
├── odontogram/             # Canvas Interactivo FDI (11..48)
├── clinical_history/       # Acordeones de anamnesis e historial
└── billing/                # Progreso financiero (Zeigarnik Effect)
3. Configuración Gradle (Exclusivo Android 16)
   app/build.gradle.kts:

Kotlin
plugins {
alias(libs.plugins.android.application)
alias(libs.plugins.kotlin.android)
alias(libs.plugins.kotlin.serialization)
}

android {
namespace = "com.dental.clinic"
compileSdk = 36 // Android 16

    defaultConfig {
        applicationId = "com.dental.clinic"
        minSdk = 36 // Restricción estricta API 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
// Jetpack Compose & UI
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)

    // Arquitectura & Background (Android 16)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Supabase SDK & Network
    implementation(platform("io.github.jan-tennert.supabase:bom:2.1.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
4. DTOs Esenciales (data/dto/)
   Kotlin
   @Serializable
   data class PatientDto(
   val id: String? = null,
   @SerialName("dni_cedula") val dniCedula: String,
   @SerialName("first_name") val firstName: String,
   @SerialName("last_name") val lastName: String,
   @SerialName("consentimiento_firmado") val consentSigned: Boolean = false,
   @SerialName("tiene_alergias_criticas") val hasCriticalAllergies: Boolean = false
   )

@Serializable
data class AppointmentDto(
val id: String? = null,
@SerialName("patient_id") val patientId: String,
@SerialName("start_time") val startTime: String,
val status: String,
@SerialName("confirmation_status") val isConfirmed: Boolean = false
)
5. Implementación del Odontograma Interactivo (Compose Canvas)
   Kotlin
   import androidx.compose.foundation.Canvas
   import androidx.compose.foundation.gestures.detectTapGestures
   import androidx.compose.ui.geometry.Offset
   import androidx.compose.ui.geometry.Size
   import androidx.compose.ui.graphics.Color
   import androidx.compose.ui.graphics.Path
   import androidx.compose.ui.graphics.drawscope.Stroke
   import androidx.compose.ui.input.pointer.pointerInput

enum class ToothSurface { VESTIBULAR, OCLUSAL, PALATINO, MESIAL, DISTAL }
enum class ToothCondition(val color: Color) {
SANO(Color(0xFFCBD5E1)), CARIES(Color(0xFFDC2626)), TRATADO(Color(0xFF10B981))
}

@Composable
fun InteractiveTooth(
toothNumber: Int,
surfaceStates: Map<ToothSurface, ToothCondition>,
onSurfaceClicked: (ToothSurface) -> Unit,
modifier: Modifier = Modifier.size(70.dp)
) {
Canvas(
modifier = modifier.pointerInput(Unit) {
detectTapGestures { offset ->
val surface = detectSurface(offset, size.width, size.height)
surface?.let { onSurfaceClicked(it) }
}
}
) {
val inset = size.width * 0.28f
// Lógica de dibujado (Oclusal, Vestibular, Palatino, Mesial, Distal)
// Ejemplo Oclusal:
drawRect(
color = surfaceStates[ToothSurface.OCLUSAL]?.color ?: ToothCondition.SANO.color,
topLeft = Offset(inset, inset),
size = Size(size.width - 2 * inset, size.height - 2 * inset)
)
// (El agente debe completar los Path geométricos para las demás caras)
}
}
6. Casos de Uso Críticos (Domain)
   Kotlin
   class CheckConsentLockUseCase {
   operator fun invoke(isConsentSigned: Boolean): Result<Unit> {
   return if (isConsentSigned) Result.success(Unit)
   else Result.failure(Exception("Acción Bloqueada: Falta Consentimiento Informado."))
   }
   }

class ConfirmAttendance1ClickUseCase(private val repository: AppointmentRepository) {
suspend operator fun invoke(appointmentId: String): Result<Unit> {
return runCatching { repository.updateConfirmationStatus(appointmentId, true) }
}
}
7. Instrucciones de Implementación para el Agente IA (Android Studio)
   Gestión de Permisos Android 16: Implementar lógica estricta para solicitar android.permission.POST_NOTIFICATIONS y android.permission.SCHEDULE_EXACT_ALARM. Sin esto, el motor de recordatorios de 48h/24h fallará.

UI Adaptativa (Tablet): Usa TwoPaneLayout o Row(Modifier.fillMaxSize()). Coloca la ficha del paciente (Alergias en rojo, progreso financiero) a la izquierda (35% ancho) y el interactivo (Odontograma/Calendario) a la derecha (65% ancho).

Flujo Optimista (Optimistic UI): Al tocar un diente en InteractiveTooth, el ViewModel debe actualizar el StateFlow (cambio de color inmediato) ANTES de procesar el upsert en Ktor/Supabase.

Background Sync: Configura Room Database como la fuente única de la verdad (Single Source of Truth). Usa WorkManager para sincronizar citas y odontogramas en segundo plano, logrando el objetivo de carga en < 2s al abrir la app.