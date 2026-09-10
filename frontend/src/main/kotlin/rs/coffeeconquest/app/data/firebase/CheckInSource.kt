package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.CheckInResult
import rs.coffeeconquest.shared.dto.CreateCheckInRequest
import rs.coffeeconquest.shared.dto.QrTokenResponse
import rs.coffeeconquest.shared.model.CafeStatus
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.FeedEventType
import rs.coffeeconquest.shared.rules.Geo
import rs.coffeeconquest.shared.rules.ScoreRules
import rs.coffeeconquest.shared.rules.Time
import java.security.SecureRandom
import java.util.Base64

class CheckInSource(
    private val cafes: CafeSource,
    private val challenges: ChallengeSource,
    private val badges: BadgeSource,
    private val social: SocialSource,
) {
    private val random = SecureRandom()

    suspend fun create(request: CreateCheckInRequest): CheckInResult {
        val uid = Fire.requireUid()
        val user = Fire.user(uid).fetch()
        if (!user.exists()) throw AppException("Korisnik ne postoji.")
        if (user.bool("isBanned")) throw AppException("Nalog je blokiran, check-in nije moguc.")

        val cafe = Fire.cafe(request.cafeId).fetch()
        if (!cafe.exists()) throw AppException("Kafic ne postoji.")
        if (cafe.enum("status", CafeStatus.PENDING) != CafeStatus.APPROVED) {
            throw AppException("Kafic jos nije odobren, pa check-in nije moguc.")
        }

        val now = Time.now()
        val cafeLat = cafe.double("latitude")
        val cafeLon = cafe.double("longitude")

        // The per-cafe visitor row answers both the cooldown and "have I been here".
        val visitorRef = Fire.visitors(request.cafeId).document(uid)
        val visitor = visitorRef.fetch()
        assertCooldown(visitor, now)

        val overLimit = countToday(uid, now) >= ScoreRules.MAX_CHECK_INS_PER_DAY
        val distance = verifyProximity(request, cafeLat, cafeLon)
        if (request.method == CheckInMethod.QR) consumeQrToken(request.qrToken, request.cafeId, now)

        val (multiplier, challenge) = challenges.multiplierFor(request.cafeId, cafe.str("city"))
        val firstVisit = !visitor.exists()

        val streak = nextStreak(user, now)
        val breakdown = ScoreRules.checkInScore(
            method = request.method,
            withPhoto = request.photoId != null,
            firstVisitToCafe = firstVisit,
            streakDays = streak,
            challengeMultiplier = multiplier,
        )

        val flagReason = flagReason(request, overLimit, uid, cafeLat, cafeLon, now)
        val status = if (flagReason == null) CheckInStatus.VALID else CheckInStatus.FLAGGED
        // A flagged check-in still shows up, but it earns nothing until a moderator clears it.
        val awarded = if (status == CheckInStatus.VALID) breakdown.total else 0

        val checkInRef = Fire.checkIns().document()
        val today = Time.epochDay(now)
        val longest = maxOf(streak, user.int("longestStreakDays"))

        Fire.db.batch().apply {
            set(
                checkInRef,
                mapOf(
                    "userId" to uid,
                    "username" to user.str("username"),
                    "cafeId" to request.cafeId,
                    "cafeName" to cafe.str("name"),
                    "method" to request.method.name,
                    "status" to status.name,
                    "pointsAwarded" to awarded,
                    "photoId" to request.photoId,
                    "note" to request.note?.trim()?.take(255),
                    "latitude" to request.latitude,
                    "longitude" to request.longitude,
                    "distanceMeters" to distance,
                    "flagReason" to flagReason,
                    "createdAt" to now,
                ),
            )

            update(
                Fire.user(uid),
                buildMap<String, Any?> {
                    put("points", FieldValue.increment(awarded.toLong()))
                    put("checkInCount", FieldValue.increment(1))
                    put("currentStreakDays", streak)
                    put("longestStreakDays", longest)
                    put("lastCheckInDay", today)
                    if (firstVisit) put("conqueredCafes", FieldValue.increment(1))
                },
            )

            // Two mirrored counters: one per cafe (who conquered it) and one per
            // user (my conquest map). Both are single-document reads later.
            set(
                visitorRef,
                mapOf(
                    "username" to user.str("username"),
                    "displayName" to user.str("displayName"),
                    "visits" to FieldValue.increment(1),
                    "lastVisitAt" to now,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            set(
                Fire.conquered(uid).document(request.cafeId),
                mapOf(
                    "name" to cafe.str("name"),
                    "latitude" to cafeLat,
                    "longitude" to cafeLon,
                    "visits" to FieldValue.increment(1),
                    "lastVisitAt" to now,
                    "isTopVisitor" to (visitor.int("visits") + 1 >= cafe.int("topVisitorVisits")),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            // The cafe carries its leading hunter so a screenful of pins needs no
            // extra queries. Visit counts only ever grow, so whoever passes the
            // stored figure is the new "osvajac".
            val myVisits = visitor.int("visits") + 1
            val cafeUpdates = buildMap<String, Any?> {
                put("checkInCount", FieldValue.increment(1))
                if (myVisits > cafe.int("topVisitorVisits")) {
                    put("topVisitorId", uid)
                    put("topVisitorUsername", user.str("username"))
                    put("topVisitorDisplayName", user.str("displayName"))
                    put("topVisitorVisits", myVisits)
                }
            }
            update(Fire.cafe(request.cafeId), cafeUpdates)
        }.commit().await()

        if (request.rating != null) {
            cafes.upsertReview(request.cafeId, user, request.rating!!, request.comment)
        }

        val updated = Fire.user(uid).fetch()

        val earned = if (status == CheckInStatus.VALID) {
            badges.evaluateAfterCheckIn(
                userId = uid,
                checkInHour = Time.hourOfDay(now),
                distinctCafes = updated.int("conqueredCafes"),
                streakDays = streak,
                reviewCount = updated.int("reviewCount"),
            )
        } else emptyList()

        if (status == CheckInStatus.VALID) {
            val bonus = challenge?.let { " (${it.title}, x${it.multiplier})" } ?: ""
            social.record(
                type = FeedEventType.CHECK_IN,
                actor = updated,
                text = "${updated.str("displayName")} je osvojio ${cafe.str("name")}$bonus",
                cafeId = request.cafeId,
                cafeName = cafe.str("name"),
                photoId = request.photoId,
                points = awarded,
            )
            earned.forEach { badge ->
                social.record(
                    type = FeedEventType.BADGE,
                    actor = updated,
                    text = "${updated.str("displayName")} je osvojio bedz ${badge.emoji} ${badge.title}",
                )
            }
        }

        return CheckInResult(
            checkIn = checkInRef.fetch().toCheckIn(),
            breakdown = breakdown,
            newTotalPoints = updated.int("points"),
            level = ScoreRules.levelFor(updated.int("points")),
            streakDays = streak,
            newBadges = earned,
        )
    }

    // ------------------------------------------------------------ anti-cheat

    private fun assertCooldown(visitor: DocumentSnapshot, now: Long) {
        if (!visitor.exists()) return
        val last = visitor.long("lastVisitAt")
        val cooldownMs = ScoreRules.COOLDOWN_HOURS_PER_CAFE * 60L * 60 * 1000
        if (now - last < cooldownMs) {
            val minutes = ((last + cooldownMs - now) / 60_000).coerceAtLeast(1)
            throw AppException("U ovom kaficu mozete ponovo za $minutes min.")
        }
    }

    private suspend fun countToday(uid: String, now: Long): Int =
        Fire.checkIns()
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("createdAt", Time.startOfDayMs(Time.epochDay(now)))
            .fetch()
            .size

    private fun verifyProximity(
        request: CreateCheckInRequest,
        cafeLat: Double,
        cafeLon: Double
    ): Double? {
        val lat = request.latitude
        val lon = request.longitude
        if (lat == null || lon == null) {
            if (request.method == CheckInMethod.GPS) throw AppException("GPS check-in zahteva lokaciju.")
            return null
        }
        if (!Geo.isValidCoordinate(lat, lon)) throw AppException("Neispravne koordinate.")

        val distance = Geo.distanceMeters(lat, lon, cafeLat, cafeLon)
        if (request.method == CheckInMethod.GPS && distance > Geo.MAX_CHECKIN_DISTANCE_M) {
            throw AppException(
                "Predaleko ste od kafica (${distance.toInt()} m). " +
                    "Priblizite se na ${Geo.MAX_CHECKIN_DISTANCE_M.toInt()} m.",
            )
        }
        return distance
    }

    private suspend fun flagReason(
        request: CreateCheckInRequest,
        overDailyLimit: Boolean,
        uid: String,
        cafeLat: Double,
        cafeLon: Double,
        now: Long,
    ): String? {
        if (overDailyLimit) return "Preko ${ScoreRules.MAX_CHECK_INS_PER_DAY} check-inova danas."
        if (request.method == CheckInMethod.HONOR) return "Check-in bez dokaza (honor)."

        val previous = Fire.checkIns()
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .fetch()
            .firstOrNull { it.doubleOrNull("latitude") != null && it.doubleOrNull("longitude") != null }
            ?: return null

        val prevLat = previous.doubleOrNull("latitude") ?: return null
        val prevLon = previous.doubleOrNull("longitude") ?: return null
        val hours = (now - previous.long("createdAt")) / 3_600_000.0
        if (hours <= 0) return null

        val km = Geo.distanceMeters(prevLat, prevLon, cafeLat, cafeLon) / 1000.0
        val speed = km / hours
        return if (speed > Geo.MAX_PLAUSIBLE_SPEED_KMH) {
            "Nemoguca brzina izmedju check-inova (${speed.toInt()} km/h)."
        } else null
    }

    private fun nextStreak(user: DocumentSnapshot, now: Long): Int {
        val today = Time.epochDay(now)
        val last = user.longOrNull("lastCheckInDay")
        val current = user.int("currentStreakDays")
        return when (last) {
            null -> 1
            today -> current.coerceAtLeast(1)
            today - 1 -> current + 1
            else -> 1
        }
    }

    // ------------------------------------------------------------- QR tokens

    suspend fun issueQrToken(cafeId: String, actor: DocumentSnapshot): QrTokenResponse {
        cafes.assertCanManage(cafeId, actor)

        val cafe = Fire.cafe(cafeId).fetch()
        if (!cafe.exists()) throw AppException("Kafic ne postoji.")

        val bytes = ByteArray(18).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val now = Time.now()
        val expiresAt = now + QR_TOKEN_SECONDS * 1000

        Fire.qrTokens(cafeId).document(token).set(
            mapOf(
                "cafeId" to cafeId,
                "issuedById" to actor.id,
                "expiresAt" to expiresAt,
                "createdAt" to now,
            ),
        ).await()

        return QrTokenResponse(
            cafeId = cafeId,
            cafeName = cafe.str("name").orEmpty(),
            token = token,
            expiresAtEpochMs = expiresAt,
        )
    }

    private suspend fun consumeQrToken(token: String?, cafeId: String, now: Long) {
        if (token.isNullOrBlank()) throw AppException("Nedostaje QR kod.")
        val doc = Fire.qrTokens(cafeId).document(token).fetch()
        if (!doc.exists()) throw AppException("QR kod ne vazi za ovaj kafic.")
        if (doc.long("expiresAt") < now) {
            throw AppException("QR kod je istekao, zamolite osoblje za novi.")
        }
    }

    // --------------------------------------------------------------- queries

    suspend fun byUser(userId: String, limit: Int): List<CheckIn> =
        Fire.checkIns()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toCheckIn() }

    suspend fun byCafe(cafeId: String, limit: Int): List<CheckIn> =
        Fire.checkIns()
            .whereEqualTo("cafeId", cafeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toCheckIn() }

    suspend fun flagged(limit: Int): List<CheckIn> =
        Fire.checkIns()
            .whereEqualTo("status", CheckInStatus.FLAGGED.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toCheckIn() }

    suspend fun moderate(checkInId: String, valid: Boolean, reason: String): CheckIn {
        val ref = Fire.checkIns().document(checkInId)
        val doc = ref.fetch()
        if (!doc.exists()) throw AppException("Check-in ne postoji.")

        val userId = doc.str("userId") ?: throw AppException("Check-in nema korisnika.")
        val current = doc.int("pointsAwarded")
        val status = doc.enum("status", CheckInStatus.VALID)

        if (valid) {
            if (status == CheckInStatus.VALID) return doc.toCheckIn()
            val restored = ScoreRules.checkInScore(
                method = doc.enum("method", CheckInMethod.HONOR),
                withPhoto = doc.str("photoId") != null,
                firstVisitToCafe = false,
                streakDays = 0,
            ).total

            Fire.db.batch().apply {
                update(
                    ref,
                    mapOf(
                        "status" to CheckInStatus.VALID.name,
                        "pointsAwarded" to restored,
                        "flagReason" to reason,
                    ),
                )
                update(
                    Fire.user(userId),
                    "points",
                    FieldValue.increment((restored - current).toLong())
                )
            }.commit().await()
        } else {
            Fire.db.batch().apply {
                update(
                    ref,
                    mapOf(
                        "status" to CheckInStatus.INVALIDATED.name,
                        "pointsAwarded" to 0,
                        "flagReason" to reason,
                    ),
                )
                update(
                    Fire.user(userId),
                    mapOf(
                        "points" to FieldValue.increment(-current.toLong()),
                        "checkInCount" to FieldValue.increment(-1),
                    ),
                )
            }.commit().await()
        }

        return ref.fetch().toCheckIn()
    }

    companion object {
        const val QR_TOKEN_SECONDS = 300L
    }
}
