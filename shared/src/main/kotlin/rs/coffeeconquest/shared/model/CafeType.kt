package rs.coffeeconquest.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class CafeType(val label: String) {
    KAFIC("Kafic"),
    PRZIONICA("Przionica"),
    POSLASTICARNICA("Poslasticarnica"),
    BAR("Bar"),
    RESTORAN("Restoran"),
    OSTALO("Ostalo"),
}

object CafeAttributes {

    const val WIFI = "wifi"
    const val TERASA = "terasa"
    const val ZA_RAD = "za-rad"
    const val SPECIALTY = "specialty"
    const val VEGAN = "vegan"
    const val LJUBIMCI = "ljubimci"
    const val NEPUSACKI = "nepusacki"
    const val DORUCAK = "dorucak"

    val all: List<String> = listOf(
        WIFI, TERASA, ZA_RAD, SPECIALTY, VEGAN, LJUBIMCI, NEPUSACKI, DORUCAK,
    )
}
