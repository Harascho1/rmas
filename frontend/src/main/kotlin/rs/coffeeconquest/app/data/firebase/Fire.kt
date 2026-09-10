package rs.coffeeconquest.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firebase handles, collection names and the small helpers every data source needs.
 *
 * Firestore has no joins, so the shape here is deliberately denormalised: a
 * check-in carries the cafe name, a feed entry carries the actor's display name,
 * and a cafe carries its own counters. Every read a screen makes should be one
 * query, not a fan-out.
 */
object Fire {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** The signed-in user's uid, which is also their document id in `users`. */
    val uid: String? get() = auth.currentUser?.uid

    fun requireUid(): String = uid ?: throw AppException("Niste prijavljeni.")

    // ------------------------------------------------------------ collections

    fun users() = db.collection("users")
    fun user(uid: String) = users().document(uid)

    /** `usernames/{usernameLower}` keeps usernames unique and maps them to an email for login. */
    fun usernames() = db.collection("usernames")

    fun cafes() = db.collection("cafes")
    fun cafe(id: String) = cafes().document(id)

    /** One review per user per cafe - the document id *is* the author's uid. */
    fun reviews(cafeId: String) = cafe(cafeId).collection("reviews")

    /**
     * Per-cafe visit counters. Answers "who conquered this cafe", "how many times
     * have I been here" and the cooldown check without ever scanning `checkins`.
     */
    fun visitors(cafeId: String) = cafe(cafeId).collection("visitors")

    /** The mirror of [visitors], for a user's personal conquest map. */
    fun conquered(uid: String) = user(uid).collection("conquered")

    fun badges(uid: String) = user(uid).collection("badges")

    /** Rotating QR codes, keyed by the token itself so a scan is a single-document read. */
    fun qrTokens(cafeId: String) = cafe(cafeId).collection("qrTokens")

    fun checkIns() = db.collection("checkins")

    /** Images, base64 in their own documents - see [rs.coffeeconquest.app.data.firebase.PhotoSource]. */
    fun photos() = db.collection("photos")
    fun follows() = db.collection("follows")
    fun challenges() = db.collection("challenges")
    fun feed() = db.collection("feed")

    /** Deterministic id so following someone twice is idempotent. */
    fun followId(followerId: String, followeeId: String) = "${followerId}_$followeeId"
}

/** Carries a message that is already in the app's language, ready for a Snackbar. */
class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)

// ------------------------------------------------------------------ helpers

suspend fun Query.fetch(): List<DocumentSnapshot> = get().await().documents

suspend fun com.google.firebase.firestore.DocumentReference.fetch(): DocumentSnapshot = get().await()

fun DocumentSnapshot.str(field: String): String? = getString(field)

fun DocumentSnapshot.int(field: String): Int = (get(field) as? Number)?.toInt() ?: 0

fun DocumentSnapshot.long(field: String): Long = (get(field) as? Number)?.toLong() ?: 0L

fun DocumentSnapshot.longOrNull(field: String): Long? = (get(field) as? Number)?.toLong()

fun DocumentSnapshot.double(field: String): Double = (get(field) as? Number)?.toDouble() ?: 0.0

fun DocumentSnapshot.doubleOrNull(field: String): Double? = (get(field) as? Number)?.toDouble()

fun DocumentSnapshot.bool(field: String): Boolean = getBoolean(field) ?: false

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.strings(field: String): List<String> =
    (get(field) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

/** Enum stored by name; falls back to [fallback] if the document predates a value. */
inline fun <reified T : Enum<T>> DocumentSnapshot.enum(field: String, fallback: T): T =
    getString(field)?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: fallback

/**
 * Firestore's `whereIn` takes at most 30 values, so friend lists and id lookups
 * are queried in chunks and stitched back together.
 */
const val WHERE_IN_LIMIT = 30

/** Turns any Firebase failure into a message that can go straight into a Snackbar. */
fun Throwable.userMessage(): String = when (this) {
    is AppException -> message ?: "Nesto je poslo naopako."
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_INVALID_EMAIL" -> "Neispravna email adresa."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Pogresno korisnicko ime ili lozinka."
        "ERROR_USER_NOT_FOUND" -> "Pogresno korisnicko ime ili lozinka."
        "ERROR_USER_DISABLED" -> "Nalog je blokiran."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Email je vec zauzet."
        "ERROR_WEAK_PASSWORD" -> "Lozinka je preslaba."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Server nije dostupan. Proverite internet konekciju."
        else -> message ?: "Prijava nije uspela."
    }
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Nemate dozvolu za ovu akciju."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Server nije dostupan. Proverite internet konekciju."
        FirebaseFirestoreException.Code.NOT_FOUND -> "Trazeni podatak ne postoji."
        else -> message ?: "Nesto je poslo naopako."
    }
    else -> message ?: "Nesto je poslo naopako. Proverite internet konekciju."
}
