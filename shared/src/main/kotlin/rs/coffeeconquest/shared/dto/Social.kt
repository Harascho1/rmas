package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.FeedEventType
import rs.coffeeconquest.shared.model.LeaderboardScope

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarPhotoId: String? = null,
    val points: Int,
    val level: Int,
    val checkInCount: Int,
    val city: String? = null,
    val isMe: Boolean = false,
)

@Serializable
data class LeaderboardResponse(
    val scope: LeaderboardScope,
    val city: String? = null,
    /** Null when the leaderboard covers all time. */
    val sinceEpochMs: Long? = null,
    val entries: List<LeaderboardEntry>,
    /** The signed-in user's row, even when it falls outside the returned page. */
    val me: LeaderboardEntry? = null,
)

@Serializable
data class FeedItem(
    val id: String,
    val type: FeedEventType,
    val actorId: String,
    val actorUsername: String,
    val actorDisplayName: String,
    val cafeId: String? = null,
    val cafeName: String? = null,
    val text: String,
    val photoId: String? = null,
    val points: Int? = null,
    val createdAtEpochMs: Long,
)

@Serializable
data class CityChampion(
    val city: String,
    val user: LeaderboardEntry?,
    val weekStartEpochMs: Long,
)

@Serializable
data class FollowResponse(val following: Boolean, val followerCount: Int)
