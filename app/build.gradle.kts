import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

val envProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val key = trimmed.substringBefore("=").trim()
                val value = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                setProperty(key, value)
            }
        }
    }
}

val supabaseUrl = envProperties.getProperty("SUPABASE_URL", "https://xiuhvuudloifnuaiqpkh.supabase.co")
val supabaseKey = envProperties.getProperty(
    "SUPABASE_KEY",
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhpdWh2dXVkbG9pZm51YWlxcGtoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgxNDUwMjMsImV4cCI6MjA5MzcyMTAyM30.ztml4G__ODxWWSwABiB4v44MUguOziJx_fjqFZlWAWk"
)

android {
    namespace = "com.example.biodontos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.biodontos"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += "NullSafeMutableLiveData"
        abortOnError = false
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy("androidx.compose.runtime.Composable")
                classes(
                    "*.BuildConfig",
                    "*ComposableSingletons*",
                    "com.example.biodontos.presentation.**.*Screen*",
                    "com.example.biodontos.presentation.**.*Dialog*",
                    "com.example.biodontos.presentation.**.shell.*",
                    "com.example.biodontos.presentation.navigation.*",
                    "com.example.biodontos.presentation.components.*",
                    "com.example.biodontos.presentation.odontogram.InteractiveTooth*",
                    "com.example.biodontos.presentation.periodontogram.InteractivePeriodontogram*",
                    "com.example.biodontos.presentation.**.*Factory*",
                    "com.example.biodontos.core.designsystem.**",
                    "com.example.biodontos.core.di.**",
                    "com.example.biodontos.core.network.**",
                    "com.example.biodontos.data.repository.**",
                    "com.example.biodontos.ui.theme.**",
                    "com.example.biodontos.MainActivity*",
                    "com.example.biodontos.BiodontosApp*"
                )
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Architecture & Background
    implementation(libs.arrow.core)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Supabase & Network
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.core.kt)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}