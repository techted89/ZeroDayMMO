package com.zeroday.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory sliding-window rate limiter. Suitable for a single-node
 * deployment; for a clustered server swap [Storage] for a Redis-backed
 * implementation without touching call sites.
 */
class RateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    interface Storage {
        fun hitAndCount(key: String, now: Long, windowMs: Long): Int
        fun reset(key: String)
    }

    private val storage = object : Storage {
        private val buckets = ConcurrentHashMap<String, Bucket>()

        override fun hitAndCount(key: String, now: Long, windowMs: Long): Int {
            val bucket = buckets.compute(key) { _, current ->
                if (current == null || now - current.windowStart >= windowMs) {
                    Bucket(now, AtomicLong(1))
                } else {
                    current.count.incrementAndGet()
                    current
                }
            }!!
            return bucket.count.get().toInt()
        }

        override fun reset(key: String) {
            buckets.remove(key)
        }
    }

    fun tryAcquire(key: String): Boolean {
        val count = storage.hitAndCount(key, clock(), windowMs)
        return count <= maxRequests
    }

    /** Reset the rate-limit counter for [key], allowing an immediate retry. */
    fun reset(key: String) = storage.reset(key)

    private data class Bucket(val windowStart: Long, val count: AtomicLong)
}
