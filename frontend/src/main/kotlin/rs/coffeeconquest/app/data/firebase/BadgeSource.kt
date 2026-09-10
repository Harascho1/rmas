package rs.coffeeconquest.app.data.firebase

import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.EarnedBadge
import rs.coffeeconquest.shared.model.Badges
import rs.coffeeconquest.shared.rules.Time

/** Evaluates the badge catalogue after a check-in and awards what is newly earned. */
class BadgeSource {

    suspend fun evaluateAfterCheckIn(
        userId: String,
        checkInHour: Int,
        distinctCafes: Int,
        streakDays: Int,
        reviewCount: Int,
    ): List<EarnedBadge> {
        val candidates = buildList {
            add(Badges.FIRST_SIP)
            if (distinctCafes >= 5) add(Badges.EXPLORER_5)
            if (distinctCafes >= 10) add(Badges.EXPLORER_10)
            if (distinctCafes >= 25) add(Badges.EXPLORER_25)
            if (streakDays >= 7) add(Badges.STREAK_7)
            if (streakDays >= 30) add(Badges.STREAK_30)
            if (reviewCount >= 10) add(Badges.CRITIC_10)
            if (checkInHour < 8) add(Badges.EARLY_BIRD)
            if (checkInHour >= 22) add(Badges.NIGHT_OWL)
        }
        return award(userId, candidates)
    }

    /**
     * Grants any of [codes] the user does not already hold. The badge code is the
     * document id, so "already holds it" is a single read and re-awarding is a no-op.
     */
    suspend fun award(userId: String, codes: List<String>): List<EarnedBadge> {
        if (codes.isEmpty()) return emptyList()

        val owned = Fire.badges(userId).fetch().map { it.id }.toSet()
        val fresh = codes.distinct().filter { it !in owned }.mapNotNull { Badges.find(it) }
        if (fresh.isEmpty()) return emptyList()

        val now = Time.now()
        Fire.db.batch().apply {
            fresh.forEach { definition ->
                set(
                    Fire.badges(userId).document(definition.code),
                    mapOf("code" to definition.code, "earnedAt" to now),
                )
            }
        }.commit().await()

        return fresh.map {
            EarnedBadge(it.code, it.title, it.description, it.emoji, now)
        }
    }
}
