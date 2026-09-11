package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.ChallengeScope

@Serializable
data class Challenge(
    val id: String,
    val scope: ChallengeScope,
    val cafeId: String? = null,
    val cafeName: String? = null,
    val city: String? = null,
    val title: String,
    val description: String? = null,
    val multiplier: Double,
    val startsAtEpochMs: Long,
    val endsAtEpochMs: Long,
    val createdById: String,
    val createdByDisplayName: String,
    val active: Boolean,
) {
    fun appliesTo(cafe: Cafe): Boolean = when (scope) {
        ChallengeScope.CAFE -> cafeId == cafe.id
        ChallengeScope.CITY -> city != null && city.equals(cafe.city, ignoreCase = true)
    }
}

@Serializable
data class CreateChallengeRequest(
    val scope: ChallengeScope,
    val cafeId: String? = null,
    val city: String? = null,
    val title: String,
    val description: String? = null,
    val multiplier: Double = 2.0,
    val startsAtEpochMs: Long,
    val endsAtEpochMs: Long,
)
