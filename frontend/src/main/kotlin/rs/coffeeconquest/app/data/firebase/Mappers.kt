package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeConqueror
import rs.coffeeconquest.shared.dto.Challenge
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.ConqueredCafe
import rs.coffeeconquest.shared.dto.EarnedBadge
import rs.coffeeconquest.shared.dto.FeedItem
import rs.coffeeconquest.shared.dto.Review
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Badges
import rs.coffeeconquest.shared.model.CafeStatus
import rs.coffeeconquest.shared.model.CafeType
import rs.coffeeconquest.shared.model.ChallengeScope
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.FeedEventType
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.Geo
import rs.coffeeconquest.shared.rules.ScoreRules
import rs.coffeeconquest.shared.rules.Time

/**
 * Firestore document -> DTO. The counters a screen needs (points, ratings, visits)
 * are stored on the document itself, so mapping never triggers another read.
 */

fun DocumentSnapshot.toUserProfile(
    isFollowedByMe: Boolean = false,
    isCityChampion: Boolean = false,
): UserProfile {
    val points = int("points")
    return UserProfile(
        id = id,
        username = str("username").orEmpty(),
        displayName = str("displayName").orEmpty(),
        role = enum("role", Role.HUNTER),
        city = str("city"),
        avatarPhotoId = str("avatarPhotoId"),
        points = points,
        level = ScoreRules.levelFor(points),
        currentStreakDays = int("currentStreakDays"),
        longestStreakDays = int("longestStreakDays"),
        checkInCount = int("checkInCount"),
        conqueredCafes = int("conqueredCafes"),
        reviewCount = int("reviewCount"),
        followerCount = int("followerCount"),
        followingCount = int("followingCount"),
        isCityChampion = isCityChampion,
        isBanned = bool("isBanned"),
        banReason = str("banReason"),
        createdAtEpochMs = long("createdAt"),
        isFollowedByMe = isFollowedByMe,
    )
}

fun DocumentSnapshot.toCafe(
    viewerLat: Double? = null,
    viewerLon: Double? = null,
    myCheckInCount: Int = 0,
    activeChallenges: List<Challenge> = emptyList(),
): Cafe {
    val lat = double("latitude")
    val lon = double("longitude")
    val reviewCount = int("reviewCount")
    return Cafe(
        id = id,
        name = str("name").orEmpty(),
        description = str("description"),
        address = str("address"),
        city = str("city"),
        latitude = lat,
        longitude = lon,
        status = enum("status", CafeStatus.PENDING),
        type = enum("type", CafeType.KAFIC),
        openingHours = str("openingHours"),
        photoId = str("photoId"),
        tags = strings("tags"),
        averageRating = if (reviewCount > 0) double("ratingSum") / reviewCount else 0.0,
        reviewCount = reviewCount,
        checkInCount = int("checkInCount"),
        ownerId = str("ownerId"),
        proposedById = str("proposedById"),
        createdAtEpochMs = long("createdAt"),
        distanceMeters = if (viewerLat != null && viewerLon != null) {
            Geo.distanceMeters(viewerLat, viewerLon, lat, lon)
        } else null,
        topVisitor = topVisitor(),
        activeChallenges = activeChallenges,
        myCheckInCount = myCheckInCount,
    )
}

fun DocumentSnapshot.toReview() = Review(
    id = id,
    cafeId = str("cafeId").orEmpty(),
    authorId = str("authorId").orEmpty(),
    authorUsername = str("authorUsername").orEmpty(),
    authorDisplayName = str("authorDisplayName").orEmpty(),
    rating = int("rating"),
    comment = str("comment"),
    createdAtEpochMs = long("createdAt"),
    ownerReply = str("ownerReply"),
    ownerReplyAtEpochMs = longOrNull("ownerReplyAt"),
)

fun DocumentSnapshot.toCheckIn() = CheckIn(
    id = id,
    cafeId = str("cafeId").orEmpty(),
    cafeName = str("cafeName").orEmpty(),
    userId = str("userId").orEmpty(),
    username = str("username").orEmpty(),
    method = enum("method", CheckInMethod.HONOR),
    status = enum("status", CheckInStatus.VALID),
    pointsAwarded = int("pointsAwarded"),
    photoId = str("photoId"),
    note = str("note"),
    createdAtEpochMs = long("createdAt"),
    distanceMeters = doubleOrNull("distanceMeters"),
    flagReason = str("flagReason"),
)

fun DocumentSnapshot.toChallenge(): Challenge {
    val now = Time.now()
    val startsAt = long("startsAt")
    val endsAt = long("endsAt")
    return Challenge(
        id = id,
        scope = enum("scope", ChallengeScope.CAFE),
        cafeId = str("cafeId"),
        cafeName = str("cafeName"),
        city = str("city"),
        title = str("title").orEmpty(),
        description = str("description"),
        multiplier = double("multiplier"),
        startsAtEpochMs = startsAt,
        endsAtEpochMs = endsAt,
        createdById = str("createdById").orEmpty(),
        createdByDisplayName = str("createdByDisplayName").orEmpty(),
        active = startsAt <= now && endsAt >= now,
    )
}

fun DocumentSnapshot.toFeedItem() = FeedItem(
    id = id,
    type = enum("type", FeedEventType.CHECK_IN),
    actorId = str("actorId").orEmpty(),
    actorUsername = str("actorUsername").orEmpty(),
    actorDisplayName = str("actorDisplayName").orEmpty(),
    cafeId = str("cafeId"),
    cafeName = str("cafeName"),
    text = str("text").orEmpty(),
    photoId = str("photoId"),
    points = (get("points") as? Number)?.toInt(),
    createdAtEpochMs = long("createdAt"),
)

/**
 * The cafe's leading hunter, denormalised onto the cafe document itself.
 *
 * A map screenful is 50 cafes; ranking each one's `visitors` subcollection would
 * be 50 extra queries, so the check-in that takes the lead writes the winner here.
 */
fun DocumentSnapshot.topVisitor(): CafeConqueror? {
    val userId = str("topVisitorId") ?: return null
    return CafeConqueror(
        userId = userId,
        username = str("topVisitorUsername").orEmpty(),
        displayName = str("topVisitorDisplayName").orEmpty(),
        visits = int("topVisitorVisits"),
    )
}

/** A document in `cafes/{id}/visitors` - one row per hunter who has been here. */
fun DocumentSnapshot.toConqueror() = CafeConqueror(
    userId = id,
    username = str("username").orEmpty(),
    displayName = str("displayName").orEmpty(),
    visits = int("visits"),
)

/** A document in `users/{uid}/conquered` - the mirror of [toConqueror]. */
fun DocumentSnapshot.toConqueredCafe(isTopVisitor: Boolean) = ConqueredCafe(
    cafeId = id,
    name = str("name").orEmpty(),
    latitude = double("latitude"),
    longitude = double("longitude"),
    visits = int("visits"),
    lastVisitEpochMs = long("lastVisitAt"),
    isTopVisitor = isTopVisitor,
)

fun DocumentSnapshot.toEarnedBadge(): EarnedBadge? {
    val definition = Badges.find(id) ?: return null
    return EarnedBadge(
        code = definition.code,
        title = definition.title,
        description = definition.description,
        emoji = definition.emoji,
        earnedAtEpochMs = long("earnedAt"),
    )
}
