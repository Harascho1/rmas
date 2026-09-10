package rs.coffeeconquest.shared.model

import kotlinx.serialization.Serializable

/**
 * Account roles. A user has exactly one role. "Gradski sampion" is deliberately
 * *not* a role: it is derived weekly from the leaderboard
 * (see `SocialSource.cityChampion`) and exposed as a flag on the profile.
 */
@Serializable
enum class Role {
    /** Plays the game: check-ins, reviews, leaderboard, following. */
    HUNTER,

    /** Runs one or more cafes: profile editing, stats, challenges, review replies. */
    OWNER,

    /** Works in a cafe: only issues QR codes that confirm a check-in on the spot. */
    STAFF,

    /** Keeps the system honest: approves cafes, invalidates check-ins, bans users. */
    ADMIN;

    val isStaffOrAbove: Boolean get() = this == STAFF || this == OWNER || this == ADMIN
}

/** Lifecycle of a cafe pin on the map. Hunters propose, admins approve. */
@Serializable
enum class CafeStatus { PENDING, APPROVED, REJECTED }

/** How a check-in was proven. */
@Serializable
enum class CheckInMethod {
    /** Phone GPS was within [rs.coffeeconquest.shared.rules.Geo.MAX_CHECKIN_DISTANCE_M] of the cafe. */
    GPS,

    /** Staff showed a rotating QR code that the hunter scanned - the strongest proof. */
    QR,

    /** "I was there, trust me." Accepted, but worth fewer points and always flagged for review. */
    HONOR
}

/** Moderation state of a check-in. */
@Serializable
enum class CheckInStatus { VALID, FLAGGED, INVALIDATED }

/** Which slice of the leaderboard to return. */
@Serializable
enum class LeaderboardScope { GLOBAL, CITY, FRIENDS }

/** Things that show up in the activity feed. */
@Serializable
enum class FeedEventType { CHECK_IN, REVIEW, BADGE, CHALLENGE_CREATED, CAFE_APPROVED }

/** Scope a challenge applies to. */
@Serializable
enum class ChallengeScope {
    /** Created by an owner/staff for a single cafe. */
    CAFE,

    /** Created by the current city champion or an admin, applies to a whole city. */
    CITY
}
