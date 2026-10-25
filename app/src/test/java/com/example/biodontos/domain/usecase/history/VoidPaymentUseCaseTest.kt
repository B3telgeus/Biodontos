package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.history.VoidInfo
import com.example.biodontos.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidPaymentUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private val useCase = VoidPaymentUseCase(billingRepository)

    private val activePayment = Payment(
        id = "pay-99",
        treatmentId = "tr-1",
        amount = CopAmount(120000L),
        paymentMethod = "EFECTIVO"
    )

    @Test
    fun `rejects reason shorter than 10 characters`() = runTest {
        val result = useCase(activePayment, "Error")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.VoidReasonTooShort)
    }

    @Test
    fun `rejects payment that is already voided`() = runTest {
        val voidedPayment = activePayment.copy(
            voidInfo = VoidInfo(LocalDateTime.now(), "Anulado previamente en caja")
        )
        val result = useCase(voidedPayment, "Intento duplicado de anulación")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.AlreadyVoided)
    }

    @Test
    fun `succeeds when reason is valid and delegates to BillingRepository`() = runTest {
        coEvery {
            billingRepository.voidPayment("pay-99", "Reverso de recibo por monto erróneo")
        } returns Result.success(Unit)

        val result = useCase(activePayment, "Reverso de recibo por monto erróneo")
        assertTrue(result.isRight())
        coVerify(exactly = 1) {
            billingRepository.voidPayment("pay-99", "Reverso de recibo por monto erróneo")
        }
    }
}
