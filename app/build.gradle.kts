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

val supabaseUrl = envProperties.getProperty("SUPABASE_URL")?.takeIf { it.isNotBlank() }
    ?: System.getenv("SUPABASE_URL")?.takeIf { it.isNotBlank() }
    ?: throw GradleException(
        "Falta SUPABASE_URL. Configura SUPABASE_URL en .env o local.properties (consulta .env.example)."
    )
val supabaseKey = envProperties.getProperty("SUPABASE_KEY")?.takeIf { it.isNotBlank() }
    ?: System.getenv("SUPABASE_KEY")?.takeIf { it.isNotBlank() }
    ?: throw GradleException(
        "Falta SUPABASE_KEY. Configura SUPABASE_KEY en .env o local.properties (consulta .env.example)."
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "2048m"
            it.forkEvery = 2L
        }
        animationsDisabled = true
        managedDevices {
            localDevices {
                create("pixel6api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "google-atd"
                }
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
            groups {
                create("ciMatrix") {
                    targetDevices.add(localDevices["pixel6api34"])
                    targetDevices.add(localDevices["pixel2api30"])
                }
            }
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
                    "com.example.biodontos.presentation.previews.**",
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

    implementation(libs.arrow.core)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.core.kt)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.google.truth)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.accessibility)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.google.truth)
    androidTestUtil(libs.androidx.test.orchestrator)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}