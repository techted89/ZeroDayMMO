package com.zeroday.service

import com.zeroday.model.AchievementEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEventBusTest {

    @Test
    fun emitCallsAllListeners() = runTest {
        val bus = GameEventBus()
        val a = AtomicInteger(0)
        val b = AtomicInteger(0)
        bus.onEvent { a.incrementAndGet() }
        bus.onEvent { b.incrementAndGet() }
        bus.emit(GameEvent(playerId = "p1", type = AchievementEvent.NODE_DISCOVERED, value = 1))
        assertEquals(1, a.get())
        assertEquals(1, b.get())
    }

    @Test
    fun oneListenerThrowingDoesNotBreakOthers() = runTest {
        val bus = GameEventBus()
        val ok = AtomicInteger(0)
        bus.onEvent { throw IllegalStateException("boom") }
        bus.onEvent { ok.incrementAndGet() }
        bus.emit(GameEvent(playerId = "p1", type = AchievementEvent.NODE_DISCOVERED, value = 1))
        assertEquals(1, ok.get(), "Good listener should still fire after bad listener throws")
    }

    @Test
    fun listenersRunConcurrently() = runTest {
        val bus = GameEventBus()
        val start = System.nanoTime()
        val delayMs = 100L
        bus.onEvent { delay(delayMs) }
        bus.onEvent { delay(delayMs) }
        bus.onEvent { delay(delayMs) }
        bus.emit(GameEvent(playerId = "p1", type = AchievementEvent.NODE_DISCOVERED, value = 1))
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        // If they ran serially: ~300ms. Concurrently: ~100ms + small overhead.
        assertTrue(elapsedMs < 250, "Listeners should run concurrently, took ${elapsedMs}ms")
    }
}
