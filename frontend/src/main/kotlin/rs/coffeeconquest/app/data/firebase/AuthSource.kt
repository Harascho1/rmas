package rs.coffeeconquest.app.data.firebase

import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.RegisterRequest
import rs.coffeeconquest.shared.dto.UpdateProfileRequest
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.Time
import rs.coffeeconquest.shared.rules.Validation

/**
 * Accounts. Firebase Auth owns the credentials and the session; the `users`
 * collection owns everything the game needs (role, points, streak).
 *
 * Firebase Auth only knows email + password, but the app has always let people
 * sign in with a username, so `usernames/{usernameLower}` doubles as a uniqueness
 * index and a username -> email lookup.
 */
class AuthSource(private val social: SocialSource) {

    suspend fun register(request: RegisterRequest): UserProfile {
        Validation.username(request.username)?.let { throw AppException(it) }
        Validation.email(request.email)?.let { throw AppException(it) }
        Validation.password(request.password)?.let { throw AppException(it) }
        Validation.displayName(request.displayName)?.let { throw AppException(it) }

        // STAFF and ADMIN are never self-assigned: an admin grants them later.
        if (request.role !in setOf(Role.HUNTER, Role.OWNER)) {
            throw AppException("Moguce je registrovati se samo kao HUNTER ili OWNER.")
        }

        val username = request.username.trim()
        val usernameLower = username.lowercase()
        val email = request.email.trim().lowercase()

        if (Fire.usernames().document(usernameLower).fetch().exists()) {
            throw AppException("Korisnicko ime je vec zauzeto.")
        }

        val credential = try {
            Fire.auth.createUserWithEmailAndPassword(email, request.password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AppException("Email je vec zauzet.", e)
        }
        val uid = credential.user?.uid ?: throw AppException("Registracija nije uspela.")

        val now = Time.now()
        val profile = mapOf(
            "username" to username,
            "usernameLower" to usernameLower,
            "email" to email,
            "displayName" to request.displayName.trim(),
            "role" to request.role.name,
            "city" to request.city?.trim()?.takeIf { it.isNotEmpty() },
            "cityLower" to request.city?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
            "avatarPhotoId" to null,
            "points" to 0,
            "checkInCount" to 0,
            "reviewCount" to 0,
            "conqueredCafes" to 0,
            "currentStreakDays" to 0,
            "longestStreakDays" to 0,
            "lastCheckInDay" to null,
            "followerCount" to 0,
            "followingCount" to 0,
            "isBanned" to false,
            "banReason" to null,
            "createdAt" to now,
        )

        // Both writes land together, so a username can never be reserved without a profile.
        Fire.db.batch().apply {
            set(Fire.user(uid), profile)
            set(Fire.usernames().document(usernameLower), mapOf("uid" to uid, "email" to email))
        }.commit().await()

        return me()
    }

    suspend fun login(usernameOrEmail: String, password: String): UserProfile {
        val needle = usernameOrEmail.trim()
        val email = if (needle.contains("@")) {
            needle.lowercase()
        } else {
            Fire.usernames().document(needle.lowercase()).fetch().str("email")
                ?: throw AppException("Pogresno korisnicko ime ili lozinka.")
        }

        Fire.auth.signInWithEmailAndPassword(email, password).await()

        val profile = me()
        if (profile.isBanned) {
            Fire.auth.signOut()
            throw AppException("Nalog je blokiran: ${profile.banReason ?: "Nije naveden razlog."}")
        }
        return profile
    }

    suspend fun me(): UserProfile {
        val uid = Fire.requireUid()
        val snapshot = Fire.user(uid).fetch()
        if (!snapshot.exists()) throw AppException("Korisnik ne postoji.")
        val base = snapshot.toUserProfile()
        return base.copy(isCityChampion = social.isCityChampion(uid, base.city))
    }

    suspend fun updateProfile(request: UpdateProfileRequest): UserProfile {
        val uid = Fire.requireUid()
        request.displayName?.let { Validation.displayName(it)?.let { msg -> throw AppException(msg) } }

        val updates = buildMap<String, Any?> {
            request.displayName?.let { put("displayName", it.trim()) }
            request.city?.let {
                val city = it.trim().takeIf { c -> c.isNotEmpty() }
                put("city", city)
                put("cityLower", city?.lowercase())
            }
            request.avatarPhotoId?.let { put("avatarPhotoId", it) }
        }
        if (updates.isNotEmpty()) Fire.user(uid).update(updates).await()
        return me()
    }

    fun logout() {
        Fire.auth.signOut()
    }

    fun isSignedIn(): Boolean = Fire.uid != null
}
