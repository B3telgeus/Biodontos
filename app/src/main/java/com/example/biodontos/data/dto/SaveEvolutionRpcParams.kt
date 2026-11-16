package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaveEvolutionRpcParams(
    @SerialName("p_evolution")
    val evolution: EvolutionDto,
    @SerialName("p_materials")
    val materials: List<EvolutionMaterialUsedDto>,
    @SerialName("p_close_record")
    val closeRecord: Boolean = false,
    @SerialName("p_closure_summary")
    val closureSummary: String? = null,
    @SerialName("p_closure_signature_url")
    val closureSignatureUrl: String? = null
) {
    override fun toString(): String {
        return "SaveEvolutionRpcParams(p_evolution=$evolution, evolution=$evolution, p_materials=$materials, materials=$materials, p_close_record=$closeRecord, closeRecord=$closeRecord, p_closure_summary=$closureSummary, closureSummary=$closureSummary, p_closure_signature_url=$closureSignatureUrl, closureSignatureUrl=$closureSignatureUrl)"
    }
}

@Serializable
data class SaveEvolutionRpcResponse(
    val success: Boolean = true,
    @SerialName("evolution_id")
    val evolutionId: String? = null
)
