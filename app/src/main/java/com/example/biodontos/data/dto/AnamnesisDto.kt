package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnamnesisDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("tratamiento_medico_actual") val tratamientoMedicoActual: Boolean = false,
    @SerialName("toma_medicamentos") val tomaMedicamentos: Boolean = false,
    val alergias: Boolean = false,
    val cardiopatias: Boolean = false,
    @SerialName("alteracion_presion_arterial") val alteracionPresionArterial: Boolean = false,
    val embarazo: Boolean = false,
    val diabetes: Boolean = false,
    val hepatitis: Boolean = false,
    val irradiaciones: Boolean = false,
    @SerialName("discrasias_sanguineas") val discrasiasSanguineas: Boolean = false,
    @SerialName("fiebre_reumatica") val fiebreReumatica: Boolean = false,
    @SerialName("enfermedades_renales") val enfermedadesRenales: Boolean = false,
    val inmunosupresion: Boolean = false,
    @SerialName("trastornos_emocionales") val trastornosEmocionales: Boolean = false,
    @SerialName("patologia_respiratoria") val patologiaRespiratoria: Boolean = false,
    @SerialName("trastornos_gastricos") val trastornosGastricos: Boolean = false,
    val epilepsia: Boolean = false,
    val cirugias: Boolean = false,
    @SerialName("enfermedades_orales") val enfermedadesOrales: Boolean = false,
    @SerialName("otras_alteraciones") val otrasAlteraciones: Boolean = false,
    val observaciones: String = "",
    @SerialName("antecedentes_familiares") val antecedentesFamiliares: String = "",
    @SerialName("medicamentos_actuales") val medicamentosActuales: String? = null,
    @SerialName("alergias_descripcion") val alergiasDescripcion: String? = null
)
