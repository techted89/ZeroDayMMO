package com.zeroday.service

import com.zeroday.model.AchievementCategory
import com.zeroday.model.AchievementEvent
import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FastHasherA : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class AchievementServiceTest {
    private val svc = AchievementService()
    private val playerService = PlayerService(FastHasherA())

    @Test
    fun definitionsCoverAllCategories() {
        assertTrue(svc.definitions.isNotEmpty())
        val categories = svc.definitions.map { it.category }.toSet()
        // META category is reserved for future "unlock all X" achievements
        // so it may legitimately be empty. Every other category must have
        // at least one definition.
        val expected = AchievementCategory.values().filter { it != AchievementCategory.META }.toSet()
        assertEquals(
            expected,
            categories,
            "Every AchievementCategory except META should have at least one definition; missing: ${expected - categories}"
        )
    }

    @Test
    fun recordIncrementsProgressAndCompletes() = runBlocking {
        val player = playerService.register("alice", "pw").getOrThrow()
        // 24 nodes should not yet complete node_hunter (target=25)
        val u1 = svc.record(player, AchievementEvent.NODE_DISCOVERED, 24L)
        val nodeHunter24 = u1.first { it.achievementId == "node_hunter" }
        assertEquals(24L, nodeHunter24.newValue)
        assertEquals(false, nodeHunter24.completed)
        assertTrue(!player.unlockedAchievements.contains("node_hunter"))
        // 1 more -> total 25 -> completes
        val u2 = svc.record(player, AchievementEvent.NODE_DISCOVERED, 1L)
        val nodeHunter25 = u2.first { it.achievementId == "node_hunter" }
        assertEquals(25L, nodeHunter25.newValue)
        assertEquals(true, nodeHunter25.completed)
        assertTrue(player.unlockedAchievements.contains("node_hunter"))
        // first_blood (target=1, also listens to NODE_DISCOVERED) was unlocked at delta=24
        assertTrue(player.unlockedAchievements.contains("first_blood"))
    }

    @Test
    fun completedAchievementsReportNoFurtherUpdates() = runBlocking {
        val player = playerService.register("bob", "pw").getOrThrow()
        svc.record(player, AchievementEvent.NODE_DISCOVERED, 1L) // completes first_blood
        // Subsequent calls should not return an update for first_blood
        val updates = svc.record(player, AchievementEvent.NODE_DISCOVERED, 1L)
        val firstBlood = updates.firstOrNull { it.achievementId == "first_blood" }
        assertEquals(null, firstBlood, "Completed achievements should not be reported again")
    }

    @Test
    fun visibleToHidesUnearnedHiddenAchievements() = runBlocking {
        val player = playerService.register("c", "pw").getOrThrow()
        val visible = svc.visibleTo(player)
        val hiddenDefs = svc.definitions.filter { it.hidden }
        if (hiddenDefs.isNotEmpty()) {
            val hiddenIds = hiddenDefs.map { it.id }
            val visibleIds = visible.map { it.id }
            assertTrue(
                visibleIds.intersect(hiddenIds.toSet()).isEmpty(),
                "Hidden achievements should not be visible until earned"
            )
        }
    }

    @Test
    fun summaryForCountsCategories() = runBlocking {
        val player = playerService.register("d", "pw").getOrThrow()
        // Drive 25 nodes -> unlocks first_blood, node_hunter; all in COMBAT
        svc.record(player, AchievementEvent.NODE_DISCOVERED, 25L)
        val summary = svc.summaryFor(player)
        assertEquals(2, summary[AchievementCategory.COMBAT.name])
    }
}
