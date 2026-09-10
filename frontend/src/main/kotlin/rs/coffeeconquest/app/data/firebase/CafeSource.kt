package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.app.data.firebase.CafeSource.Companion.NO_SUCH_AUTHOR
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeConqueror
import rs.coffeeconquest.shared.dto.CafeFilter
import rs.coffeeconquest.shared.dto.CafeStats
import rs.coffeeconquest.shared.dto.CreateCafeRequest
import rs.coffeeconquest.shared.dto.DailyCount
import rs.coffeeconquest.shared.dto.Review
import rs.coffeeconquest.shared.dto.UpdateCafeRequest
import rs.coffeeconquest.shared.model.CafeStatus
import rs.coffeeconquest.shared.model.CafeType
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.Geo
import rs.coffeeconquest.shared.rules.Time
import rs.coffeeconquest.shared.rules.Validation

class CafeSource(private val challenges: ChallengeSource) {
    companion object {
        /** Ceiling on documents pulled for one map screen, however narrow the filter. */
        const val MAX_FETCH = 300L

        /** A uid no document can have, used to make an unknown author match nothing. */
        private const val NO_SUCH_AUTHOR = "\u0000-no-such-author"
    }

    suspend fun nearby(
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double,
        filter: CafeFilter,
        city: String?,
        limit: Int,
    ): List<Cafe> {
        val base = Fire.cafes().whereEqualTo("status", CafeStatus.APPROVED.name)

        // A narrowed search throws more away, so it has to start from a wider net.
        val fetchLimit = (limit * if (filter.isActive) 10L else 4L).coerceAtMost(MAX_FETCH)

        val docs = when {
            latitude != null && longitude != null -> {
                val box = Geo.boundingBox(latitude, longitude, radiusMeters)
                base.whereGreaterThanOrEqualTo("latitude", box.minLat)
                    .whereLessThanOrEqualTo("latitude", box.maxLat)
                    .limit(fetchLimit)
                    .fetch()
                    .filter { it.double("longitude") in box.minLon..box.maxLon }
            }

            !city.isNullOrBlank() -> base.whereEqualTo("cityLower", city.lowercase())
                .limit(fetchLimit)
                .fetch()

            else -> base.limit(fetchLimit).fetch()
        }

        val needle = filter.query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val authorId = resolveAuthorId(filter)
        val addedAfter = filter.addedWithinDays?.let { Time.daysAgoMs(it.toLong()) }
        val active = challenges.activeNow()

        // "How many times have I been here" for every pin at once: the viewer's
        // own conquest map is the mirror of the per-cafe visitor counters.
        val myVisits = Fire.uid?.let { uid ->
            Fire.conquered(uid).fetch().associate { it.id to it.int("visits") }
        } ?: emptyMap()

        return docs
            .asSequence()
            .filter { doc ->
                needle == null ||
                    doc.str("name").orEmpty().lowercase().contains(needle) ||
                    doc.str("address").orEmpty().lowercase().contains(needle)
            }
            .filter { doc ->
                city.isNullOrBlank() || city.equals(
                    doc.str("city"),
                    ignoreCase = true
                )
            }
            // tip
            .filter { doc ->
                filter.type == null || doc.enum(
                    "type",
                    CafeType.KAFIC
                ) == filter.type
            }
            // atributi - every requested tag must be present
            .filter { doc -> doc.strings("tags").containsAll(filter.attributes) }
            // autor
            .filter { doc -> authorId == null || doc.str("proposedById") == authorId }
            // datumi
            .filter { doc -> addedAfter == null || doc.long("createdAt") >= addedAfter }
            .map { doc ->
                doc.toCafe(
                    viewerLat = latitude,
                    viewerLon = longitude,
                    myCheckInCount = myVisits[doc.id] ?: 0,
                )
            }
            .filter { cafe -> filter.minRating == null || cafe.averageRating >= filter.minRating!! }
            .filter { cafe ->
                latitude == null || longitude == null || (cafe.distanceMeters
                    ?: 0.0) <= radiusMeters
            }
            .map { cafe -> cafe.copy(activeChallenges = active.filter { it.appliesTo(cafe) }) }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            .take(limit)
            .toList()
    }

