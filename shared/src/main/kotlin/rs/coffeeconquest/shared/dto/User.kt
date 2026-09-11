package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.Role

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val role: Role,
    val city: String? = null,
    val avatarPhotoId: String? = null,
    val points: Int = 0,
    val level: Int = 1,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val checkInCount: Int = 0,
    val conqueredCafes: Int = 0,
    val reviewCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isCityChampion: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val createdAtEpochMs: Long = 0,
    val isFollowedByMe: Boolean = false,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val city: String? = null,
    val avatarPhotoId: String? = null,
)

@Serializable
data class EarnedBadge(
    val code: String,
    val title: String,
    val description: String,
    val emoji: String,
    val earnedAtEpochMs: Long,
)

@Serializable
data class UserStats(
    val profile: UserProfile,
    val badges: List<EarnedBadge>,
    val conquered: List<ConqueredCafe>,
)

@Serializable
data class ConqueredCafe(
    val cafeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val visits: Int,
    val lastVisitEpochMs: Long,
    val isTopVisitor: Boolean,
)
