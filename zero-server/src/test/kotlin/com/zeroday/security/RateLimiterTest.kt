package com.zeroday.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test
    fun `permits requests under the limit`() {
        val limiter = RateLimiter(maxRequests = 3, windowMs = 1000L, clock = { 0L })
        assertTrue(limiter.tryAcquire("k1"))
        assertTrue(limiter.tryAcquire("k1"))
        assertTrue(limiter.tryAcquire("k1"))
    }

    @Test
    fun `blocks requests over the limit in the same window`() {
        val limiter = RateLimiter(maxRequests = 3, windowMs = 1000L, clock = { 0L })
        repeat(3) { limiter.tryAcquire("k1") }
        assertFalse(limiter.tryAcquire("k1"))
    }

    @Test
    fun `keys are isolated from each other`() {
        val limiter = RateLimiter(maxRequests = 1, windowMs = 1000L, clock = { 0L })
        assertTrue(limiter.tryAcquire("k1"))
        assertFalse(limiter.tryAcquire("k1"))
        assertTrue(limiter.tryAcquire("k2"))
    }

    @Test
    fun `window slides forward`() {
        var now = 0L
        val limiter = RateLimiter(maxRequests = 1, windowMs = 1000L, clock = { now })
        assertTrue(limiter.tryAcquire("k1"))
        assertFalse(limiter.tryAcquire("k1"))
        now = 1001L
        assertTrue(limiter.tryAcquire("k1"))
    }
}
