package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.CityChampion
import rs.coffeeconquest.shared.dto.ConqueredCafe
import rs.coffeeconquest.shared.dto.EarnedBadge
import rs.coffeeconquest.shared.dto.FeedItem
import rs.coffeeconquest.shared.dto.FollowResponse
import rs.coffeeconquest.shared.dto.LeaderboardEntry
import rs.coffeeconquest.shared.dto.LeaderboardResponse
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.dto.UserStats
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.FeedEventType
import rs.coffeeconquest.shared.model.LeaderboardScope
import rs.coffeeconquest.shared.rules.ScoreRules
import rs.coffeeconquest.shared.rules.Time

class SocialSource {
    companion object {
        const val WEEKLY_SCAN_LIMIT = 500L
    }

    suspend fun leaderboard(
        scope: LeaderboardScope,
        city: String?,
        weekly: Boolean,
        limit: Int,
    ): LeaderboardResponse {
        val viewerId = Fire.uid
        val since = if (weekly) Time.startOfWeekMs() else null

        val entries = if (since == null) {
            allTime(scope, city, viewerId, limit)
        } else {
            windowed(scope, city, viewerId, limit, since)
        }

        val me = entries.firstOrNull { it.isMe } ?: viewerId?.let { myRow(it, since) }

        return LeaderboardResponse(
            scope = scope,
            city = city,
            sinceEpochMs = since,
            entries = entries,
            me = me,
        )
    }

    private suspend fun allTime(
        scope: LeaderboardScope,
        city: String?,
        viewerId: String?,
        limit: Int,
    ): List<LeaderboardEntry> {
        val docs = when (scope) {
            LeaderboardScope.FRIENDS -> {
                val ids = friendIds(viewerId)
                if (ids.isEmpty()) return emptyList()
                usersByIds(ids).sortedByDescending { it.int("points") }
            }

            LeaderboardScope.CITY -> {
                if (city.isNullOrBlank()) return emptyList()
                Fire.users()
                    .whereEqualTo("cityLower", city.lowercase())
                    .whereEqualTo("isBanned", false)
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .fetch()
            }

            LeaderboardScope.GLOBAL -> Fire.users()
                .whereEqualTo("isBanned", false)
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .fetch()
        }

        return docs.filter { !it.bool("isBanned") }
            .take(limit)
            .mapIndexed { index, doc -> doc.toEntry(index + 1, doc.int("points"), viewerId) }
    }

    private suspend fun windowed(
        scope: LeaderboardScope,
        city: String?,
        viewerId: String?,
        limit: Int,
        since: Long,
    ): List<LeaderboardEntry> {
        val checkIns = Fire.checkIns()
            .whereGreaterThanOrEqualTo("createdAt", since)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(WEEKLY_SCAN_LIMIT)
            .fetch()

        val pointsByUser = mutableMapOf<String, Int>()
        for (doc in checkIns) {
            if (doc.enum("status", CheckInStatus.VALID) != CheckInStatus.VALID) continue
            val userId = doc.str("userId") ?: continue
            pointsByUser[userId] = (pointsByUser[userId] ?: 0) + doc.int("pointsAwarded")
        }
        if (pointsByUser.isEmpty()) return emptyList()

        val eligible = when (scope) {
            LeaderboardScope.FRIENDS -> friendIds(viewerId).toSet()
            else -> null
        }

        val ranked = pointsByUser.entries
            .filter { eligible == null || it.key in eligible }
            .sortedByDescending { it.value }
            .take(limit * 3)

        val profiles = usersByIds(ranked.map { it.key }).associateBy { it.id }

        return ranked.mapNotNull { (userId, points) ->
            val doc = profiles[userId] ?: return@mapNotNull null
            if (doc.bool("isBanned")) return@mapNotNull null
            if (scope == LeaderboardScope.CITY && !city.isNullOrBlank() &&
                !city.equals(doc.str("city"), ignoreCase = true)
            ) {
                return@mapNotNull null
            }
            doc to points
        }
            .take(limit)
            .mapIndexed { index, (doc, points) -> doc.toEntry(index + 1, points, viewerId) }
    }

    private suspend fun myRow(viewerId: String, since: Long?): LeaderboardEntry? {
        val doc = Fire.user(viewerId).fetch()
        if (!doc.exists()) return null

        val points = if (since == null) {
            doc.int("points")
        } else {
            Fire.checkIns()
                .whereEqualTo("userId", viewerId)
                .whereGreaterThanOrEqualTo("createdAt", since)
                .fetch()
                .filter { it.enum("status", CheckInStatus.VALID) == CheckInStatus.VALID }
                .sumOf { it.int("pointsAwarded") }
        }

        val ahead = Fire.users()
            .whereEqualTo("isBanned", false)
            .whereGreaterThan("points", points)
            .count()
            .get(com.google.firebase.firestore.AggregateSource.SERVER)
            .await()
            .count
            .toInt()

        return doc.toEntry(ahead + 1, points, viewerId)
    }

    suspend fun cityChampion(city: String): CityChampion {
        val weekStart = Time.startOfWeekMs()
        val board = windowed(LeaderboardScope.CITY, city, Fire.uid, 1, weekStart)
        return CityChampion(city = city, user = board.firstOrNull(), weekStartEpochMs = weekStart)
    }

