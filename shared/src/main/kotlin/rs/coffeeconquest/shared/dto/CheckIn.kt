package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.rules.ScoreRules

@Serializable
data class CreateCheckInRequest(
    val cafeId: String,
    val method: CheckInMethod,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Required when [method] is QR - the rotating token from the cafe's screen. */
    val qrToken: String? = null,
    val photoId: String? = null,
    val note: String? = null,
    /** Optional rating submitted together with the check-in. */
    val rating: Int? = null,
    val comment: String? = null,
)

@Serializable
data class CheckIn(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val userId: String,
    val username: String,
    val method: CheckInMethod,
    val status: CheckInStatus,
    val pointsAwarded: Int,
    val photoId: String? = null,
    val note: String? = null,
    val createdAtEpochMs: Long,
    val distanceMeters: Double? = null,
    val flagReason: String? = null,
)

/** Response to a successful check-in: what happened and what it earned. */
@Serializable
data class CheckInResult(
    val checkIn: CheckIn,
    val breakdown: ScoreRules.ScoreBreakdown,
    val newTotalPoints: Int,
    val level: Int,
    val streakDays: Int,
    val newBadges: List<EarnedBadge> = emptyList(),
    /** Set when the check-in overtook someone on the city leaderboard. */
    val overtook: List<String> = emptyList(),
)

/** The rotating token a staff member shows on screen. */
@Serializable
data class QrTokenResponse(
    val cafeId: String,
    val cafeName: String,
    val token: String,
    val expiresAtEpochMs: Long,
)
