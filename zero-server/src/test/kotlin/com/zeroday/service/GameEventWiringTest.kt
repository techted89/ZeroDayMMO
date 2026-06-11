package com.zeroday.service

import com.zeroday.model.AchievementEvent
import com.zeroday.model.NotificationType
import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

private class FastHasherGEW : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

/**
 * Verifies the achievement / challenge listener coalesces multiple
 * unlocks into a single notification rather than spamming the inbox.
 */
class GameEventWiringTest {
    @Test
    fun multipleAchievementsInOneEventBecomeOneNotification() = runBlocking {
        val ps = PlayerService(FastHasherGEW())
        val bus = GameEventBus()
        val achievements = AchievementService()
        val challenges = ChallengeService()
        val notifications = NotificationService(perPlayerCap = 100)
        val skills = SkillService()
        GameEventWiring(bus, ps, achievements, challenges, notifications, skills).install()
        val p = ps.register("alice", "pw").getOrThrow()
        // Force a known challenge set so the test is deterministic.
        // We hand-pick daily_discover_3 and weekly_discover_25 — both
        // listen to NODE_DISCOVERED and both will complete with value=25.
        // Setting lastChallengeRotation avoids the lazy-initialization
        // branch in ChallengeService.rotateIfNeeded that clears anything
        // we set here.
        val now = System.currentTimeMillis()
        p.lastChallengeRotation = now
        p.activeChallenges.clear()
        p.activeChallenges += com.zeroday.model.ActiveChallenge(
            challengeId = "daily_discover_3", assignedAt = now, expiresAt = now + 86_400_000
        )
        p.activeChallenges += com.zeroday.model.ActiveChallenge(
            challengeId = "weekly_discover_25", assignedAt = now, expiresAt = now + 604_800_000
        )
        // NODE_DISCOVERED x 25 will complete:
        //   - first_blood (target=1)  -> achievement
        //   - node_hunter (target=25) -> achievement
        //   - daily_discover_3  (target=3)  -> challenge
        //   - weekly_discover_25 (target=25) -> challenge
        // All four fire as one emit; both the achievement and the
        // challenge branches should coalesce.
        bus.emit(GameEvent(playerId = p.id, type = AchievementEvent.NODE_DISCOVERED, value = 25))
        val n = notifications.list(p)
        val ach = n.filter { it.type == NotificationType.ACHIEVEMENT_UNLOCKED }
        val ch = n.filter { it.type == NotificationType.CHALLENGE_COMPLETED }
        assertEquals(1, ach.size, "Two achievements should coalesce to one, got ${ach.map { it.title }}")
        assertEquals(1, ch.size, "Two challenges should coalesce to one, got ${ch.map { it.title }}")
        assertEquals("2", ach[0].data["count"])
        assertEquals("2", ch[0].data["count"])
    }

    @Test
    fun singleAchievementStaysAsSingleNotification() = runBlocking {
        val ps = PlayerService(FastHasherGEW())
        val bus = GameEventBus()
        val achievements = AchievementService()
        val challenges = ChallengeService()
        val notifications = NotificationService(perPlayerCap = 100)
        val skills = SkillService()
        GameEventWiring(bus, ps, achievements, challenges, notifications, skills).install()
        val p = ps.register("bob", "pw").getOrThrow()
        // NODE_DISCOVERED x 1 completes only first_blood (target=1)
        bus.emit(GameEvent(playerId = p.id, type = AchievementEvent.NODE_DISCOVERED, value = 1))
        val ach = notifications.list(p).filter { it.type == NotificationType.ACHIEVEMENT_UNLOCKED }
        assertEquals(1, ach.size)
        // Single notifications are plain pushes, no coalesce key
        assertEquals(null, ach[0].data["count"])
    }
}
