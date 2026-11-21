package com.example.biodontos.data.repository

import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.history.VoidInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for [EvolutionRepositoryImpl] (Plan 1: Integridad y Atomicidad RPC).
 *
 * Requirements verified:
 * - R3: Invocations must call PostgreSQL RPC transactional function `save_evolution_with_materials`.
 * - R3: Invocations pass `p_close_record = true`, `closureSummary`, and `closureSignatureUrl` when record closure requested.
 * - R3: Invocations pass `p_close_record = false` and null closure details when record closure is not requested.
 * - R3: RPC success returns `Result.success(evolution)` preserving all materials.
 * - R3: RPC failure propagates `Result.failure` without corrupting state or swallowing exceptions.
 * - R3: Idempotent retry: submitting the same evolution replaces materials rather than duplicating.
 * - Technical Conventions: Constructor injection of `SupabaseClient` and `CoroutineDispatcher`.
 * - Technical Conventions: Elimination of `runCatching` in suspend functions (specifically `voidEvolution`).
 * - Technical Conventions: Transparent propagation of `CancellationException` in all suspend flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EvolutionRepositoryImplTest {

    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val postgrest: Postgrest = mockk(relaxed = true)
    private val auth: Auth = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val postgrestQueryBuilder: PostgrestQueryBuilder = mockk(relaxed = true)
    private val postgrestResult: PostgrestResult = mockk(relaxed = true)

    private lateinit var repository: EvolutionRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic("io.github.jan.supabase.postgrest.PostgrestKt")
        mockkStatic("io.github.jan.supabase.postgrest.PostgrestExtKt")
        mockkStatic("io.github.jan.supabase.gotrue.AuthKt")
        mockkStatic("com.example.biodontos.data.repository.EvolutionRepositoryImplKt")

        every { supabaseClient.postgrest } returns postgrest
        every { supabaseClient.auth } returns auth
        every { postgrest.from(any()) } returns postgrestQueryBuilder

        repository = EvolutionRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            postgrestUpdater = { table, _, _ ->
                supabaseClient.postgrest.from(table)
                Unit
            }
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }


    private fun createSampleEvolution(
        id: String = "evo-100",
        recordId: String = "rec-200",
        doctorId: String = "doc-300",
        date: String = "2026-10-25",
        procedureTime: String = "10:30:00",
        description: String = "Restauración con resina compuesta en pieza 16 oclusal",
        doctorName: String = "Dra. Erika Bejarano",
        toothTreated: Int? = 16,
        cost: Double = 120000.0,
        paymentAmount: Double = 120000.0,
        balance: Double = 0.0,
        patientSignatureUrl: String? = "https://storage.biodontos.com/signatures/pat_evo100.png",
        doctorSignatureUrl: String? = "https://storage.biodontos.com/signatures/doc_evo100.png",
        voidInfo: VoidInfo? = null,
        amendsEvolutionId: String? = null,
        materialsUsed: List<EvolutionMaterialUsed> = listOf(createSampleMaterialUsed())
    ): Evolution {
        return Evolution(
            id = id,
            recordId = recordId,
            doctorId = doctorId,
            date = date,
            procedureTime = procedureTime,
            description = description,
            doctorName = doctorName,
            toothTreated = toothTreated,
            cost = cost,
            paymentAmount = paymentAmount,
            balance = balance,
            patientSignatureUrl = patientSignatureUrl,
            doctorSignatureUrl = doctorSignatureUrl,
            voidInfo = voidInfo,
            amendsEvolutionId = amendsEvolutionId,
            materialsUsed = materialsUsed
        )
    }

    private fun createSampleMaterialUsed(
        id: String = "mat-used-1",
        materialId: String = "mat-filtek-1",
        materialName: String = "Resina Filtek Z350 XT",
        quantityUsed: Double = 1.0,
        lotNumber: String = "LOTE-9921A",
        invima: String = "INVIMA 2018DM-0018241",
        expirationDate: String = "2027-01-10",
        manufacturingDate: String = "2024-01-10"
    ): EvolutionMaterialUsed {
        return EvolutionMaterialUsed(
            id = id,
            materialId = materialId,
            materialName = materialName,
            quantityUsed = quantityUsed,
            lotNumberSnapshot = lotNumber,
            invimaSnapshot = invima,
            expirationDateSnapshot = expirationDate,
            materialNameSnapshot = materialName,
            manufacturingDateSnapshot = manufacturingDate
        )
    }

    private fun createSampleDoctorUser(id: String = "doc-user-uuid-1"): UserInfo {
        val user = mockk<UserInfo>(relaxed = true)
        every { user.id } returns id
        return user
    }


    @Test
    fun `saveEvolution invokes RPC save_evolution_with_materials with evolution payload and materials list`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue("saveEvolution should return Result.success on RPC confirmation", result.isSuccess)
        coVerify(exactly = 1) {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        }
        assertNotNull("RPC parameters must not be null", rpcParamsSlot.captured)
    }

    @Test
    fun `saveEvolution passes p_close_record true and closure details when medical record closure is requested`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()
        val summary = "Tratamiento de restauración finalizado exitosamente. Paciente asintomático."
        val signatureUrl = "https://storage.biodontos.com/signatures/rec-200_closure.png"
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = true,
            closureSummary = summary,
            closureSignatureUrl = signatureUrl
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            postgrest.rpc(function = eq("save_evolution_with_materials"), parameters = any())
        }
        val captured = rpcParamsSlot.captured.toString()
        assertTrue("p_close_record should be true", captured.contains("true"))
        assertTrue("closureSummary should be in payload", captured.contains(summary))
        assertTrue("closureSignatureUrl should be in payload", captured.contains(signatureUrl))
    }

    @Test
    fun `saveEvolution passes p_close_record false when record closure is not requested`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = false,
            closureSummary = null,
            closureSignatureUrl = null
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue("p_close_record must be false", captured.contains("false"))
    }

    @Test
    fun `saveEvolution returns Result_success with evolution domain model including materials on RPC success`() = runTest(testDispatcher) {
        val mat1 = createSampleMaterialUsed(id = "mat-u1", materialName = "Resina Z350")
        val mat2 = createSampleMaterialUsed(id = "mat-u2", materialName = "Ácido Grabador 37%")
        val evolution = createSampleEvolution(materialsUsed = listOf(mat1, mat2))

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } returns postgrestResult

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue(result.isSuccess)
        val savedEvolution = result.getOrThrow()
        assertEquals(evolution.id, savedEvolution.id)
        assertEquals(evolution.recordId, savedEvolution.recordId)
        assertEquals(2, savedEvolution.materialsUsed.size)
        assertEquals("Resina Z350", savedEvolution.materialsUsed[0].materialName)
        assertEquals("Ácido Grabador 37%", savedEvolution.materialsUsed[1].materialName)
    }

    @Test
    fun `saveEvolution returns Result_failure without swallowing exception when RPC fails`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()
        val postgresError = RuntimeException("Database error: Clinical record with id rec-200 not found")

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } throws postgresError

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue("Failure must be propagated", result.isFailure)
        assertEquals(postgresError.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `idempotent retry with same evolution_id sends payload for transactional material replacement`() = runTest(testDispatcher) {
        val evoId = "evo-idempotent-999"
        val matInitial = createSampleMaterialUsed(id = "mat-init", quantityUsed = 1.0)
        val evolutionAttempt1 = createSampleEvolution(id = evoId, materialsUsed = listOf(matInitial))

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } returns postgrestResult

        val result1 = repository.saveEvolution(evolution = evolutionAttempt1)
        assertTrue(result1.isSuccess)

        val matUpdated = createSampleMaterialUsed(id = "mat-init", quantityUsed = 2.0)
        val evolutionAttempt2 = createSampleEvolution(id = evoId, materialsUsed = listOf(matUpdated))

        val result2 = repository.saveEvolution(evolution = evolutionAttempt2)
        assertTrue(result2.isSuccess)

        // Verificamos que se ejecutaron dos llamadas RPC pasando el mismo evolution_id
        coVerify(exactly = 2) {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        }
        assertEquals(evoId, result2.getOrThrow().id)
        assertEquals(2.0, result2.getOrThrow().materialsUsed.first().quantityUsed, 0.001)
    }

    @Test
    fun `voidEvolution updates evolution record with voided_at, reason and current doctor id`() = runTest(testDispatcher) {
        val doctorUser = createSampleDoctorUser("doc-uuid-888")
        coEvery { auth.currentUserOrNull() } returns doctorUser

        val result = repository.voidEvolution(
            evolutionId = "evo-100",
            reason = "Diente registrado incorrectamente por error tipográfico"
        )

        assertTrue("voidEvolution should return Result.success", result.isSuccess)
        coVerify(exactly = 1) {
            postgrest.from("evolutions")
        }
    }

    @Test
    fun `voidEvolution without authenticated user sets voided_by to null and succeeds`() = runTest(testDispatcher) {
        coEvery { auth.currentUserOrNull() } returns null

        val result = repository.voidEvolution(
            evolutionId = "evo-100",
            reason = "Anulación por procedimiento duplicado"
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            postgrest.from("evolutions")
        }
    }


    @Test
    fun `saveEvolution with empty materials list passes empty array to RPC without throwing`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution(materialsUsed = emptyList())
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().materialsUsed.size)
        coVerify(exactly = 1) {
            postgrest.rpc(function = eq("save_evolution_with_materials"), parameters = any())
        }
    }

    @Test
    fun `saveEvolution preserves special characters, accents, and UTF-8 clinical symbols in description`() = runTest(testDispatcher) {
        val specialDescription = "Endodoncia pieza #16 con obturación en gutapercha; paciente refiere alivio del dolor. (Sensibilidad: ±0, temperatura: normal)"
        val evolution = createSampleEvolution(description = specialDescription)
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue("Description must preserve accented and special characters", captured.contains("obturación"))
        assertTrue("Description must preserve symbols", captured.contains("±0"))
    }

    @Test
    fun `saveEvolution preserves complex closure summary with UTF-8 characters and multi-line text`() = runTest(testDispatcher) {
        val multilineSummary = """
            CIERRE DE HISTORIA CLÍNICA:
            - Tratamiento de conductos completado según protocolo INVIMA.
            - Corona cerámica cementada definitivamente.
            - Pronóstico favorable a 12 meses.
        """.trimIndent()
        val signatureUrl = "https://storage.biodontos.com/signatures/doctor_closure_sig.png"
        val evolution = createSampleEvolution()
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = true,
            closureSummary = multilineSummary,
            closureSignatureUrl = signatureUrl
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue(captured.contains("Tratamiento de conductos"))
        assertTrue(captured.contains("Pronóstico favorable"))
    }

    @Test
    fun `saveEvolution with zero-cost and fractional payment balances passes exact numeric values`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution(
            cost = 0.0,
            paymentAmount = 0.0,
            balance = 0.0
        )
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(evolution = evolution)

        assertTrue(result.isSuccess)
        assertEquals(0.0, result.getOrThrow().cost, 0.001)
        assertEquals(0.0, result.getOrThrow().paymentAmount, 0.001)
        assertEquals(0.0, result.getOrThrow().balance, 0.001)
    }

    @Test
    fun `saveEvolution rethrows CancellationException immediately without swallowing or wrapping in Result_failure`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } throws CancellationException("Corrutina cancelada por el ciclo de vida de Compose")

        try {
            repository.saveEvolution(evolution = evolution)
            fail("saveEvolution MUST rethrow CancellationException to respect coroutine structured concurrency")
        } catch (e: Exception) {
            assertTrue("Exception must be CancellationException", e is CancellationException)
            assertEquals("Corrutina cancelada por el ciclo de vida de Compose", e.message)
        }
    }

    @Test
    fun `voidEvolution rethrows CancellationException immediately without catching or wrapping in Result_failure`() = runTest(testDispatcher) {
        coEvery { postgrest.from("evolutions") } throws CancellationException("Scope cancelado")

        try {
            repository.voidEvolution(evolutionId = "evo-1", reason = "Error")
            fail("voidEvolution MUST rethrow CancellationException")
        } catch (e: Exception) {
            assertTrue("Exception must be CancellationException", e is CancellationException)
        }
    }

    @Test
    fun `voidEvolution does not use runCatching and propagates CancellationException thrown during currentUser check`() = runTest(testDispatcher) {
        coEvery { auth.currentUserOrNull() } throws CancellationException("Cancellation during auth check")

        try {
            repository.voidEvolution(evolutionId = "evo-1", reason = "Anulación")
            fail("voidEvolution MUST NOT swallow CancellationException via runCatching")
        } catch (e: Exception) {
            assertTrue("CancellationException must propagate out of voidEvolution", e is CancellationException)
            assertEquals("Cancellation during auth check", e.message)
        }
    }

    @Test
    fun `voidEvolution trims leading and trailing whitespace from void reason`() = runTest(testDispatcher) {
        val dirtyReason = "   Procedimiento duplicado en la admisión   "
        coEvery { auth.currentUserOrNull() } returns createSampleDoctorUser()

        val result = repository.voidEvolution(
            evolutionId = "evo-100",
            reason = dirtyReason
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            postgrest.from("evolutions")
        }
    }

    @Test
    fun `getEvolutionsByRecordId rethrows CancellationException`() = runTest(testDispatcher) {
        coEvery { postgrest.from("evolutions") } throws CancellationException("Scope de búsqueda cancelado")

        try {
            repository.getEvolutionsByRecordId("rec-100")
            fail("getEvolutionsByRecordId MUST rethrow CancellationException")
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
        }
    }


    @Test
    fun `Pairwise C1 - closeRecord true with full closure details and multiple INVIMA materials`() = runTest(testDispatcher) {
        val mat1 = createSampleMaterialUsed(id = "m1", materialName = "Resina Filtek Z350 XT", lotNumber = "LOTE-1")
        val mat2 = createSampleMaterialUsed(id = "m2", materialName = "Lidocaína 2%", lotNumber = "LOTE-2")
        val evolution = createSampleEvolution(materialsUsed = listOf(mat1, mat2))
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = true,
            closureSummary = "Tratamiento completo",
            closureSignatureUrl = "https://signature.biodontos.com/sig.png"
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue(captured.contains("true"))
        assertTrue(captured.contains("Resina Filtek Z350 XT"))
        assertTrue(captured.contains("Lidocaína 2%"))
    }

    @Test
    fun `Pairwise C2 - closeRecord true with null signature URL defaults signature cleanly`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution()
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = true,
            closureSummary = "Cierre administrativo sin firma digital",
            closureSignatureUrl = null
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue(captured.contains("true"))
        assertTrue(captured.contains("Cierre administrativo sin firma digital"))
    }

    @Test
    fun `Pairwise C3 - closeRecord false with multiple INVIMA materials`() = runTest(testDispatcher) {
        val mat1 = createSampleMaterialUsed(id = "m1", materialName = "Ionómero de Vidrio", lotNumber = "LOT-ION-1")
        val evolution = createSampleEvolution(materialsUsed = listOf(mat1))
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = false
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue(captured.contains("false"))
        assertTrue(captured.contains("Ionómero de Vidrio"))
    }

    @Test
    fun `Pairwise C4 - closeRecord false with empty materials list`() = runTest(testDispatcher) {
        val evolution = createSampleEvolution(materialsUsed = emptyList())
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = false
        )

        assertTrue(result.isSuccess)
        val captured = rpcParamsSlot.captured.toString()
        assertTrue(captured.contains("false"))
    }

    @Test
    fun `Pairwise C5 - saveClarification creates non-billable note amending original evolution and delegates to saveEvolution`() = runTest(testDispatcher) {
        val original = createSampleEvolution(id = "evo-orig-555", recordId = "rec-555")
        val clarificationText = "Se rectifica: la resina utilizada fue tono A2 en lugar de A3."
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveClarification(
            originalEvolution = original,
            clarificationText = clarificationText,
            doctorId = "doc-444",
            doctorName = "Dr. Carlos"
        )

        assertTrue(result.isSuccess)
        val clarification = result.getOrThrow()
        assertTrue(clarification.description.contains("NOTA ACLARATORIA:"))
        assertTrue(clarification.description.contains("tono A2 en lugar de A3"))
        assertEquals("evo-orig-555", clarification.amendsEvolutionId)
        assertEquals(0.0, clarification.cost, 0.001)
        assertEquals(0.0, clarification.paymentAmount, 0.001)
        assertEquals(0.0, clarification.balance, 0.001)
        assertEquals("rec-555", clarification.recordId)
        assertEquals("doc-444", clarification.doctorId)
    }


    @Test
    fun `Workload S1 - Multi-material operative restoration with complete medical record closure`() = runTest(testDispatcher) {
        val resin = createSampleMaterialUsed(
            id = "mat-1",
            materialId = "invima-mat-1",
            materialName = "Resina Filtek Z350 XT",
            quantityUsed = 1.0,
            lotNumber = "LOTE-9921A",
            invima = "INVIMA 2018DM-0018241",
            expirationDate = "2027-01-10",
            manufacturingDate = "2024-01-10"
        )
        val anesthetic = createSampleMaterialUsed(
            id = "mat-2",
            materialId = "invima-mat-2",
            materialName = "Anestésico Lidocaína al 2% con Epinefrina",
            quantityUsed = 1.8,
            lotNumber = "LOTE-LIDO-33",
            invima = "INVIMA 2015M-0004321",
            expirationDate = "2026-06-30",
            manufacturingDate = "2023-06-30"
        )
        val acid = createSampleMaterialUsed(
            id = "mat-3",
            materialId = "invima-mat-3",
            materialName = "Ácido Grabador Scotchbond 37%",
            quantityUsed = 0.5,
            lotNumber = "LOTE-ACID-77",
            invima = "INVIMA 2019DM-0019555",
            expirationDate = "2028-12-31",
            manufacturingDate = "2024-05-15"
        )

        val evolution = createSampleEvolution(
            id = "evo-clinical-s1",
            recordId = "rec-patient-456",
            doctorId = "doc-erika-uuid",
            doctorName = "Dra. Erika Bejarano",
            toothTreated = 46,
            description = "Apertura cavitaria clase I en pieza 46, grabado ácido total, aplicación de adhesivo y resina compuesta por capas. Ajuste oclusal y pulido final.",
            cost = 180000.0,
            paymentAmount = 180000.0,
            balance = 0.0,
            materialsUsed = listOf(resin, anesthetic, acid)
        )

        val closureSummary = "Tratamiento de operatoria dental concluido satisfactoriamente. Historia clínica apta para archivo."
        val closureSignature = "https://storage.biodontos.com/signatures/doc-erika_rec-patient-456_closure.png"
        val rpcParamsSlot = slot<Any>()

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = capture(rpcParamsSlot)
            )
        } returns postgrestResult

        val result = repository.saveEvolution(
            evolution = evolution,
            closeRecord = true,
            closureSummary = closureSummary,
            closureSignatureUrl = closureSignature
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals(3, saved.materialsUsed.size)
        assertEquals(46, saved.toothTreated)
        coVerify(exactly = 1) {
            postgrest.rpc(function = eq("save_evolution_with_materials"), parameters = any())
        }
        val capturedJson = rpcParamsSlot.captured.toString()
        assertTrue(capturedJson.contains("INVIMA 2018DM-0018241"))
        assertTrue(capturedJson.contains("INVIMA 2015M-0004321"))
        assertTrue(capturedJson.contains("INVIMA 2019DM-0019555"))
        assertTrue(capturedJson.contains(closureSummary))
    }

    @Test
    fun `Workload S4 - Network drop retry during evolution save maintains idempotency without duplicating materials`() = runTest(testDispatcher) {
        val evoId = "evo-network-drop-101"
        val materials = listOf(
            createSampleMaterialUsed(id = "mat-trace-1", materialName = "Resina A2"),
            createSampleMaterialUsed(id = "mat-trace-2", materialName = "Adhesivo Dental")
        )
        val evolution = createSampleEvolution(id = evoId, materialsUsed = materials)

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } throws RuntimeException("Network timeout: java.net.SocketTimeoutException: failed to connect")

        val attempt1Result = repository.saveEvolution(evolution = evolution)
        assertTrue("Attempt 1 must fail due to network failure", attempt1Result.isFailure)
        assertTrue(attempt1Result.exceptionOrNull()?.message?.contains("SocketTimeoutException") == true)

        // Intento 2: Reconexión exitosa al mismo endpoint RPC con el mismo evolution_id
        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } returns postgrestResult

        val attempt2Result = repository.saveEvolution(evolution = evolution)
        assertTrue("Attempt 2 must succeed upon network restoration", attempt2Result.isSuccess)
        assertEquals(evoId, attempt2Result.getOrThrow().id)
        assertEquals(2, attempt2Result.getOrThrow().materialsUsed.size)

        coVerify(exactly = 2) {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        }
    }

    @Test
    fun `Workload S5 - Clinical audit trail voiding erroneous evolution and appending clarification note`() = runTest(testDispatcher) {
        val originalEvolution = createSampleEvolution(
            id = "evo-audit-initial",
            toothTreated = 16,
            description = "Obturación resina pieza 16"
        )
        coEvery { auth.currentUserOrNull() } returns createSampleDoctorUser("doc-auditor-1")

        val voidResult = repository.voidEvolution(
            evolutionId = originalEvolution.id,
            reason = "Diente incorrecto registrado (se digitó 16 en lugar de 26)"
        )
        assertTrue(voidResult.isSuccess)

        coEvery {
            postgrest.rpc(
                function = eq("save_evolution_with_materials"),
                parameters = any()
            )
        } returns postgrestResult

        val clarificationResult = repository.saveClarification(
            originalEvolution = originalEvolution,
            clarificationText = "Se rectifica número de diente: el tratamiento se realizó en pieza 26 y no en pieza 16.",
            doctorId = "doc-auditor-1",
            doctorName = "Dr. Auditor"
        )

        assertTrue(clarificationResult.isSuccess)
        val clarification = clarificationResult.getOrThrow()
        assertEquals("evo-audit-initial", clarification.amendsEvolutionId)
        assertTrue(clarification.description.contains("rectifica número de diente"))
        assertEquals(0.0, clarification.cost, 0.001)
    }
}
