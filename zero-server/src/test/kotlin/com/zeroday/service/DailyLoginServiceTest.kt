package com.zeroday.service

import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FastHasherDLS : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class DailyLoginServiceTest {

    @Test
    fun firstLoginStartsStreakAt1() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("alice", "pw").getOrThrow()
            val result = svc.processLogin(p.id)
            assertNotNull(result)
            assertEquals(1, result.streak)
            assertTrue(result.isNewStreak)
            assertNotNull(result.streakReward)
        }
    }

    @Test
    fun consecutiveLoginWithinWindowIncrementsStreak() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("bob", "pw").getOrThrow()
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = System.currentTimeMillis() - DailyLoginService.DAY_MILLIS - 1
                player.loginStreak = 1
            }
            val result = svc.processLogin(p.id)
            assertNotNull(result)
            assertEquals(2, result.streak)
            assertTrue(result.isNewStreak)
        }
    }

    @Test
    fun loginTooSoonDoesNotReward() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("carol", "pw").getOrThrow()
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = System.currentTimeMillis()
                player.loginStreak = 5
            }
            val result = svc.processLogin(p.id)
            assertNotNull(result)
            assertEquals(5, result.streak)
            assertEquals(false, result.isNewStreak)
            assertNull(result.streakReward)
        }
    }

    @Test
    fun streakResetsAfterGap() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("dave", "pw").getOrThrow()
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = System.currentTimeMillis() - DailyLoginService.DAY_MILLIS * 3
                player.loginStreak = 10
            }
            val result = svc.processLogin(p.id)
            assertNotNull(result)
            assertEquals(1, result.streak, "Streak should reset to 1")
            assertTrue(result.isNewStreak)
        }
    }

    @Test
    fun streakRewardCyclesThrough7Days() {
        runBlocking {
            val r1 = DailyLoginService.dailyStreakReward(1)
            assertEquals(100, r1.credits)
            assertEquals(50, r1.xp)
            assertEquals(0, r1.reputation)
            val r7 = DailyLoginService.dailyStreakReward(7)
            assertEquals(2000, r7.credits)
            assertEquals(1000, r7.xp)
            assertEquals(5, r7.reputation)
            val r8 = DailyLoginService.dailyStreakReward(8)
            assertEquals(r1.credits, r8.credits)
            assertEquals(r1.xp, r8.xp)
        }
    }

    @Test
    fun offlineAccumulationCalculatedCorrectly() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("eve", "pw").getOrThrow()
            assertNull(svc.calculateOffline(p))
            val fourHoursAgo = System.currentTimeMillis() - 14_400_000L
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = fourHoursAgo
                player.level = 5
            }
            val acc = svc.calculateOffline(p)
            assertNotNull(acc)
            assertEquals(4.0, acc.offlineHours, 0.5)
            assertTrue(acc.cpuGained > 0)
            assertTrue(acc.ramGained > 0)
            assertTrue(acc.creditsGained > 0)
        }
    }

    @Test
    fun offlineAccumulationCapsAt8h() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("frank", "pw").getOrThrow()
            val twoDaysAgo = System.currentTimeMillis() - 172_800_000L
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = twoDaysAgo
                player.level = 10
            }
            val acc = svc.calculateOffline(p)
            assertNotNull(acc)
            assertTrue(acc.offlineHours <= 8.5, "Should be capped around 8h, got ${acc.offlineHours}")
        }
    }

    @Test
    fun processLoginAppliesOfflineResources() {
        runBlocking {
            val ps = PlayerService(FastHasherDLS())
            val svc = DailyLoginService(ps)
            val p = ps.register("grace", "pw").getOrThrow()
            ps.withPlayerAction(p.id) { player ->
                player.lastLoginAt = System.currentTimeMillis() - 21_600_000L
                player.level = 2
                player.cpu = 0
                player.ram = 0
                player.credits = 0
            }
            svc.processLogin(p.id)
            val fresh = ps.getPlayer(p.id)!!
            assertTrue(fresh.cpu > 0, "CPU should have regenned")
            assertTrue(fresh.ram > 0, "RAM should have regenned")
            assertTrue(fresh.credits > 0, "Credits should have been earned")
        }
    }
}
