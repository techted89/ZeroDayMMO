package com.zeroday.handler

import io.ktor.websocket.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionRegistryTest {

    @Test
    fun `register creates a session id and accepts the connection`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 5, maxConnectionsPerIp = 2)
        val res = reg.register(FakeWebSocketSession(), remoteAddress = "1.2.3.4")
        assertTrue(res is ConnectionRegistry.RegisterResult.Accepted, "First connection should be accepted")
        val entry = (res as ConnectionRegistry.RegisterResult.Accepted).entry
        assertTrue(entry.info.sessionId.startsWith("ws_"))
        assertTrue(entry.info.sessionId.length > 16, "Session id should be a real token, not 12 chars of UUID")
        reg.unregister(entry.info.sessionId)
    }

    @Test
    fun `per-IP cap rejects excess connections`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 100, maxConnectionsPerIp = 2)
        val a1 = reg.register(FakeWebSocketSession(), remoteAddress = "10.0.0.1")
        val a2 = reg.register(FakeWebSocketSession(), remoteAddress = "10.0.0.1")
        val a3 = reg.register(FakeWebSocketSession(), remoteAddress = "10.0.0.1")
        assertTrue(a1 is ConnectionRegistry.RegisterResult.Accepted)
        assertTrue(a2 is ConnectionRegistry.RegisterResult.Accepted)
        assertTrue(a3 is ConnectionRegistry.RegisterResult.Rejected, "Third connection from same IP should be rejected")
        assertEquals(2, reg.totalConnections())
    }

    @Test
    fun `global cap rejects when reached`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 2, maxConnectionsPerIp = 10)
        reg.register(FakeWebSocketSession(), remoteAddress = "1.1.1.1")
        reg.register(FakeWebSocketSession(), remoteAddress = "2.2.2.2")
        val third = reg.register(FakeWebSocketSession(), remoteAddress = "3.3.3.3")
        assertTrue(third is ConnectionRegistry.RegisterResult.Rejected)
    }

    @Test
    fun `unregister decrements per-IP count`() = runTest {
        val router = MessageRouter(emptyList())
        val reg = ConnectionRegistry(router, maxTotalConnections = 100, maxConnectionsPerIp = 1)
        val a = reg.register(FakeWebSocketSession(), remoteAddress = "9.9.9.9")
        val sid = (a as ConnectionRegistry.RegisterResult.Accepted).entry.info.sessionId
        val b = reg.register(FakeWebSocketSession(), remoteAddress = "9.9.9.9")
        assertTrue(b is ConnectionRegistry.RegisterResult.Rejected)
        reg.unregister(sid)
        val c = reg.register(FakeWebSocketSession(), remoteAddress = "9.9.9.9")
        assertTrue(c is ConnectionRegistry.RegisterResult.Accepted, "After unregister, a new connection from same IP should be accepted")
    }
}
