package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordPaymentUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private lateinit var useCase: RecordPaymentUseCase

    @Before
    fun setUp() {
        useCase = RecordPaymentUseCase(billingRepository)
    }

    @Test
    fun returnsFailureWhenTreatmentIdIsBlank() = runTest {
        val result = useCase(
            treatmentId = "   ",
            amountPesos = 50_000L
        )

        assertTrue(result.isFailure)
        assertEquals("El identificador del tratamiento no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun returnsFailureWhenAmountIsZeroOrNegative() = runTest {
        val resultZero = useCase(
            treatmentId = "tr-123",
            amountPesos = 0L
        )
        assertTrue(resultZero.isFailure)
        assertEquals("El monto del abono debe ser mayor a cero pesos", resultZero.exceptionOrNull()?.message)

        val resultNegative = useCase(
            treatmentId = "tr-123",
            amountPesos = -100L
        )
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun delegatesToRepositoryWhenInputsAreValid() = runTest {
        val expectedPayment = Payment(
            id = "pay-1",
            treatmentId = "tr-100",
            amount = CopAmount(150_000L),
            paymentMethod = "EFECTIVO"
        )
        coEvery { billingRepository.recordPayment(any()) } returns Result.success(expectedPayment)

        val result = useCase(
            treatmentId = "tr-100",
            amountPesos = 150_000L,
            paymentMethod = "EFECTIVO"
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedPayment, result.getOrNull())
        coVerify(exactly = 1) {
            billingRepository.recordPayment(match {
                it.treatmentId == "tr-100" && it.amount.pesos == 150_000L && it.paymentMethod == "EFECTIVO"
            })
        }
    }
}
