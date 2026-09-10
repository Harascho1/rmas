package rs.coffeeconquest.app.data

import com.google.firebase.firestore.DocumentSnapshot
import rs.coffeeconquest.app.data.firebase.AdminSource
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.data.firebase.AuthSource
import rs.coffeeconquest.app.data.firebase.CafeSource
import rs.coffeeconquest.app.data.firebase.ChallengeSource
import rs.coffeeconquest.app.data.firebase.CheckInSource
import rs.coffeeconquest.app.data.firebase.Fire
import rs.coffeeconquest.app.data.firebase.PhotoSource
import rs.coffeeconquest.app.data.firebase.SocialSource
import rs.coffeeconquest.app.data.firebase.enum
import rs.coffeeconquest.app.data.firebase.fetch
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeFilter
import rs.coffeeconquest.shared.dto.CafeStats
import rs.coffeeconquest.shared.dto.Challenge
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.CheckInResult
import rs.coffeeconquest.shared.dto.CityChampion
import rs.coffeeconquest.shared.dto.CreateCafeRequest
import rs.coffeeconquest.shared.dto.CreateChallengeRequest
import rs.coffeeconquest.shared.dto.CreateCheckInRequest
import rs.coffeeconquest.shared.dto.FeedItem
import rs.coffeeconquest.shared.dto.FollowResponse
import rs.coffeeconquest.shared.dto.LeaderboardResponse
import rs.coffeeconquest.shared.dto.QrTokenResponse
import rs.coffeeconquest.shared.dto.RegisterRequest
import rs.coffeeconquest.shared.dto.Review
import rs.coffeeconquest.shared.dto.UpdateCafeRequest
import rs.coffeeconquest.shared.dto.UpdateProfileRequest
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.dto.UserStats
import rs.coffeeconquest.shared.model.LeaderboardScope
import rs.coffeeconquest.shared.model.Role

