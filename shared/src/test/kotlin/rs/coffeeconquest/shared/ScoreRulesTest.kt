package rs.coffeeconquest.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.rules.ScoreRules

class ScoreRulesTest {

    @Test
    fun `plain gps check-in is worth the base amount`() {
        val score = ScoreRules.checkInScore(
            method = CheckInMethod.GPS,
            withPhoto = false,
            firstVisitToCafe = false,
            streakDays = 0,
        )
        assertEquals(ScoreRules.BASE_CHECK_IN, score.total)
    }

    @Test
    fun `photo, qr, first visit and streak all add up`() {
        val score = ScoreRules.checkInScore(
            method = CheckInMethod.QR,
            withPhoto = true,
            firstVisitToCafe = true,
            streakDays = 3,
        )
        val expected = ScoreRules.BASE_CHECK_IN +
            ScoreRules.PHOTO_BONUS +
            ScoreRules.QR_BONUS +
            ScoreRules.FIRST_VISIT_BONUS +
            3 * ScoreRules.STREAK_POINTS_PER_DAY
        assertEquals(expected, score.total)
    }

    @Test
    fun `streak bonus stops growing after the cap`() {
        val capped = ScoreRules.checkInScore(CheckInMethod.GPS, false, false, ScoreRules.MAX_STREAK_FOR_BONUS)
        val beyond = ScoreRules.checkInScore(CheckInMethod.GPS, false, false, ScoreRules.MAX_STREAK_FOR_BONUS + 50)
        assertEquals(capped.total, beyond.total)
    }

    @Test
    fun `honor check-in is worth half`() {
        val honor = ScoreRules.checkInScore(CheckInMethod.HONOR, withPhoto = true, firstVisitToCafe = true, streakDays = 0)
        val gps = ScoreRules.checkInScore(CheckInMethod.GPS, withPhoto = true, firstVisitToCafe = true, streakDays = 0)
        assertEquals(gps.total / 2, honor.total)
        assertTrue(honor.honorPenalty < 0)
    }

    @Test
    fun `challenge multiplier scales the total`() {
        val normal = ScoreRules.checkInScore(CheckInMethod.GPS, false, false, 0)
        val boosted = ScoreRules.checkInScore(CheckInMethod.GPS, false, false, 0, challengeMultiplier = 2.0)
        assertEquals(normal.total * 2, boosted.total)
    }

    @Test
    fun `levels need progressively more points`() {
        assertEquals(1, ScoreRules.levelFor(0))
        assertEquals(1, ScoreRules.levelFor(ScoreRules.LEVEL_STEP - 1))
        assertEquals(2, ScoreRules.levelFor(ScoreRules.LEVEL_STEP))
        assertEquals(3, ScoreRules.levelFor(300))

        val progress = ScoreRules.levelProgress(150)
        assertEquals(2, progress.level)
        assertEquals(50, progress.into)
        assertEquals(200, progress.needed)
    }
}
