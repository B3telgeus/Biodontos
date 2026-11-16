# SQLCipher Feasibility Study: Biodontos Android (Plan 1 §R5)

## 1. Executive Summary
This document analyzes the technical, operational, and architectural feasibility of implementing full local database encryption at rest using **SQLCipher** (`net.zetetic:sqlcipher-android`) for the **Biodontos** Android clinical application.

Biodontos manages sensitive protected health information (PHI) under Colombian regulatory frameworks (Resolución 1995 de 1999, Ley Estatutaria 1581 de 2012 de Protección de Datos Personales, and Circular 002 de la Superintendencia de Industria y Comercio). While Room SQLite files are stored in internal sandboxed application storage (`/data/data/com.example.biodontos/databases/`), rooted devices, forensic physical access, and improper backup handling pose non-negligible data exposure risks.

---

## 2. Architectural Design with SQLCipher and Room

### 2.1 Dependencies
SQLCipher integrates directly with Room via AndroidX SQLite's `SupportFactory`:

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("net.zetetic:sqlcipher-android:4.5.4@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
}
```

### 2.2 Database Initialization
Room abstracts SQLite access through `SupportSQLiteOpenHelper.Factory`. SQLCipher provides `net.zetetic.database.sqlcipher.SupportFactory`:

```kotlin
val passphrase = KeyManager.getOrCreateDatabasePassphrase(context)
val supportFactory = SupportFactory(passphrase)

val db = Room.databaseBuilder(
    context.applicationContext,
    BiodontosDatabase::class.java,
    "biodontos_db"
)
.openHelperFactory(supportFactory)
.build()
```

---

## 3. Cryptographic Key Derivation & Android Keystore Integration

Hardcoding encryption keys or storing passphrases in plain SharedPreferences completely undermines the security model. A zero-leakage architecture requires hardware-backed key derivation.

### 3.1 Architecture Overview
1. **Hardware-Backed Master Key (Android Keystore)**:
   - Generate an AES-256-GCM master key in `AndroidKeyStore`.
   - The private/secret key never leaves the Trusted Execution Environment (TEE) or StrongBox Keymaster.
2. **Database Passphrase Generation**:
   - On initial launch, generate a cryptographically secure 256-bit random byte array (`SecureRandom`).
   - Encrypt this passphrase using the Keystore AES-GCM key and persist the ciphertext + IV in encrypted preferences or private storage.
3. **Key Unwrapping on App Startup**:
   - Decrypt the encrypted passphrase using Android Keystore inside the TEE.
   - Supply the raw passphrase bytes directly to `SupportFactory(passphraseBytes)`.
   - Clear transient byte arrays in memory using `Arrays.fill(bytes, 0.toByte())` to minimize heap memory exposure.

```
+------------------------+      +-------------------------+
|   Android Keystore     | ---> | AES-256-GCM Master Key  | (TEE / StrongBox)
+------------------------+      +-------------------------+
                                             |
                                          Decrypt
                                             v
+------------------------+      +-------------------------+      +-----------------------+
| Encrypted Passphrase   | ---> | 256-bit Passphrase      | ---> | SupportFactory        |
| (Private App Storage)  |      | (Transient Heap Memory) |      | (SQLCipher Engine)    |
+------------------------+      +-------------------------+      +-----------------------+
```

---

## 4. Performance Overhead and Binary Footprint

### 4.1 I/O and Processing Overhead
- **Page-Level Encryption**: SQLCipher performs AES-256-CBC page-level encryption with HMAC-SHA512 or HMAC-SHA1 verification for every database page read/write.
- **Throughput Impact**:
  - Read queries: **5% – 10%** latency increase due to on-the-fly page decryption.
  - Write / Batch Insert: **10% – 15%** latency increase due to HMAC computation and page encryption.
  - In-memory querying of cached items: negligible impact once loaded into memory.
- For Biodontos' clinical record volume (~tens of patients cached locally, drafts with small JSON payloads), the absolute time delta is well within acceptable UI responsiveness thresholds (< 5 ms per query).

### 4.2 APK Size and Native Libraries (JNI)
- SQLCipher bundles compiled native shared libraries (`.so`) for target ABIs:
  - `arm64-v8a` (~3.8 MB)
  - `armeabi-v7a` (~2.5 MB)
  - `x86_64` (~4.0 MB)
  - `x86` (~3.7 MB)
- **Total APK Impact**:
  - Universal APK: **+14 MB to +16 MB**.
  - ABI-split APK / Android App Bundle (AAB): **+3.5 MB to +4.2 MB** per delivered architecture.

---

## 5. Security vs. Complexity Tradeoffs

| Factor | Standard Room (Current) | SQLCipher + Room |
|---|---|---|
| **Data at Rest Protection** | OS sandbox only (accessible if rooted) | Full AES-256 cryptographic protection |
| **Legal / Compliance Posture** | Meets basic baseline; relies on OS sandboxing | Exceeds health data protection norms (HIPAA / Ley 1581) |
| **Key Management Complexity** | None | High (TEE, Keystore invalidation, biometric gates) |
| **Database Migration Complexity** | Standard Room migration scripts | Key migration, SQLite plaintext-to-cipher migration |
| **Memory Footprint** | Low | Low-to-moderate (+ page buffer overhead) |
| **Binary Footprint** | 0 MB overhead (uses system SQLite) | +4MB (AAB) to +15MB (Universal APK) |

---

## 6. Migration Roadmap

### Phase 1: M2 Baseline (Current Milestone)
- Room standard SQLite implementation with strict schema versioning (`exportSchema = true`).
- Sandbox isolation: `allowBackup = false` and exclusions in `dataExtractionRules.xml` and `backup_rules.xml`.
- Session wiping via `SessionLocalCleaner` on logout.

### Phase 2: Cipher Foundation & Prototyping
- Add `net.zetetic:sqlcipher-android` dependency.
- Implement `KeystorePassphraseProvider` with StrongBox support and fallback to standard TEE.
- Implement `SupportFactory` wiring in `BiodontosDatabase.getInstance(context)`.

### Phase 3: Seamless Database Plaintext-to-Cipher Migration
- For existing app installations, use `net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase` with `attach` command (`ATTACH DATABASE 'encrypted.db' AS encrypted KEY 'passphrase'; SELECT sqlcipher_export('encrypted'); DETACH DATABASE encrypted;`) to migrate unencrypted databases without data loss.

### Phase 4: Production Rollout
- Verify memory wiping protocols on application backgrounding.
- Perform end-to-end stress tests across ARM64 physical test devices.

---

## 7. Conclusion
Integrating SQLCipher is **fully feasible** and architecturally sound for Biodontos Android via Room's `SupportFactory`. Given the current Plan 1 requirements, the local database is structured with clean separation of entities and DAOs, enabling immediate drop-in replacement of the open helper factory when Phase 2 is prioritized.
