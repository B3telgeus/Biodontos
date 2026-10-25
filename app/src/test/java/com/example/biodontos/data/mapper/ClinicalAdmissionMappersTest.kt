package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AnamnesisDto
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.dto.ClinicalRecordDto
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.data.dto.InformedConsentDto
import com.example.biodontos.data.dto.InformedConsentProcedureDto
import com.example.biodontos.data.dto.PeriodontalExaminationDto
import com.example.biodontos.data.dto.PeriodontalSiteDto
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.InformedConsentProcedure
import com.example.biodontos.domain.model.PeriodontalExamination
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClinicalAdmissionMappersTest {

    @Test
    fun `AnamnesisDto toDomain and Anamnesis toDto map fields accurately`() {
        val dto = AnamnesisDto(
            id = "anam-1",
            patientId = "pat-1",
            tratamientoMedicoActual = true,
            alergias = true,
            diabetes = true,
            observaciones = "Control glucémico",
            alergiasDescripcion = "Penicilina"
        )
        val domain = dto.toDomain()
        assertEquals("anam-1", domain.id)
        assertEquals("pat-1", domain.patientId)
        assertEquals(true, domain.tratamientoMedicoActual)
        assertEquals(true, domain.alergias)
        assertEquals(true, domain.diabetes)
        assertEquals("Control glucémico", domain.observaciones)
        assertEquals("Penicilina", domain.alergiasDescripcion)

        val backToDto = domain.toDto()
        assertEquals("anam-1", backToDto.id)
        assertEquals("pat-1", backToDto.patientId)
        assertEquals(true, backToDto.alergias)
        assertEquals("Penicilina", backToDto.alergiasDescripcion)

        val generatedDto = Anamnesis(id = "", patientId = "pat-1").toDto()
        assertNotNull(generatedDto.id)
    }

    @Test
    fun `ClinicalRecordDto toDomain and ClinicalRecord toDto map fields accurately`() {
        val dto = ClinicalRecordDto(
            id = "rec-1",
            patientId = "pat-1",
            dentitionType = "CHILD",
            mainDiagnosis = "Caries dental",
            status = "EN_TRATAMIENTO"
        )
        val domain = dto.toDomain()
        assertEquals("rec-1", domain.id)
        assertEquals("pat-1", domain.patientId)
        assertEquals(DentitionType.CHILD, domain.dentitionType)
        assertEquals("Caries dental", domain.mainDiagnosis)
        assertEquals(ClinicalRecordStatus.EN_TRATAMIENTO, domain.status)

        val backToDto = domain.toDto()
        assertEquals("rec-1", backToDto.id)
        assertEquals("CHILD", backToDto.dentitionType)
        assertEquals("EN_TRATAMIENTO", backToDto.status)

        val emptyIdDto = ClinicalRecord(id = "", patientId = "pat-1").toDto()
        assertNull(emptyIdDto.id)
    }

    @Test
    fun `PeriodontalExaminationDto and PeriodontalSiteDto map toDomain and toDto accurately`() {
        val siteDto = PeriodontalSiteDto(
            id = "site-1",
            examinationId = "exam-1",
            toothNumber = 16,
            siteLocation = "MESIO_VESTIBULAR",
            pocketDepthMm = 4.0f,
            gingivalRecessionMm = 1.0f,
            bleedingOnProbing = true,
            suppuration = false,
            mobilityGrade = 1,
            furcationGrade = 2
        )
        val siteDomain = siteDto.toDomain()
        assertEquals(16, siteDomain.toothNumber)
        assertEquals(PeriodontalSiteLocation.MESIO_VESTIBULAR, siteDomain.siteLocation)
        assertEquals(4.0f, siteDomain.pocketDepthMm, 0.01f)
        assertEquals(1.0f, siteDomain.gingivalRecessionMm, 0.01f)
        assertEquals(5.0f, siteDomain.clinicalAttachmentLossMm, 0.01f)
        assertEquals(true, siteDomain.bleedingOnProbing)
        assertEquals(1, siteDomain.mobilityGrade)
        assertEquals(2, siteDomain.furcationGrade)

        val mappedSiteDto = siteDomain.toDto("exam-1")
        assertNull(mappedSiteDto.id)
        assertEquals("exam-1", mappedSiteDto.examinationId)
        assertEquals("MESIO_VESTIBULAR", mappedSiteDto.siteLocation)
        assertEquals(5.0f, mappedSiteDto.clinicalAttachmentLossMm ?: 0f, 0.01f)

        val examDto = PeriodontalExaminationDto(
            id = "exam-1",
            recordId = "rec-1",
            doctorId = "doc-1",
            examinationDate = "2026-10-20",
            generalObservations = "Gingivitis localizada"
        )
        val examDomain = examDto.toDomain(listOf(siteDomain))
        assertEquals("exam-1", examDomain.id)
        assertEquals("rec-1", examDomain.recordId)
        assertEquals(1, examDomain.sites.size)

        val mappedExamDto = examDomain.toDto()
        assertEquals("exam-1", mappedExamDto.id)
        assertEquals("Gingivitis localizada", mappedExamDto.generalObservations)
    }

    @Test
    fun `EvolutionDto and EvolutionMaterialUsedDto map toDomain and toDto accurately`() {
        val matUsedDto = EvolutionMaterialUsedDto(
            id = "mat-used-1",
            evolutionId = "evo-1",
            materialId = "mat-1",
            quantityUsed = 2.0,
            lotNumberSnapshot = "LOT-99",
            invimaSnapshot = "INV-123",
            expirationDateSnapshot = "2027-12-31"
        )
        val matUsedDomain = matUsedDto.toDomain()
        assertEquals("mat-used-1", matUsedDomain.id)
        assertEquals("mat-1", matUsedDomain.materialId)
        assertEquals(2.0, matUsedDomain.quantityUsed, 0.01)
        assertEquals("LOT-99", matUsedDomain.lotNumberSnapshot)

        val backMatUsedDto = matUsedDomain.toDto("evo-1")
        assertEquals("evo-1", backMatUsedDto.evolutionId)
        assertEquals("LOT-99", backMatUsedDto.lotNumberSnapshot)

        val evoDto = EvolutionDto(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "doc-1",
            date = "2026-10-20",
            procedureTime = "10:30:00",
            description = "Resina compuesta Oclusal",
            doctorName = "Dr. Mendoza",
            toothTreated = 24,
            cost = 150000.0,
            paymentAmount = 50000.0
        )
        val evoDomain = evoDto.toDomain(listOf(matUsedDomain))
        assertEquals("evo-1", evoDomain.id)
        assertEquals(100000.0, evoDomain.balance, 0.01)
        assertEquals(1, evoDomain.materialsUsed.size)

        val backEvoDto = evoDomain.toDto()
        assertEquals("evo-1", backEvoDto.id)
        assertEquals("rec-1", backEvoDto.recordId)
        assertEquals(100000.0, backEvoDto.balance, 0.01)
    }

    @Test
    fun `ClinicalMaterialDto toDomain and ClinicalMaterial toDto map fields accurately`() {
        val dto = ClinicalMaterialDto(
            id = "mat-1",
            name = "Resina 3M Filtek",
            invimaRegistration = "INVIMA-2022DM",
            batchLotNumber = "B-2026",
            manufacturingDate = "2025-01-01",
            expirationDate = "2028-01-01",
            currentStock = 15,
            unitOfMeasure = "JERINGA"
        )
        val domain = dto.toDomain()
        assertEquals("mat-1", domain.id)
        assertEquals("Resina 3M Filtek", domain.name)
        assertEquals(15, domain.currentStock)
        assertEquals("JERINGA", domain.unitOfMeasure)

        val backToDto = domain.toDto()
        assertEquals("mat-1", backToDto.id)
        assertEquals("INVIMA-2022DM", backToDto.invimaRegistration)
    }

    @Test
    fun `InformedConsentDto and InformedConsentProcedureDto map toDomain and toDto accurately`() {
        val procDto = InformedConsentProcedureDto(
            id = "proc-1",
            consentId = "cons-1",
            toothNumber = 36,
            procedureDate = "2026-10-20",
            patientSignatureUrl = "https://sig.test/pat.png"
        )
        val procDomain = procDto.toDomain()
        assertEquals("proc-1", procDomain.id)
        assertEquals(36, procDomain.toothNumber)

        val backProcDto = procDomain.toDto("cons-1")
        assertEquals("cons-1", backProcDto.consentId)
        assertEquals(36, backProcDto.toothNumber)

        val consentDto = InformedConsentDto(
            id = "cons-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            treatmentSummary = "Exodoncia simple",
            status = "SIGNED"
        )
        val consentDomain = consentDto.toDomain(listOf(procDomain))
        assertEquals("cons-1", consentDomain.id)
        assertEquals("Exodoncia simple", consentDomain.treatmentSummary)
        assertEquals(1, consentDomain.procedures.size)

        val backConsentDto = consentDomain.toDto()
        assertEquals("cons-1", backConsentDto.id)
        assertEquals("SIGNED", backConsentDto.status)
    }
}
