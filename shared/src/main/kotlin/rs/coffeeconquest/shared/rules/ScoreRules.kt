package rs.coffeeconquest.shared.rules

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.CheckInMethod

/**
 * The scoring model. It lives in `shared` so the app can show the user
 * "ovaj check-in vredi 35 poena" *before* they submit, using the exact same
 * arithmetic that awards the points a moment later - and so that moving it
 * into a Cloud Function later would move one file, not the rules themselves.
 */
object ScoreRules {

    const val BASE_CHECK_IN = 10
    const val FIRST_VISIT_BONUS = 15
    const val PHOTO_BONUS = 5
    const val QR_BONUS = 10
    const val REVIEW_BONUS = 5

    /** Honor check-ins are worth half, rounded down - they carry no proof. */
    const val HONOR_MULTIPLIER = 0.5

    /** Each consecutive day adds this much, capped at [MAX_STREAK_FOR_BONUS] days. */
    const val STREAK_POINTS_PER_DAY = 2
    const val MAX_STREAK_FOR_BONUS = 7

    /** One user may only score at the same cafe once per this many hours. */
    const val COOLDOWN_HOURS_PER_CAFE = 6

    /** Scoring check-ins per user per day; anything beyond this is flagged. */
    const val MAX_CHECK_INS_PER_DAY = 10

    /** Points needed for each level; level N starts at `LEVEL_STEP * N * (N + 1) / 2`. */
    const val LEVEL_STEP = 100

    fun levelFor(points: Int): Int {
        var level = 1
        var threshold = LEVEL_STEP
        var spent = 0
        while (points >= spent + threshold) {
            spent += threshold
            level++
            threshold = LEVEL_STEP * level
        }
        return level
    }

    /** Points still needed to reach the next level, and how far into the current one we are. */
    fun levelProgress(points: Int): LevelProgress {
        val level = levelFor(points)
        var spent = 0
        for (l in 1 until level) spent += LEVEL_STEP * l
        val needed = LEVEL_STEP * level
        return LevelProgress(level = level, into = points - spent, needed = needed)
    }

    /**
     * Computes what a check-in is worth.
     *
     * @param method how the visit was proven
     * @param withPhoto the hunter attached a photo
     * @param firstVisitToCafe the hunter has never scored at this cafe before
     * @param streakDays consecutive days with a check-in, including today
     * @param challengeMultiplier active bonus challenge, 1.0 when none
     */
    fun checkInScore(
        method: CheckInMethod,
        withPhoto: Boolean,
        firstVisitToCafe: Boolean,
        streakDays: Int,
        challengeMultiplier: Double = 1.0,
    ): ScoreBreakdown {
        val base = BASE_CHECK_IN
        val photo = if (withPhoto) PHOTO_BONUS else 0
        val qr = if (method == CheckInMethod.QR) QR_BONUS else 0
        val firstVisit = if (firstVisitToCafe) FIRST_VISIT_BONUS else 0
        val streak = streakDays.coerceIn(0, MAX_STREAK_FOR_BONUS) * STREAK_POINTS_PER_DAY

        val subtotal = base + photo + qr + firstVisit + streak
        val afterMethod =
            if (method == CheckInMethod.HONOR) (subtotal * HONOR_MULTIPLIER).toInt() else subtotal
        val total = (afterMethod * challengeMultiplier).toInt()

        return ScoreBreakdown(
            base = base,
            photoBonus = photo,
            qrBonus = qr,
            firstVisitBonus = firstVisit,
            streakBonus = streak,
            honorPenalty = afterMethod - subtotal,
            challengeMultiplier = challengeMultiplier,
            total = total,
        )
    }

    @Serializable
    data class ScoreBreakdown(
        val base: Int,
        val photoBonus: Int,
        val qrBonus: Int,
        val firstVisitBonus: Int,
        val streakBonus: Int,
        /** Negative when the honor multiplier ate into the subtotal. */
        val honorPenalty: Int,
        val challengeMultiplier: Double,
        val total: Int,
    )

    @Serializable
    data class LevelProgress(val level: Int, val into: Int, val needed: Int) {
        val fraction: Float get() = if (needed == 0) 0f else into.toFloat() / needed
    }
}
