package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.GuardianDto
import com.example.biodontos.domain.model.Guardian
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianMapperTest {

    @Test
    fun guardianDtoToDomainMapsCorrectly() {
        val dto = GuardianDto(
            id = "g-101",
            firstName = "Martha",
            lastName = "Gomez",
            phone = "3109876543",
            address = "Calle 100 # 15-20",
            locality = "Usaquén"
        )

        val domain = dto.toDomain()

        assertEquals("g-101", domain.id)
        assertEquals("Martha", domain.firstName)
        assertEquals("Gomez", domain.lastName)
        assertEquals("3109876543", domain.phone)
        assertEquals("Calle 100 # 15-20", domain.address)
        assertEquals("Usaquén", domain.locality)
        assertEquals("Martha Gomez", domain.fullName)
    }

    @Test
    fun guardianToDtoMapsCorrectlyWithDefaults() {
        val domain = Guardian(
            id = "",
            firstName = " Carlos ",
            lastName = " Rodriguez ",
            phone = " 3001234567 ",
            address = " Carrera 7 # 45-10 ",
            locality = " Chapinero "
        )

        val dto = domain.toDto()

        assertNull(dto.id)
        assertEquals("Carlos", dto.firstName)
        assertEquals("Rodriguez", dto.lastName)
        assertEquals("3001234567", dto.phone)
        assertEquals("Carrera 7 # 45-10", dto.address)
        assertEquals("Chapinero", dto.locality)
    }
}
