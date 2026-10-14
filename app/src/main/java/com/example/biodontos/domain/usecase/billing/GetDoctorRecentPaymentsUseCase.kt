package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.DoctorPaymentRecord
import com.example.biodontos.domain.repository.BillingRepository

class GetDoctorRecentPaymentsUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<DoctorPaymentRecord>> {
        return billingRepository.getRecentDoctorPayments(limit)
    }
}
