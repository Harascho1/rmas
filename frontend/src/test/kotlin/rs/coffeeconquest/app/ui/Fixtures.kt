package rs.coffeeconquest.app.ui

import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.Challenge
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.EarnedBadge
import rs.coffeeconquest.shared.dto.FeedItem
import rs.coffeeconquest.shared.dto.Review
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.dto.UserStats
import rs.coffeeconquest.shared.model.CafeStatus
import rs.coffeeconquest.shared.model.ChallengeScope
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.FeedEventType
import rs.coffeeconquest.shared.model.Role

fun cafe(
    id: String = "c1",
    name: String = "Kafeterija",
    latitude: Double = 43.3200,
    longitude: Double = 21.9000,
    myCheckInCount: Int = 0,
    activeChallenges: List<Challenge> = emptyList(),
) = Cafe(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    status = CafeStatus.APPROVED,
    myCheckInCount = myCheckInCount,
    activeChallenges = activeChallenges,
)

fun profile(
    id: String = "u1",
    username: String = "luka",
    role: Role = Role.HUNTER,
    city: String? = null,
    isFollowedByMe: Boolean = false,
) = UserProfile(
    id = id,
    username = username,
    displayName = username.replaceFirstChar { it.uppercase() },
    role = role,
    city = city,
    isFollowedByMe = isFollowedByMe,
)

fun stats(
    profile: UserProfile = profile(),
    badges: List<EarnedBadge> = emptyList(),
) = UserStats(profile = profile, badges = badges, conquered = emptyList())

fun badge(code: String, earnedAtEpochMs: Long = 1_000) = EarnedBadge(
    code = code,
    title = code,
    description = code,
    emoji = "*",
    earnedAtEpochMs = earnedAtEpochMs,
)

fun checkIn(
    id: String = "ci1",
    cafeId: String = "c1",
    method: CheckInMethod = CheckInMethod.GPS,
    status: CheckInStatus = CheckInStatus.VALID,
    points: Int = 10,
) = CheckIn(
    id = id,
    cafeId = cafeId,
    cafeName = "Kafeterija",
    userId = "u1",
    username = "luka",
    method = method,
    status = status,
    pointsAwarded = points,
    createdAtEpochMs = 1_000,
)

fun review(id: String = "r1", rating: Int = 5, ownerReply: String? = null) = Review(
    id = id,
    cafeId = "c1",
    authorId = "u1",
    authorUsername = "luka",
    authorDisplayName = "Luka",
    rating = rating,
    createdAtEpochMs = 1_000,
    ownerReply = ownerReply,
)

fun challenge(id: String = "ch1", title: String = "Dupli poeni", multiplier: Double = 2.0) =
    Challenge(
        id = id,
        scope = ChallengeScope.CAFE,
        cafeId = "c1",
        title = title,
        multiplier = multiplier,
        startsAtEpochMs = 0,
        endsAtEpochMs = Long.MAX_VALUE,
        createdById = "u2",
        createdByDisplayName = "Vlasnik",
        active = true,
    )

fun feedItem(id: String = "f1", type: FeedEventType = FeedEventType.CHECK_IN) = FeedItem(
    id = id,
    type = type,
    actorId = "u1",
    actorUsername = "luka",
    actorDisplayName = "Luka",
    text = "Luka je bio u Kafeteriji",
    createdAtEpochMs = 1_000,
)
