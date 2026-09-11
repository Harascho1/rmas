package rs.coffeeconquest.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class BadgeDefinition(
    val code: String,
    val title: String,
    val description: String,
    val requirement: String,
    val emoji: String,
)

object Badges {
    const val FIRST_SIP = "FIRST_SIP"
    const val EXPLORER_5 = "EXPLORER_5"
    const val EXPLORER_10 = "EXPLORER_10"
    const val EXPLORER_25 = "EXPLORER_25"
    const val STREAK_7 = "STREAK_7"
    const val STREAK_30 = "STREAK_30"
    const val CRITIC_10 = "CRITIC_10"
    const val EARLY_BIRD = "EARLY_BIRD"
    const val NIGHT_OWL = "NIGHT_OWL"
    const val CITY_CHAMPION = "CITY_CHAMPION"

    val all: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            FIRST_SIP, "Prvi gutljaj",
            "Prvi check-in u aplikaciji.",
            "Uradite svoj prvi check-in u bilo kom kaficu.",
            "☕",
        ),
        BadgeDefinition(
            EXPLORER_5, "Istrazivac",
            "Osvojeno 5 razlicitih kafica.",
            "Uradite check-in u 5 razlicitih kafica. Ponovljene posete se ne racunaju.",
            "🧭",
        ),
        BadgeDefinition(
            EXPLORER_10, "Kartograf",
            "Osvojeno 10 razlicitih kafica.",
            "Uradite check-in u 10 razlicitih kafica.",
            "🗺",
        ),
        BadgeDefinition(
            EXPLORER_25, "Gospodar grada",
            "Osvojeno 25 razlicitih kafica.",
            "Uradite check-in u 25 razlicitih kafica.",
            "🏰",
        ),
        BadgeDefinition(
            STREAK_7, "Nedelja bez pauze",
            "7 dana zaredom sa check-inom.",
            "Uradite bar jedan check-in svakog dana, 7 dana zaredom. " +
                "Jedan preskocen dan vraca niz na nulu.",
            "🔥",
        ),
        BadgeDefinition(
            STREAK_30, "Mesec bez pauze",
            "30 dana zaredom sa check-inom.",
            "Uradite bar jedan check-in svakog dana, 30 dana zaredom.",
            "🌋",
        ),
        BadgeDefinition(
            CRITIC_10, "Kriticar",
            "Ostavljeno 10 recenzija.",
            "Ostavite 10 recenzija sa ocenom. Broji se po jedna po kaficu.",
            "✍",
        ),
        BadgeDefinition(
            EARLY_BIRD, "Ranoranilac",
            "Check-in pre 8h ujutru.",
            "Uradite check-in pre 8 sati ujutru.",
            "🌅",
        ),
        BadgeDefinition(
            NIGHT_OWL, "Nocna smena",
            "Check-in posle 22h.",
            "Uradite check-in posle 22 sata.",
            "🦉",
        ),
        BadgeDefinition(
            CITY_CHAMPION, "Gradski sampion",
            "Prvo mesto na gradskoj listi.",
            "Zauzmite prvo mesto na rang listi svog grada.",
            "👑",
        ),
    )

    private val byCode = all.associateBy { it.code }

    fun find(code: String): BadgeDefinition? = byCode[code]
}
