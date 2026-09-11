package rs.coffeeconquest.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object Fire {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val uid: String? get() = auth.currentUser?.uid
    fun requireUid(): String = uid ?: throw AppException("Niste prijavljeni.")

    fun users() = db.collection("users")
    fun user(uid: String) = users().document(uid)
    fun usernames() = db.collection("usernames")
    fun cafes() = db.collection("cafes")
    fun cafe(id: String) = cafes().document(id)
    fun reviews(cafeId: String) = cafe(cafeId).collection("reviews")
    fun visitors(cafeId: String) = cafe(cafeId).collection("visitors")
    fun conquered(uid: String) = user(uid).collection("conquered")
    fun badges(uid: String) = user(uid).collection("badges")
    fun qrTokens(cafeId: String) = cafe(cafeId).collection("qrTokens")
    fun checkIns() = db.collection("checkins")
    fun photos() = db.collection("photos")
    fun follows() = db.collection("follows")
    fun challenges() = db.collection("challenges")
    fun feed() = db.collection("feed")
    fun followId(followerId: String, followeeId: String) = "${followerId}_$followeeId"
}

class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)

suspend fun Query.fetch(): List<DocumentSnapshot> = get().await().documents

fun Query.snapshots(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot.documents)
        }
    }
    awaitClose { registration.remove() }
}

suspend fun com.google.firebase.firestore.DocumentReference.fetch(): DocumentSnapshot =
    get().await()

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

inline fun <reified T : Enum<T>> DocumentSnapshot.enum(field: String, fallback: T): T =
    getString(field)?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: fallback

const val WHERE_IN_LIMIT = 30

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
