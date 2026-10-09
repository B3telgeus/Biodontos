package com.example.biodontos.domain

import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodontalCalculationTest {

    @Test
    fun `CAL is correctly computed as Pocket Depth plus Gingival Recession`() {
        val measurement = PeriodontalSiteMeasurement(
            toothNumber = 16,
            siteLocation = PeriodontalSiteLocation.MESIO_VESTIBULAR,
            pocketDepthMm = 3.0f,
            gingivalRecessionMm = 2.0f
        )

        assertEquals(5.0f, measurement.clinicalAttachmentLossMm, 0.01f)
    }

    @Test
    fun `Pocket depth greater or equal to 4mm triggers deep pocket alert`() {
        val normalSite = PeriodontalSiteMeasurement(
            toothNumber = 11,
            siteLocation = PeriodontalSiteLocation.MID_VESTIBULAR,
            pocketDepthMm = 3.0f
        )
        assertFalse(normalSite.isDeepPocket)

        val pathologicalSite = PeriodontalSiteMeasurement(
            toothNumber = 11,
            siteLocation = PeriodontalSiteLocation.DISTO_VESTIBULAR,
            pocketDepthMm = 4.0f
        )
        assertTrue(pathologicalSite.isDeepPocket)

        val severeSite = PeriodontalSiteMeasurement(
            toothNumber = 11,
            siteLocation = PeriodontalSiteLocation.MESIO_LINGUAL,
            pocketDepthMm = 6.5f
        )
        assertTrue(severeSite.isDeepPocket)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Negative pocket depth throws exception`() {
        PeriodontalSiteMeasurement(
            toothNumber = 21,
            siteLocation = PeriodontalSiteLocation.MID_VESTIBULAR,
            pocketDepthMm = -1.0f
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Mobility grade above 3 throws exception`() {
        PeriodontalSiteMeasurement(
            toothNumber = 21,
            siteLocation = PeriodontalSiteLocation.MID_VESTIBULAR,
            pocketDepthMm = 2.0f,
            mobilityGrade = 4
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Furcation grade above 4 throws exception`() {
        PeriodontalSiteMeasurement(
            toothNumber = 21,
            siteLocation = PeriodontalSiteLocation.MID_VESTIBULAR,
            pocketDepthMm = 2.0f,
            furcationGrade = 5
        )
    }
}
