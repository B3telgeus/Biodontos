package com.example.biodontos.domain.model

enum class AnamnesisCondition(
    val key: String,
    val question: String,
    val shortLabel: String,
    val noteHint: String
) {
    TRATAMIENTO_MEDICO("tratamientoMedicoActual", "¿Está bajo tratamiento médico actual?", "Tratamiento médico actual", "Especifique el tratamiento médico actual y especialista..."),
    TOMA_MEDICAMENTOS("tomaMedicamentos", "¿Toma algún medicamento actualmente?", "Toma medicamentos", "Indique nombre del medicamento, dosis y frecuencia..."),
    ALERGIAS("alergias", "¿Tiene alergia a medicamentos, alimentos o látex?", "Alergias", "Indique a qué sustancias presenta reacción alérgica..."),
    CARDIOPATIAS("cardiopatias", "¿Padece o ha padecido del corazón o hipertensión?", "Cardiopatías", "Detalle la afección cardiaca o diagnóstico..."),
    ALTERACION_PRESION("alteracionPresionArterial", "¿Presenta alteración en la presión arterial?", "Presión arterial", "Indique si suele tener presión alta o baja..."),
    EMBARAZO("embarazo", "¿Está en estado de embarazo?", "Embarazo", "Indique semanas o meses de gestación..."),
    DIABETES("diabetes", "¿Padece de diabetes?", "Diabetes", "Indique tipo (1 o 2) y si está controlado..."),
    HEPATITIS("hepatitis", "¿Ha padecido hepatitis u otra enfermedad del hígado?", "Hepatitis", "Indique tipo de hepatitis o afección hepática..."),
    IRRADIACIONES("irradiaciones", "¿Ha recibido quimioterapia o radioterapia?", "Irradiaciones / Quimioterapia", "Indique fecha y zona del tratamiento recibido..."),
    DISCRASIAS_SANGUINEAS("discrasiasSanguineas", "¿Presenta problemas de coagulación o sangrado excesivo?", "Trastornos de coagulación", "Detalle el trastorno hemorrágico..."),
    FIEBRE_REUMATICA("fiebreReumatica", "¿Ha tenido fiebre reumática?", "Fiebre reumática", "Indique antecedentes o secuelas conocidas..."),
    ENFERMEDADES_RENALES("enfermedadesRenales", "¿Padece alguna enfermedad renal?", "Enf. Renales", "Indique diagnóstico o si requiere diálisis..."),
    INMUNOSUPRESION("inmunosupresion", "¿Tiene VIH/SIDA o condición de inmunosupresión?", "Inmunosupresión", "Detalle la condición si desea especificar..."),
    TRASTORNOS_EMOCIONALES("trastornosEmocionales", "¿Sufre de ansiedad, depresión o estrés severo?", "Trastornos emocionales", "Detalle el tratamiento o estado actual..."),
    PATOLOGIA_RESPIRATORIA("patologiaRespiratoria", "¿Padece asma, EPOC u otra enfermedad respiratoria?", "Patología respiratoria", "Indique afección y si usa inhalador..."),
    TRASTORNOS_GASTRICOS("trastornosGastricos", "¿Tiene problemas de úlcera, gastritis o reflujo severo?", "Trastornos gástricos", "Detalle la patología gástrica..."),
    EPILEPSIA("epilepsia", "¿Ha sufrido convulsiones o epilepsia?", "Epilepsia", "Indique cuándo fue el último episodio..."),
    CIRUGIAS("cirugias", "¿Le han realizado cirugías o transfusiones previas?", "Cirugías previas", "Indique tipo de cirugía y año aproximado..."),
    ENFERMEDADES_ORALES("enfermedadesOrales", "¿Tiene o ha tenido lesiones, llagas o sangrado en encías?", "Enfermedades orales", "Describa la afección oral..."),
    OTRAS_ALTERACIONES("otrasAlteraciones", "¿Presenta alguna otra condición de salud relevante?", "Otras alteraciones", "Describa cualquier otra condición importante...");

    companion object {
        fun fromKey(key: String): AnamnesisCondition? = entries.firstOrNull { it.key == key }
    }
}