class CoffeeRepository(
    private val auth: AuthSource,
    private val cafeSource: CafeSource,
    private val checkInSource: CheckInSource,
    private val challengeSource: ChallengeSource,
    private val social: SocialSource,
    private val photos: PhotoSource,
    private val admin: AdminSource,
) {

    private suspend fun actor(): DocumentSnapshot {
        val snapshot = Fire.user(Fire.requireUid()).fetch()
        if (!snapshot.exists()) throw AppException("Korisnik ne postoji.")
        return snapshot
    }

    // ------------------------------------------------------------------ auth

    suspend fun register(request: RegisterRequest): UserProfile = auth.register(request)

    suspend fun login(usernameOrEmail: String, password: String): UserProfile =
        auth.login(usernameOrEmail, password)

    suspend fun me(): UserProfile = auth.me()

    suspend fun updateProfile(request: UpdateProfileRequest): UserProfile =
        auth.updateProfile(request)

    fun logout() = auth.logout()

    /** Firebase Auth persists the session itself, so this survives a restart. */
    fun isSignedIn(): Boolean = auth.isSignedIn()

    // ----------------------------------------------------------------- cafes

    suspend fun cafes(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Double = 5_000.0,
        filter: CafeFilter = CafeFilter(),
        city: String? = null,
        limit: Int = 50,
    ): List<Cafe> = cafeSource.nearby(latitude, longitude, radiusMeters, filter, city, limit)

    suspend fun cafe(id: String, latitude: Double? = null, longitude: Double? = null): Cafe =
        cafeSource.byId(id, latitude, longitude)

    suspend fun myCafes(): List<Cafe> = cafeSource.managedBy(actor())

    suspend fun createCafe(request: CreateCafeRequest): Cafe = cafeSource.create(request, actor())

    suspend fun updateCafe(id: String, request: UpdateCafeRequest): Cafe =
        cafeSource.update(id, request, actor())

    suspend fun cafeStats(id: String): CafeStats = cafeSource.stats(id, actor())

    suspend fun reviews(cafeId: String): List<Review> = cafeSource.reviews(cafeId)

    suspend fun postReview(cafeId: String, rating: Int, comment: String?): Review =
        cafeSource.upsertReview(cafeId, actor(), rating, comment)

    suspend fun replyToReview(cafeId: String, reviewId: String, reply: String): Review =
        cafeSource.replyToReview(cafeId, reviewId, reply, actor())

    suspend fun cafeCheckIns(cafeId: String): List<CheckIn> =
        checkInSource.byCafe(cafeId, limit = 30)

    suspend fun assignStaff(cafeId: String, userId: String) =
        cafeSource.assignStaff(cafeId, userId, actor())

    // ------------------------------------------------------------- check-ins

    suspend fun checkIn(request: CreateCheckInRequest): CheckInResult =
        checkInSource.create(request)

    suspend fun myCheckIns(limit: Int = 30): List<CheckIn> =
        checkInSource.byUser(Fire.requireUid(), limit)

    suspend fun userCheckIns(userId: String, limit: Int = 30): List<CheckIn> =
        checkInSource.byUser(userId, limit)

    suspend fun qrToken(cafeId: String): QrTokenResponse =
        checkInSource.issueQrToken(cafeId, actor())

    // ----------------------------------------------------------------- media

    /** Returns the photo id to store on a cafe, check-in or profile. */
    suspend fun uploadPhoto(bytes: ByteArray, fileName: String = "photo.jpg"): String =
        photos.upload(bytes, fileName)

    /** The stored image bytes, or null when the photo is missing. */
    suspend fun photo(photoId: String): ByteArray? = photos.load(photoId)

    // ---------------------------------------------------------------- social

    suspend fun leaderboard(
        scope: LeaderboardScope,
        city: String? = null,
        weekly: Boolean = false,
        limit: Int = 50,
    ): LeaderboardResponse = social.leaderboard(scope, city, weekly, limit)

    suspend fun cityChampion(city: String): CityChampion = social.cityChampion(city)

    suspend fun feed(followingOnly: Boolean, limit: Int = 30): List<FeedItem> =
        social.feed(followingOnly, limit)

    suspend fun user(id: String): UserProfile = social.user(id)

    suspend fun userStats(id: String): UserStats = social.stats(id)

    suspend fun searchUsers(query: String): List<UserProfile> = social.search(query)

    suspend fun follow(userId: String, follow: Boolean): FollowResponse =
        social.follow(userId, follow)

    // ------------------------------------------------------------ challenges

    suspend fun challenges(city: String? = null, cafeId: String? = null): List<Challenge> =
        challengeSource.list(city, cafeId, onlyActive = true)

    suspend fun createChallenge(request: CreateChallengeRequest): Challenge {
        val actor = actor()
        return challengeSource.create(
            request = request,
            creator = actor,
            canManageCafe = { cafeId -> cafeSource.assertCanManage(cafeId, actor) },
            canBoostCity = { city -> assertCanBoostCity(city, actor) },
        )
    }

    suspend fun deleteChallenge(id: String) {
        val actor = actor()
        challengeSource.delete(id, actor, isAdmin = actor.enum("role", Role.HUNTER) == Role.ADMIN)
    }

    private suspend fun assertCanBoostCity(city: String, actor: DocumentSnapshot) {
        if (actor.enum("role", Role.HUNTER) == Role.ADMIN) return
        if (!social.isCityChampion(actor.id, city)) {
            throw AppException("Izazov za ceo grad moze da napravi samo gradski sampion.")
        }
    }

    // ----------------------------------------------------------------- admin

    suspend fun pendingCafes(): List<Cafe> = cafeSource.pending()

    suspend fun moderateCafe(cafeId: String, approve: Boolean, reason: String? = null): Cafe =
        cafeSource.moderate(cafeId, approve, reason)

    suspend fun flaggedCheckIns(): List<CheckIn> = checkInSource.flagged(limit = 50)

    suspend fun invalidateCheckIn(id: String, reason: String): CheckIn =
        checkInSource.moderate(id, valid = false, reason = reason)

    suspend fun approveCheckIn(id: String): CheckIn =
        checkInSource.moderate(id, valid = true, reason = "Odobreno rucno.")

    suspend fun allUsers(): List<UserProfile> = admin.allUsers()

    suspend fun banUser(userId: String, banned: Boolean, reason: String) =
        admin.setBanned(userId, banned, reason)

    suspend fun changeRole(userId: String, role: Role) = admin.setRole(userId, role)
}