    suspend fun isCityChampion(userId: String, city: String?): Boolean {
        if (city.isNullOrBlank()) return false
        return runCatching { cityChampion(city).user?.userId == userId }.getOrDefault(false)
    }

    private fun DocumentSnapshot.toEntry(rank: Int, points: Int, viewerId: String?) =
        LeaderboardEntry(
            rank = rank,
            userId = id,
            username = str("username").orEmpty(),
            displayName = str("displayName").orEmpty(),
            avatarPhotoId = str("avatarPhotoId"),
            points = points,
            level = ScoreRules.levelFor(points),
            checkInCount = int("checkInCount"),
            city = str("city"),
            isMe = id == viewerId,
        )

    suspend fun feed(followingOnly: Boolean, limit: Int): List<FeedItem> {
        if (!followingOnly) {
            return Fire.feed()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .fetch()
                .map { it.toFeedItem() }
        }

        val actorIds = friendIds(Fire.uid)
        if (actorIds.isEmpty()) return emptyList()

        return actorIds.chunked(WHERE_IN_LIMIT)
            .flatMap { chunk ->
                Fire.feed()
                    .whereIn("actorId", chunk)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .fetch()
            }
            .map { it.toFeedItem() }
            .sortedByDescending { it.createdAtEpochMs }
            .take(limit)
    }

    suspend fun record(
        type: FeedEventType,
        actor: DocumentSnapshot,
        text: String,
        cafeId: String? = null,
        cafeName: String? = null,
        photoId: String? = null,
        points: Int? = null,
    ) {
        Fire.feed().add(
            mapOf(
                "type" to type.name,
                "actorId" to actor.id,
                "actorUsername" to actor.str("username"),
                "actorDisplayName" to actor.str("displayName"),
                "cafeId" to cafeId,
                "cafeName" to cafeName,
                "text" to text.take(255),
                "photoId" to photoId,
                "points" to points,
                "createdAt" to Time.now(),
            ),
        ).await()
    }

    suspend fun user(userId: String): UserProfile {
        val doc = Fire.user(userId).fetch()
        if (!doc.exists()) throw AppException("Korisnik ne postoji.")
        val viewerId = Fire.uid
        val followed = viewerId != null && viewerId != userId &&
            Fire.follows().document(Fire.followId(viewerId, userId)).fetch().exists()
        val base = doc.toUserProfile(isFollowedByMe = followed)
        return base.copy(isCityChampion = isCityChampion(userId, base.city))
    }

    suspend fun stats(userId: String): UserStats =
        UserStats(profile = user(userId), badges = badges(userId), conquered = conquered(userId))

    suspend fun badges(userId: String): List<EarnedBadge> =
        Fire.badges(userId)
            .orderBy("earnedAt", Query.Direction.DESCENDING)
            .fetch()
            .mapNotNull { it.toEarnedBadge() }

    suspend fun conquered(userId: String): List<ConqueredCafe> =
        Fire.conquered(userId)
            .orderBy("lastVisitAt", Query.Direction.DESCENDING)
            .fetch()
            .map { it.toConqueredCafe(isTopVisitor = it.bool("isTopVisitor")) }

    suspend fun search(query: String, limit: Int = 20): List<UserProfile> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        return Fire.users()
            .orderBy("usernameLower")
            .startAt(needle)
            .endAt(needle + "\uf8ff")
            .limit(limit.toLong())
            .fetch()
            .map { it.toUserProfile() }
    }

    suspend fun follow(followeeId: String, follow: Boolean): FollowResponse {
        val followerId = Fire.requireUid()
        if (followerId == followeeId) throw AppException("Ne mozete pratiti sami sebe.")

        val followDoc = Fire.follows().document(Fire.followId(followerId, followeeId))
        val already = followDoc.fetch().exists()

        if (follow && !already) {
            Fire.db.batch().apply {
                set(
                    followDoc,
                    mapOf(
                        "followerId" to followerId,
                        "followeeId" to followeeId,
                        "createdAt" to Time.now(),
                    ),
                )
                update(Fire.user(followeeId), "followerCount", FieldValue.increment(1))
                update(Fire.user(followerId), "followingCount", FieldValue.increment(1))
            }.commit().await()
        } else if (!follow && already) {
            Fire.db.batch().apply {
                delete(followDoc)
                update(Fire.user(followeeId), "followerCount", FieldValue.increment(-1))
                update(Fire.user(followerId), "followingCount", FieldValue.increment(-1))
            }.commit().await()
        }

        val followerCount = Fire.user(followeeId).fetch().int("followerCount")
        return FollowResponse(following = follow, followerCount = followerCount)
    }

    suspend fun followingIds(userId: String): List<String> =
        Fire.follows()
            .whereEqualTo("followerId", userId)
            .fetch()
            .mapNotNull { it.str("followeeId") }

    private suspend fun friendIds(viewerId: String?): List<String> {
        if (viewerId == null) return emptyList()
        return followingIds(viewerId) + viewerId
    }

    private suspend fun usersByIds(ids: List<String>): List<DocumentSnapshot> =
        ids.distinct().chunked(WHERE_IN_LIMIT).flatMap { chunk ->
            Fire.users().whereIn(FieldPath.documentId(), chunk).fetch()
        }
}
