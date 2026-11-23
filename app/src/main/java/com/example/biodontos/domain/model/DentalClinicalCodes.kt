package com.example.biodontos.domain.model

import java.text.Normalizer

data class Cie10Code(
    val code: String,
    val description: String
) {
    val displayLabel: String get() = "$code - $description"
}

data class CupsCode(
    val code: String,
    val description: String,
    val category: String
) {
    val displayLabel: String get() = "$code - $description"
}

object DentalClinicalCodes {
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

    val cie10Catalog: List<Cie10Code> = listOf(
        Cie10Code("K02.0", "Caries limitada al esmalte"),
        Cie10Code("K02.1", "Caries de la dentina"),
        Cie10Code("K02.2", "Caries del cemento"),
        Cie10Code("K02.3", "Caries dentaria detenida"),
        Cie10Code("K02.8", "Otras caries dentales"),
        Cie10Code("K02.9", "Caries dental, no especificada"),
        Cie10Code("K03.0", "Atrición excesiva de los dientes"),
        Cie10Code("K03.1", "Abrasión de los dientes"),
        Cie10Code("K03.2", "Erosión de los dientes"),
        Cie10Code("K03.6", "Depósitos [acreciones] en los dientes (cálculo dental)"),
        Cie10Code("K04.0", "Pulpitis"),
        Cie10Code("K04.1", "Necrosis de la pulpa"),
        Cie10Code("K04.4", "Periodontitis apical aguda originada en la pulpa"),
        Cie10Code("K04.5", "Periodontitis apical crónica"),
        Cie10Code("K04.6", "Absceso periapical con fístula"),
        Cie10Code("K04.7", "Absceso periapical sin fístula"),
        Cie10Code("K04.8", "Quiste radicular"),
        Cie10Code("K05.0", "Gingivitis aguda"),
        Cie10Code("K05.1", "Gingivitis crónica"),
        Cie10Code("K05.2", "Periodontitis aguda"),
        Cie10Code("K05.3", "Periodontitis crónica"),
        Cie10Code("K05.4", "Periodontosis"),
        Cie10Code("K05.6", "Enfermedad del periodonto, no especificada"),
        Cie10Code("K06.0", "Retracción gingival"),
        Cie10Code("K06.1", "Hiperplasia gingival"),
        Cie10Code("K00.6", "Alteraciones en la erupción dentaria"),
        Cie10Code("K01.0", "Dientes incluidos"),
        Cie10Code("K01.1", "Dientes impactados"),
        Cie10Code("K07.0", "Anomalías evidentes del tamaño de los maxilares"),
        Cie10Code("K07.2", "Anomalías de la relación entre los arcos dentarios"),
        Cie10Code("K07.3", "Anomalías de la posición del diente"),
        Cie10Code("K07.4", "Maloclusión de tipo no especificado"),
        Cie10Code("K07.6", "Trastornos de la articulación temporomandibular"),
        Cie10Code("K08.1", "Pérdida de dientes debida a accidente, extracción o enfermedad periodontal"),
        Cie10Code("K08.3", "Raíz dental retenida"),
        Cie10Code("K08.8", "Otras afecciones especificadas de los dientes y estructuras de sostén"),
        Cie10Code("K10.3", "Alveolitis del maxilar"),
        Cie10Code("K12.0", "Estomatitis aftosa recurrente"),
        Cie10Code("S02.5", "Fractura de los dientes"),
        Cie10Code("Z01.2", "Examen odontológico")
    )