    /**
     * Turns the author filter into a uid. "Only mine" is the signed-in user;
     * otherwise the username is resolved through the same index login uses.
     * An unknown username yields [NO_SUCH_AUTHOR], which matches nothing - the
     * honest answer, rather than silently ignoring the filter.
     */
    private suspend fun resolveAuthorId(filter: CafeFilter): String? {
        if (filter.onlyMine) return Fire.uid ?: NO_SUCH_AUTHOR
        val username = filter.authorUsername?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: return null
        return Fire.usernames().document(username).fetch().str("uid") ?: NO_SUCH_AUTHOR
    }

    suspend fun byId(cafeId: String, viewerLat: Double? = null, viewerLon: Double? = null): Cafe {
        val doc = Fire.cafe(cafeId).fetch()
        if (!doc.exists()) throw AppException("Kafic ne postoji.")
        return doc.toCafe(
            viewerLat = viewerLat,
            viewerLon = viewerLon,
            myCheckInCount = myVisits(cafeId),
            activeChallenges = challenges.activeFor(cafeId, doc.str("city")),
        )
    }

    /**
     * Hunters propose cafes (PENDING, an admin approves them); admins create them
     * approved on the spot. An owner becomes the owner of what they create.
     */
    suspend fun create(request: CreateCafeRequest, author: DocumentSnapshot): Cafe {
        Validation.cafeName(request.name)?.let { throw AppException(it) }
        if (!Geo.isValidCoordinate(request.latitude, request.longitude)) {
            throw AppException("Neispravne koordinate.")
        }

        val role = author.enum("role", Role.HUNTER)
        val name = request.name.trim()

        // Same guard the SQL version had: same name within ~50 m is the same cafe.
        val duplicate = Fire.cafes()
            .whereEqualTo("nameLower", name.lowercase())
            .fetch()
            .any { doc ->
                kotlin.math.abs(doc.double("latitude") - request.latitude) <= 0.0005 &&
                    kotlin.math.abs(doc.double("longitude") - request.longitude) <= 0.0005
            }
        if (duplicate) throw AppException("Taj kafic je vec na mapi.")

        val status = if (role == Role.ADMIN) CafeStatus.APPROVED else CafeStatus.PENDING
        val city = request.city?.trim()?.takeIf { it.isNotEmpty() }

        val doc = Fire.cafes().add(
            mapOf(
                "name" to name,
                "nameLower" to name.lowercase(),
                "description" to request.description?.trim(),
                "address" to request.address?.trim(),
                "city" to city,
                "cityLower" to city?.lowercase(),
                "latitude" to request.latitude,
                "longitude" to request.longitude,
                "status" to status.name,
                "type" to request.type.name,
                "openingHours" to request.openingHours?.trim(),
                "photoId" to request.photoId,
                "tags" to request.tags,
                "ownerId" to author.id.takeIf { role == Role.OWNER },
                "proposedById" to author.id,
                "staff" to emptyList<String>(),
                "moderationNote" to null,
                "checkInCount" to 0,
                "reviewCount" to 0,
                "ratingSum" to 0,
                "topVisitorId" to null,
                "topVisitorUsername" to null,
                "topVisitorDisplayName" to null,
                "topVisitorVisits" to 0,
                "createdAt" to Time.now(),
            ),
        ).await()

        return byId(doc.id)
    }

    suspend fun update(cafeId: String, request: UpdateCafeRequest, actor: DocumentSnapshot): Cafe {
        request.name?.let { Validation.cafeName(it)?.let { msg -> throw AppException(msg) } }
        assertCanManage(cafeId, actor)

        val updates = buildMap<String, Any?> {
            request.name?.let {
                put("name", it.trim())
                put("nameLower", it.trim().lowercase())
            }
            request.description?.let { put("description", it.trim()) }
            request.address?.let { put("address", it.trim()) }
            request.city?.let {
                put("city", it.trim())
                put("cityLower", it.trim().lowercase())
            }
            request.type?.let { put("type", it.name) }
            request.openingHours?.let { put("openingHours", it.trim()) }
            request.photoId?.let { put("photoId", it) }
            request.tags?.let { put("tags", it) }
        }
        if (updates.isNotEmpty()) Fire.cafe(cafeId).update(updates).await()
        return byId(cafeId)
    }

