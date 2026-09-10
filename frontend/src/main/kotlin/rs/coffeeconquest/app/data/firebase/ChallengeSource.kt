package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.Challenge
import rs.coffeeconquest.shared.dto.CreateChallengeRequest
import rs.coffeeconquest.shared.model.ChallengeScope
import rs.coffeeconquest.shared.rules.Time

class ChallengeSource {
    companion object {
        const val MIN_MULTIPLIER = 1.5
        const val MAX_MULTIPLIER = 3.0
        const val MAX_DURATION_DAYS = 14L
    }

    suspend fun create(
        request: CreateChallengeRequest,
        creator: DocumentSnapshot,
        canManageCafe: suspend (String) -> Unit,
        canBoostCity: suspend (String) -> Unit,
    ): Challenge {
        if (request.title.isBlank()) throw AppException("Naslov je obavezan.")
        if (request.multiplier !in MIN_MULTIPLIER..MAX_MULTIPLIER) {
            throw AppException("Mnozilac mora biti izmedju $MIN_MULTIPLIER i $MAX_MULTIPLIER.")
        }
        if (request.endsAtEpochMs <= request.startsAtEpochMs) {
            throw AppException("Kraj mora biti posle pocetka.")
        }
        if (request.endsAtEpochMs - request.startsAtEpochMs > MAX_DURATION_DAYS * 24 * 60 * 60 * 1000) {
            throw AppException("Izazov moze trajati najvise $MAX_DURATION_DAYS dana.")
        }

        var cafeName: String? = null
        when (request.scope) {
            ChallengeScope.CAFE -> {
                val cafeId = request.cafeId ?: throw AppException("Nedostaje kafic.")
                canManageCafe(cafeId)
                cafeName = Fire.cafe(cafeId).fetch().str("name")
            }

            ChallengeScope.CITY -> {
                val city = request.city?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw AppException("Nedostaje grad.")
                canBoostCity(city)
            }
        }

        val city = request.city?.trim()?.takeIf { it.isNotEmpty() }
        val doc = Fire.challenges().add(
            mapOf(
                "scope" to request.scope.name,
                "cafeId" to request.cafeId,
                "cafeName" to cafeName,
                "city" to city,
                "cityLower" to city?.lowercase(),
                "title" to request.title.trim(),
                "description" to request.description?.trim(),
                "multiplier" to request.multiplier,
                "startsAt" to request.startsAtEpochMs,
                "endsAt" to request.endsAtEpochMs,
                "createdById" to creator.id,
                "createdByDisplayName" to creator.str("displayName"),
                "createdAt" to Time.now(),
            ),
        ).await()

        return doc.get().await().toChallenge()
    }

    suspend fun delete(id: String, actor: DocumentSnapshot, isAdmin: Boolean) {
        val doc = Fire.challenges().document(id).fetch()
        if (!doc.exists()) throw AppException("Izazov ne postoji.")
        if (!isAdmin && doc.str("createdById") != actor.id) {
            throw AppException("Mozete obrisati samo svoje izazove.")
        }
        Fire.challenges().document(id).delete().await()
    }

    suspend fun activeNow(): List<Challenge> {
        val now = Time.now()
        return Fire.challenges()
            .whereGreaterThanOrEqualTo("endsAt", now)
            .fetch()
            .map { it.toChallenge() }
            .filter { it.startsAtEpochMs <= now }
    }

    suspend fun activeFor(cafeId: String, city: String?): List<Challenge> =
        activeNow().filter { challenge ->
            challenge.cafeId == cafeId || (city != null && challenge.city.equals(
                city,
                ignoreCase = true
            ))
        }

    suspend fun multiplierFor(cafeId: String, city: String?): Pair<Double, Challenge?> {
        val best = activeFor(cafeId, city).maxByOrNull { it.multiplier }
        return (best?.multiplier ?: 1.0) to best
    }

    suspend fun list(city: String?, cafeId: String?, onlyActive: Boolean): List<Challenge> {
        val now = Time.now()
        val docs = when {
            cafeId != null -> Fire.challenges().whereEqualTo("cafeId", cafeId).fetch()
            !city.isNullOrBlank() -> Fire.challenges().whereEqualTo("cityLower", city.lowercase())
                .fetch()

            else -> Fire.challenges().orderBy("endsAt", Query.Direction.ASCENDING).fetch()
        }
        return docs.map { it.toChallenge() }
            .filter { !onlyActive || (it.startsAtEpochMs <= now && it.endsAtEpochMs >= now) }
            .sortedBy { it.endsAtEpochMs }
    }
}
