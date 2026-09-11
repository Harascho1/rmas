package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.CafeStatus
import rs.coffeeconquest.shared.model.CafeType

@Serializable
data class Cafe(
    val id: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
    val status: CafeStatus,
    val type: CafeType = CafeType.KAFIC,
    val openingHours: String? = null,
    val photoId: String? = null,
    val tags: List<String> = emptyList(),
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val checkInCount: Int = 0,
    val ownerId: String? = null,
    val proposedById: String? = null,
    val createdAtEpochMs: Long = 0,
    val distanceMeters: Double? = null,
    val topVisitor: CafeConqueror? = null,
    val activeChallenges: List<Challenge> = emptyList(),
    val myCheckInCount: Int = 0,
)

@Serializable
data class CafeConqueror(
    val userId: String,
    val username: String,
    val displayName: String,
    val visits: Int,
)

@Serializable
data class CreateCafeRequest(
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
    val type: CafeType = CafeType.KAFIC,
    val openingHours: String? = null,
    val photoId: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class UpdateCafeRequest(
    val name: String? = null,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val type: CafeType? = null,
    val openingHours: String? = null,
    val photoId: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class Review(
    val id: String,
    val cafeId: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val rating: Int,
    val comment: String? = null,
    val createdAtEpochMs: Long,
    val ownerReply: String? = null,
    val ownerReplyAtEpochMs: Long? = null,
)

@Serializable
data class CafeStats(
    val cafeId: String,
    val cafeName: String,
    val totalCheckIns: Int,
    val checkInsLast7Days: Int,
    val checkInsLast30Days: Int,
    val uniqueVisitors: Int,
    val averageRating: Double,
    val reviewCount: Int,
    val unansweredReviews: Int,
    val dailyCheckIns: List<DailyCount>,
    val topVisitors: List<CafeConqueror>,
)

@Serializable
data class DailyCount(val dayEpochMs: Long, val count: Int)

@Serializable
data class CafeFilter(
    val query: String? = null,
    val type: CafeType? = null,
    val attributes: List<String> = emptyList(),
    val authorUsername: String? = null,
    val onlyMine: Boolean = false,
    val addedWithinDays: Int? = null,
    val minRating: Double? = null,
) {
    val activeCount: Int
        get() = listOfNotNull(
            type,
            attributes.takeIf { it.isNotEmpty() },
            authorUsername?.takeIf { it.isNotBlank() },
            true.takeIf { onlyMine },
            addedWithinDays,
            minRating,
        ).size

    val isActive: Boolean get() = activeCount > 0

    companion object {
        val DATE_PRESETS: List<Pair<String, Int?>> = listOf(
            "Bilo kada" to null,
            "7 dana" to 7,
            "30 dana" to 30,
            "Godinu dana" to 365,
        )
    }
}
