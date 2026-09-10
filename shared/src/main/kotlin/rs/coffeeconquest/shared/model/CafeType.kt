package rs.coffeeconquest.shared.model

import kotlinx.serialization.Serializable

/**
 * What kind of place a POI is. Stored as the enum name on the cafe document and
 * used as the "tip" filter on the map.
 */
@Serializable
enum class CafeType(val label: String) {
    KAFIC("Kafic"),
    PRZIONICA("Przionica"),
    POSLASTICARNICA("Poslasticarnica"),
    BAR("Bar"),
    RESTORAN("Restoran"),
    OSTALO("Ostalo"),
}

/**
 * The suggested attribute vocabulary ("atributi").
 *
 * Tags stay free-form so a hunter can add anything, but the app offers this list
 * both when proposing a cafe and when filtering, so the common ones stay
 * spelled consistently and are actually findable.
 */
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
