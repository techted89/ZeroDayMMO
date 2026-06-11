package com.zeroday.handler

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionWatchdogTest {

    @Test
    fun `watchdog closes idle connections`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 100, maxConnectionsPerIp = 100)
        val session = FakeWebSocketSession()
        reg.register(session, remoteAddress = "1.1.1.1")
        assertEquals(1, reg.totalConnections())
        // The sweep checks lastActivity and connectedAt. The test
        // connection has very old timestamps so it should be swept.
        val d = ConnectionWatchdog(reg, idleTimeoutMs = 0, sweepIntervalMs = 1_000_000)
        d.sweepNow()
        assertEquals(0, reg.totalConnections(), "Idle connection should be closed")
    }

    @Test
    fun `active connection is not swept`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 100, maxConnectionsPerIp = 100)
        val session = FakeWebSocketSession()
        reg.register(session, remoteAddress = "2.2.2.2")
        reg.touch(reg.all().first().info.sessionId)
        val d = ConnectionWatchdog(reg, idleTimeoutMs = 1_000_000, sweepIntervalMs = 1_000_000)
        d.sweepNow()
        assertEquals(1, reg.totalConnections(), "Connection with recent activity should survive")
    }

    @Test
    fun `status reports sweep count`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router)
        val d = ConnectionWatchdog(reg, idleTimeoutMs = 10_000, sweepIntervalMs = 1_000_000)
        d.sweepNow()
        val s = d.status()
        assertTrue((s["sweeps"] as Int) >= 1)
    }
}
