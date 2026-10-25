# Reglas ProGuard / R8 para Biodontos (Release Production)

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preservar serializadores generados en DTOs y rutas Type-Safe Navigation
-keep,includedescriptorclasses class com.example.biodontos.**$$serializer { *; }
-keepclassmembers class com.example.biodontos.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.biodontos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preservar DTOs de Supabase / Open Dental y Rutas de Navegación
-keep class com.example.biodontos.data.dto.** { *; }
-keep class com.example.biodontos.presentation.navigation.** { *; }

# Ktor & OkHttp & Supabase & SLF4J
-keep class io.ktor.** { *; }
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Arrow-kt
-keep class arrow.** { *; }
-dontwarn arrow.**
