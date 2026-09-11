package rs.coffeeconquest.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    HUNTER,

    OWNER,

    STAFF,

    ADMIN;

    val isStaffOrAbove: Boolean get() = this == STAFF || this == OWNER || this == ADMIN
}

@Serializable
enum class CafeStatus { PENDING, APPROVED, REJECTED }

@Serializable
enum class CheckInMethod {
    GPS,

    QR,

    HONOR
}

@Serializable
enum class CheckInStatus { VALID, FLAGGED, INVALIDATED }

@Serializable
enum class LeaderboardScope { GLOBAL, CITY, FRIENDS }

@Serializable
enum class FeedEventType { CHECK_IN, REVIEW, BADGE, CHALLENGE_CREATED, CAFE_APPROVED }

@Serializable
enum class ChallengeScope {
    CAFE,

    CITY
}