    suspend fun moderate(cafeId: String, approve: Boolean, note: String?): Cafe {
        val doc = Fire.cafe(cafeId).fetch()
        if (!doc.exists()) throw AppException("Kafic ne postoji.")
        Fire.cafe(cafeId).update(
            mapOf(
                "status" to (if (approve) CafeStatus.APPROVED else CafeStatus.REJECTED).name,
                "moderationNote" to note,
            ),
        ).await()
        return byId(cafeId)
    }

    suspend fun pending(): List<Cafe> =
        Fire.cafes()
            .whereEqualTo("status", CafeStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .fetch()
            .map { it.toCafe() }

    /** Cafes an owner owns, a staff member works at, or - for an admin - all of them. */
    suspend fun managedBy(actor: DocumentSnapshot): List<Cafe> {
        val docs = when (actor.enum("role", Role.HUNTER)) {
            Role.OWNER -> Fire.cafes().whereEqualTo("ownerId", actor.id).fetch()
            Role.STAFF -> Fire.cafes().whereArrayContains("staff", actor.id).fetch()
            Role.ADMIN -> Fire.cafes().fetch()
            Role.HUNTER -> emptyList()
        }
        return docs.map { it.toCafe() }
    }

    suspend fun assignStaff(cafeId: String, userId: String, actor: DocumentSnapshot) {
        assertCanManage(cafeId, actor, staffAllowed = false)

        val user = Fire.user(userId).fetch()
        if (!user.exists()) throw AppException("Korisnik ne postoji.")
        if (user.enum("role", Role.HUNTER) != Role.STAFF) {
            throw AppException("Korisnik mora imati rolu STAFF. Zamolite admina da mu je dodeli.")
        }
        Fire.cafe(cafeId).update("staff", FieldValue.arrayUnion(userId)).await()
    }

    /** Throws unless [actor] may manage this cafe. Mirrors the Firestore rules. */
    suspend fun assertCanManage(
        cafeId: String,
        actor: DocumentSnapshot,
        staffAllowed: Boolean = true
    ) {
        val role = actor.enum("role", Role.HUNTER)
        if (role == Role.ADMIN) return

        val cafe = Fire.cafe(cafeId).fetch()
        if (!cafe.exists()) throw AppException("Kafic ne postoji.")

        val isOwner = cafe.str("ownerId") == actor.id
        val isStaff = staffAllowed && role == Role.STAFF && actor.id in cafe.strings("staff")
        if (!isOwner && !isStaff) throw AppException("Ovaj kafic nije vas.")
    }

    // --------------------------------------------------------------- reviews

    suspend fun reviews(cafeId: String, limit: Int = 50): List<Review> =
        Fire.reviews(cafeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toReview() }

    /**
     * One review per user per cafe: the document id is the author's uid, so
     * posting again edits the existing review instead of adding a second one.
     */
    suspend fun upsertReview(
        cafeId: String,
        author: DocumentSnapshot,
        rating: Int,
        comment: String?
    ): Review {
        Validation.rating(rating)?.let { throw AppException(it) }
        Validation.reviewComment(comment)?.let { throw AppException(it) }

        val cafeRef = Fire.cafe(cafeId)
        if (!cafeRef.fetch().exists()) throw AppException("Kafic ne postoji.")

        val reviewRef = Fire.reviews(cafeId).document(author.id)
        val existing = reviewRef.fetch()
        val previousRating = if (existing.exists()) existing.int("rating") else null

        val payload = mapOf(
            "cafeId" to cafeId,
            "authorId" to author.id,
            "authorUsername" to author.str("username"),
            "authorDisplayName" to author.str("displayName"),
            "rating" to rating,
            "comment" to comment?.trim(),
            "createdAt" to Time.now(),
            "ownerReply" to existing.str("ownerReply"),
            "ownerReplyAt" to existing.longOrNull("ownerReplyAt"),
        )

        // The cafe carries its own rating counters, so reading a cafe never has
        // to page through its reviews.
        Fire.db.batch().apply {
            set(reviewRef, payload)
            if (previousRating == null) {
                update(
                    cafeRef,
                    mapOf(
                        "reviewCount" to FieldValue.increment(1),
                        "ratingSum" to FieldValue.increment(rating.toLong()),
                    ),
                )
                update(Fire.user(author.id), "reviewCount", FieldValue.increment(1))
            } else {
                update(
                    cafeRef,
                    "ratingSum",
                    FieldValue.increment((rating - previousRating).toLong())
                )
            }
        }.commit().await()

        return reviewRef.fetch().toReview()
    }

    suspend fun replyToReview(
        cafeId: String,
        reviewId: String,
        reply: String,
        actor: DocumentSnapshot
    ): Review {
        assertCanManage(cafeId, actor, staffAllowed = false)

        val ref = Fire.reviews(cafeId).document(reviewId)
        if (!ref.fetch().exists()) throw AppException("Recenzija ne postoji.")

        ref.update(
            mapOf(
                "ownerReply" to reply.trim().take(500),
                "ownerReplyAt" to Time.now(),
            ),
        ).await()
        return ref.fetch().toReview()
    }

    // ----------------------------------------------------------- owner stats

    suspend fun stats(cafeId: String, actor: DocumentSnapshot): CafeStats {
        assertCanManage(cafeId, actor)

        val cafe = Fire.cafe(cafeId).fetch()
        if (!cafe.exists()) throw AppException("Kafic ne postoji.")

        // One window of check-ins covers the 7-day, 30-day and per-day figures.
        val since = Time.daysAgoMs(30)
        val recent = Fire.checkIns()
            .whereEqualTo("cafeId", cafeId)
            .whereGreaterThanOrEqualTo("createdAt", since)
            .fetch()
            .filter { it.enum("status", CheckInStatus.VALID) != CheckInStatus.INVALIDATED }

        val last7From = Time.daysAgoMs(7)
        val visitors = Fire.visitors(cafeId).fetch()
        val reviews = Fire.reviews(cafeId).fetch()

        val today = Time.today()
        val daily = (13 downTo 0).map { back ->
            val day = today - back
            val from = Time.startOfDayMs(day)
            val to = Time.startOfDayMs(day + 1)
            DailyCount(from, recent.count { it.long("createdAt") in from until to })
        }

        val reviewCount = reviews.size
        return CafeStats(
            cafeId = cafeId,
            cafeName = cafe.str("name").orEmpty(),
            totalCheckIns = cafe.int("checkInCount"),
            checkInsLast7Days = recent.count { it.long("createdAt") >= last7From },
            checkInsLast30Days = recent.size,
            uniqueVisitors = visitors.size,
            averageRating = if (reviewCount > 0) reviews.sumOf { it.int("rating") }
                .toDouble() / reviewCount else 0.0,
            reviewCount = reviewCount,
            unansweredReviews = reviews.count { it.str("ownerReply") == null },
            dailyCheckIns = daily,
            topVisitors = topVisitors(cafeId, 5),
        )
    }

    // -------------------------------------------------------------- visitors

    /**
     * The owner dashboard's top five, ranked from the per-cafe visitor counters.
     * A single cafe can afford the query; the map reads the denormalised leader
     * off the cafe document instead.
     */
    suspend fun topVisitors(cafeId: String, limit: Int): List<CafeConqueror> =
        Fire.visitors(cafeId)
            .orderBy("visits", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toConqueror() }

    private suspend fun myVisits(cafeId: String): Int {
        val uid = Fire.uid ?: return 0
        return Fire.visitors(cafeId).document(uid).fetch().int("visits")
    }
}