data class Anamnesis(
    val id: String = "",
    val patientId: String,
    val tratamientoMedicoActual: Boolean = false,
    val tomaMedicamentos: Boolean = false,
    val alergias: Boolean = false,
    val cardiopatias: Boolean = false,
    val alteracionPresionArterial: Boolean = false,
    val embarazo: Boolean = false,
    val diabetes: Boolean = false,
    val hepatitis: Boolean = false,
    val irradiaciones: Boolean = false,
    val discrasiasSanguineas: Boolean = false,
    val fiebreReumatica: Boolean = false,
    val enfermedadesRenales: Boolean = false,
    val inmunosupresion: Boolean = false,
    val trastornosEmocionales: Boolean = false,
    val patologiaRespiratoria: Boolean = false,
    val trastornosGastricos: Boolean = false,
    val epilepsia: Boolean = false,
    val cirugias: Boolean = false,
    val enfermedadesOrales: Boolean = false,
    val otrasAlteraciones: Boolean = false,
    val observaciones: String = "",
    val antecedentesFamiliares: String = "",
    val medicamentosActuales: String? = null,
    val alergiasDescripcion: String? = null,
    val optionNotes: Map<String, String> = emptyMap()
) {
    fun hasCondition(condition: AnamnesisCondition): Boolean = when (condition) {
        AnamnesisCondition.TRATAMIENTO_MEDICO -> tratamientoMedicoActual
        AnamnesisCondition.TOMA_MEDICAMENTOS -> tomaMedicamentos
        AnamnesisCondition.ALERGIAS -> alergias
        AnamnesisCondition.CARDIOPATIAS -> cardiopatias
        AnamnesisCondition.ALTERACION_PRESION -> alteracionPresionArterial
        AnamnesisCondition.EMBARAZO -> embarazo
        AnamnesisCondition.DIABETES -> diabetes
        AnamnesisCondition.HEPATITIS -> hepatitis
        AnamnesisCondition.IRRADIACIONES -> irradiaciones
        AnamnesisCondition.DISCRASIAS_SANGUINEAS -> discrasiasSanguineas
        AnamnesisCondition.FIEBRE_REUMATICA -> fiebreReumatica
        AnamnesisCondition.ENFERMEDADES_RENALES -> enfermedadesRenales
        AnamnesisCondition.INMUNOSUPRESION -> inmunosupresion
        AnamnesisCondition.TRASTORNOS_EMOCIONALES -> trastornosEmocionales
        AnamnesisCondition.PATOLOGIA_RESPIRATORIA -> patologiaRespiratoria
        AnamnesisCondition.TRASTORNOS_GASTRICOS -> trastornosGastricos
        AnamnesisCondition.EPILEPSIA -> epilepsia
        AnamnesisCondition.CIRUGIAS -> cirugias
        AnamnesisCondition.ENFERMEDADES_ORALES -> enfermedadesOrales
        AnamnesisCondition.OTRAS_ALTERACIONES -> otrasAlteraciones
    }

    fun activeConditions(): Set<AnamnesisCondition> =
        AnamnesisCondition.entries.filterTo(mutableSetOf()) { hasCondition(it) }

    fun withConditions(active: Set<AnamnesisCondition>): Anamnesis = copy(
        tratamientoMedicoActual = AnamnesisCondition.TRATAMIENTO_MEDICO in active,
        tomaMedicamentos = AnamnesisCondition.TOMA_MEDICAMENTOS in active,
        alergias = AnamnesisCondition.ALERGIAS in active,
        cardiopatias = AnamnesisCondition.CARDIOPATIAS in active,
        alteracionPresionArterial = AnamnesisCondition.ALTERACION_PRESION in active,
        embarazo = AnamnesisCondition.EMBARAZO in active,
        diabetes = AnamnesisCondition.DIABETES in active,
        hepatitis = AnamnesisCondition.HEPATITIS in active,
        irradiaciones = AnamnesisCondition.IRRADIACIONES in active,
        discrasiasSanguineas = AnamnesisCondition.DISCRASIAS_SANGUINEAS in active,
        fiebreReumatica = AnamnesisCondition.FIEBRE_REUMATICA in active,
        enfermedadesRenales = AnamnesisCondition.ENFERMEDADES_RENALES in active,
        inmunosupresion = AnamnesisCondition.INMUNOSUPRESION in active,
        trastornosEmocionales = AnamnesisCondition.TRASTORNOS_EMOCIONALES in active,
        patologiaRespiratoria = AnamnesisCondition.PATOLOGIA_RESPIRATORIA in active,
        trastornosGastricos = AnamnesisCondition.TRASTORNOS_GASTRICOS in active,
        epilepsia = AnamnesisCondition.EPILEPSIA in active,
        cirugias = AnamnesisCondition.CIRUGIAS in active,
        enfermedadesOrales = AnamnesisCondition.ENFERMEDADES_ORALES in active,
        otrasAlteraciones = AnamnesisCondition.OTRAS_ALTERACIONES in active
    )
}

