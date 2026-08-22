package com.example.biodontos.domain.usecase.clinical

class CheckConsentLockUseCase {
    operator fun invoke(isConsentSigned: Boolean): Result<Unit> {
        return if (isConsentSigned) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Acción Bloqueada: Falta Consentimiento Informado."))
        }
    }
}
