# 🦷 Biodontos — Plataforma Clínica Odontológica (Android)

Aplicación móvil clínica odontológica desarrollada en **Kotlin**, **Jetpack Compose (Material 3)** y **Supabase (PostgreSQL + PostgREST + GoTrue Auth)** bajo **Clean Architecture + MVI/MVVM**.

Incluye dos portales integrados:
* **Portal de Doctores:** Gestión de pacientes, admisión clínica con anamnesis y firma digital de consentimiento informado, odontograma interactivo FDI, periodontograma de 6 sitios, historia clínica y evoluciones inmutables, catálogo de materiales clínicos (INVIMA), facturación/abonos y agenda médica.
* **Portal de Pacientes:** Búsqueda de especialistas, perfil y servicios de consultorio, agendamiento y reprogramación de citas con confirmación en 2 pasos, historial clínico y estado de cuenta.

---

## 📲 Descarga e Instalación del APK

Puedes descargar e instalar la última versión del APK directamente desde el portal oficial de despliegue:

👉 **[Descargar APK de Biodontos — https://biodontosdeploy.vercel.app/](https://biodontosdeploy.vercel.app/)**

### Recomendaciones para instalar el APK en dispositivos Android

Al instalar el archivo `.apk` directamente en tu celular o tablet, ten en cuenta estas **3 recomendaciones prácticas**:

1. **Requisitos del celular:**
   * Funciona en cualquier teléfono o tablet con **Android 8.0 o superior** (`minSdk = 26`, compatible con más del 99% de los dispositivos actuales) y conexión a internet (Wi-Fi o datos móviles).
2. **Permiso de "Fuentes desconocidas":**
   * Como la aplicación se descarga de forma directa desde la web y no desde Google Play Store, la primera vez que abras el archivo `.apk` el sistema te pedirá autorizar **"Confiar en esta fuente" / "Instalar apps desconocidas"** para tu navegador o gestor de archivos.
3. **Aviso de Google Play Protect:**
   * Al tratarse de un APK de distribución directa (`debug`), puede aparecer una ventana de Google Play Protect indicando que *"no reconoce al desarrollador"*. Simplemente toca en **"Más detalles" → "Instalar de todas formas"**.

> 💡 **Nota sobre actualizaciones:** Si tenías instalada una versión previa del APK compilada en otro equipo (con una firma de depuración distinta) y Android muestra el mensaje *"No se instaló la app (conflicto de paquete)"*, **desinstala primero la versión anterior** del teléfono antes de instalar el nuevo APK.

---

## 🔐 Credenciales de Prueba (Portal de Doctores)

* **Correo:** `cmendoza@biodontos.com`
* **Contraseña:** `Clinica2026*`
* **Rol:** `DOCTOR` (*Dr. Carlos Mendoza — Odontología General & Rehabilitación Oral*)

---

## 🏗️ Arquitectura y Compilación Local

El proyecto consta de un único módulo Gradle (`:app`) organizado en cuatro capas (`core`, `domain`, `data`, `presentation`).

### Requisitos de compilación
* **JDK 17** (por ejemplo, el JBR incluido con Android Studio).
* **Android SDK 35** (`compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`).

```powershell
# Configurar JAVA_HOME en PowerShell (Windows)
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'

# Ejecutar suite de pruebas unitarias JVM
./gradlew testDebugUnitTest

# Generar APK de depuración
./gradlew assembleDebug
```

El archivo APK compilado localmente se genera en:
`app/build/outputs/apk/debug/app-debug.apk`
