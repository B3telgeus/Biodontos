package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.repository.DoctorRepository

class SearchDoctorsUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(
        query: String = "",
        specialty: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Doctor>> {
        return doctorRepository.searchDoctors(
            query = query.trim(),
            specialty = specialty?.takeIf { it.isNotBlank() && it != "Todas" && it != "Todos" },
            limit = limit,
            offset = offset
        )
    }
}
