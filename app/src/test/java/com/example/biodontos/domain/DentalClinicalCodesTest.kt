package com.example.biodontos.domain

import com.example.biodontos.domain.model.BogotaLocalities
import com.example.biodontos.domain.model.DentalClinicalCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DentalClinicalCodesTest {

    @Test
    fun `Bogota localities contains all 20 official localities plus Otra`() {
        assertEquals(20, BogotaLocalities.OFFICIAL_TWENTY.size)
        assertEquals(21, BogotaLocalities.ALL_OPTIONS.size)
        assertEquals(BogotaLocalities.OTHER_OPTION, BogotaLocalities.ALL_OPTIONS.last())
        assertTrue(BogotaLocalities.isOfficialLocality("Suba"))
        assertTrue(BogotaLocalities.isOfficialLocality("usaquén"))
        assertFalse(BogotaLocalities.isOfficialLocality("Soacha"))
    }

    @Test
    fun `CIE10 search matches by code and accent-insensitive description`() {
        val byCode = DentalClinicalCodes.searchCie10("K02.1")
        assertTrue(byCode.any { it.code == "K02.1" })

        val byWord = DentalClinicalCodes.searchCie10("caries")
        assertTrue(byWord.size >= 4)

        assertNotNull(DentalClinicalCodes.findCie10ByCode("K05.0"))
    }

    @Test
    fun `CUPS search matches by code description and category`() {
        val byCode = DentalClinicalCodes.searchCups("232102")
        assertEquals(1, byCode.size)
        assertTrue(byCode.first().description.contains("resina", ignoreCase = true))

        val byCategory = DentalClinicalCodes.searchCups("endodoncia")
        assertTrue(byCategory.size >= 4)

        assertNotNull(DentalClinicalCodes.findCupsByCode("890203"))
    }
}