    val cupsCatalog: List<CupsCode> = listOf(
        // Consultas
        CupsCode("890203", "Consulta de primera vez por odontología general", "Consulta"),
        CupsCode("890303", "Consulta de control o seguimiento por odontología general", "Consulta"),
        CupsCode("890703", "Consulta de urgencias por odontología general", "Consulta"),
        CupsCode("890204", "Consulta de primera vez por odontología especializada", "Consulta"),
        CupsCode("890304", "Consulta de control por odontología especializada", "Consulta"),

        // Promoción y Prevención
        CupsCode("997101", "Aplicación de sellantes de fotocurado", "Prevención"),
        CupsCode("997102", "Aplicación de sellantes de autocurado", "Prevención"),
        CupsCode("997103", "Topicación de flúor en gel", "Prevención"),
        CupsCode("997104", "Topicación de flúor en solución / barniz", "Prevención"),
        CupsCode("997106", "Control de placa dental", "Prevención"),
        CupsCode("997300", "Profilaxis dental (eliminación de placa bacteriana)", "Prevención"),
        CupsCode("997310", "Detartraje supragingival (raspaje)", "Prevención"),

        // Operatoria Dental
        CupsCode("232101", "Obturación dental por superficie con amalgama", "Operatoria"),
        CupsCode("232102", "Obturación dental por superficie con resina de fotocurado", "Operatoria"),
        CupsCode("232103", "Obturación dental por superficie con ionómero de vidrio", "Operatoria"),
        CupsCode("232200", "Obturación temporal por diente", "Operatoria"),
        CupsCode("232401", "Reconstrucción de ángulo incisal con resina de fotocurado", "Operatoria"),
        CupsCode("232402", "Reconstrucción tercio incisal con resina de fotocurado", "Operatoria"),
        CupsCode("232501", "Blanqueamiento dental intrínseco o extrínseco", "Estética"),

        // Endodoncia
        CupsCode("237102", "Pulpotomía", "Endodoncia"),
        CupsCode("237103", "Pulpectomía", "Endodoncia"),
        CupsCode("237301", "Terapia de conducto radicular en diente unirradicular", "Endodoncia"),
        CupsCode("237302", "Terapia de conducto radicular en diente birradicular", "Endodoncia"),
        CupsCode("237303", "Terapia de conducto radicular en diente multirradicular", "Endodoncia"),
        CupsCode("237304", "Retratamiento de conducto radicular", "Endodoncia"),

        // Periodoncia
        CupsCode("240200", "Detartraje subgingival y alisado radicular (por cuadrante)", "Periodoncia"),
        CupsCode("240300", "Alisado radicular campo abierto", "Periodoncia"),
        CupsCode("243101", "Gingivectomía / gingivoplastia", "Periodoncia"),
        CupsCode("243102", "Curetaje a campo cerrado", "Periodoncia"),

        // Cirugía Oral y Exodoncias
        CupsCode("230101", "Exodoncia de diente permanente unirradicular", "Cirugía"),
        CupsCode("230102", "Exodoncia de diente permanente multirradicular", "Cirugía"),
        CupsCode("230201", "Exodoncia de diente temporal unirradicular", "Cirugía"),
        CupsCode("230202", "Exodoncia de diente temporal multirradicular", "Cirugía"),
        CupsCode("231100", "Exodoncia quirúrgica de diente incluido o impactado", "Cirugía"),
        CupsCode("231301", "Exodoncia de restos radiculares (método abierto)", "Cirugía"),
        CupsCode("240400", "Drenaje de absceso periodontal o periapical", "Cirugía"),

        // Rehabilitación Oral y Prótesis
        CupsCode("234101", "Inserción o adaptación de prótesis removible parcial", "Rehabilitación"),
        CupsCode("234201", "Inserción o adaptación de prótesis total", "Rehabilitación"),
        CupsCode("234301", "Corona individual en metal-porcelana o cerámica", "Rehabilitación"),
        CupsCode("234401", "Cementación de corona o prótesis fija", "Rehabilitación"),
        CupsCode("234402", "Núcleo o poste intrarradicular", "Rehabilitación"),

        // Ortodoncia
        CupsCode("247100", "Colocación de aparatología ortodóntica fija", "Ortodoncia"),
        CupsCode("247201", "Control mensual de ortodoncia", "Ortodoncia"),
        CupsCode("247300", "Retiro de aparatología ortodóntica y retenedores", "Ortodoncia"),

        // Radiología Oral
        CupsCode("870440", "Radiografía periapical", "Radiología"),
        CupsCode("870450", "Radiografía oclusal", "Radiología"),
        CupsCode("870460", "Radiografía panorámica de maxilares", "Radiología")
    )

    fun findCie10ByCode(code: String?): Cie10Code? =
        code?.trim()?.takeIf { it.isNotEmpty() }?.let { c ->
            cie10Catalog.firstOrNull { it.code.equals(c, ignoreCase = true) }
        }

    fun findCupsByCode(code: String?): CupsCode? =
        code?.trim()?.takeIf { it.isNotEmpty() }?.let { c ->
            cupsCatalog.firstOrNull { it.code.equals(c, ignoreCase = true) }
        }

    fun searchCie10(query: String): List<Cie10Code> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return cie10Catalog
        return cie10Catalog.filter { item ->
            normalize(item.code).contains(normalized) ||
                normalize(item.description).contains(normalized)
        }
    }

    fun searchCups(query: String): List<CupsCode> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return cupsCatalog
        return cupsCatalog.filter { item ->
            normalize(item.code).contains(normalized) ||
                normalize(item.description).contains(normalized) ||
                normalize(item.category).contains(normalized)
        }
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(DIACRITICS_REGEX, "")
    }
}
