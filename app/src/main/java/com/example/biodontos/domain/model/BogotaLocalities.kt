package com.example.biodontos.domain.model

object BogotaLocalities {
    const val OTHER_OPTION = "Otra"

    val OFFICIAL_TWENTY: List<String> = listOf(
        "Usaquén",
        "Chapinero",
        "Santa Fe",
        "San Cristóbal",
        "Usme",
        "Tunjuelito",
        "Bosa",
        "Kennedy",
        "Fontibón",
        "Engativá",
        "Suba",
        "Barrios Unidos",
        "Teusaquillo",
        "Los Mártires",
        "Antonio Nariño",
        "Puente Aranda",
        "La Candelaria",
        "Rafael Uribe Uribe",
        "Ciudad Bolívar",
        "Sumapaz"
    )

    val ALL_OPTIONS: List<String> = OFFICIAL_TWENTY + OTHER_OPTION

    fun isOfficialLocality(value: String): Boolean =
        OFFICIAL_TWENTY.any { it.equals(value.trim(), ignoreCase = true) }
}
