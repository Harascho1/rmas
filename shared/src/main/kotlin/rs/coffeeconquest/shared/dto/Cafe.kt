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
    /** Filled in by the radius search so the app can sort and label pins. */
    val distanceMeters: Double? = null,
    /** The hunter with the most check-ins here - the cafe's "osvajac". */
    val topVisitor: CafeConqueror? = null,
    /** Bonus challenges currently running at this cafe. */
    val activeChallenges: List<Challenge> = emptyList(),
    /** How many times the signed-in hunter has scored here; 0 means the cafe is unconquered. */
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

/** What an owner sees on their dashboard. */
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
    /** Check-in counts per day for the last 14 days, oldest first. */
    val dailyCheckIns: List<DailyCount>,
    val topVisitors: List<CafeConqueror>,
)

@Serializable
data class DailyCount(val dayEpochMs: Long, val count: Int)

/**
 * Everything the map screen can narrow the POI list by.
 *
 * Position and radius stay separate arguments because Firestore answers those in
 * the query itself; the fields here are applied to the result, which is what
 * lets them be combined freely - Firestore would otherwise need one composite
 * index per combination.
 */
@Serializable
data class CafeFilter(
    /** Free text over name and address. */
    val query: String? = null,
    val type: CafeType? = null,
    /** A cafe must carry *all* of these tags to match. */
    val attributes: List<String> = emptyList(),
    /** Whoever proposed the cafe, by username. */
    val authorUsername: String? = null,
    /** Shortcut for "cafes I proposed", without typing your own name. */
    val onlyMine: Boolean = false,
    /** Added within this many days; null means any time. */
    val addedWithinDays: Int? = null,
    val minRating: Double? = null,
) {
    /** How many narrowing choices are active - drives the badge on the filter button. */
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
        /** Presets offered for the "datumi" filter, in days. */
        val DATE_PRESETS: List<Pair<String, Int?>> = listOf(
            "Bilo kada" to null,
            "7 dana" to 7,
            "30 dana" to 30,
            "Godinu dana" to 365,
        )
    }
}
