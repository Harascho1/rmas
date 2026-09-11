package rs.coffeeconquest.shared.rules

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.CheckInMethod

object ScoreRules {

    const val BASE_CHECK_IN = 10
    const val FIRST_VISIT_BONUS = 15
    const val PHOTO_BONUS = 5
    const val QR_BONUS = 10
    const val REVIEW_BONUS = 5

    const val HONOR_MULTIPLIER = 0.5

    const val STREAK_POINTS_PER_DAY = 2
    const val MAX_STREAK_FOR_BONUS = 7

    const val COOLDOWN_HOURS_PER_CAFE = 6

    const val MAX_CHECK_INS_PER_DAY = 10

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

    fun levelProgress(points: Int): LevelProgress {
        val level = levelFor(points)
        var spent = 0
        for (l in 1 until level) spent += LEVEL_STEP * l
        val needed = LEVEL_STEP * level
        return LevelProgress(level = level, into = points - spent, needed = needed)
    }

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
        val honorPenalty: Int,
        val challengeMultiplier: Double,
        val total: Int,
    )

    @Serializable
    data class LevelProgress(val level: Int, val into: Int, val needed: Int) {
        val fraction: Float get() = if (needed == 0) 0f else into.toFloat() / needed
    }
}
