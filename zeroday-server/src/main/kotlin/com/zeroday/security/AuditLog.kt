package com.zeroday.security

import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * Structured audit log for security-sensitive events.
 *
 * Every call writes one line to a dedicated "AUDIT" logger at INFO level.
 * The format is `event={name} player={id|-} ip={addr|-} extra={kv pairs}`.
 * Operators pipe that logger to a separate appender (file/syslog) so a
 * compromise of the regular game log doesn't lose the audit trail.
 *
 * MDC is set with "audit" so downstream logback configs can route
 * AUDIT events to a different file without filtering by content.
 */
object AuditLog {
    private val log = LoggerFactory.getLogger("AUDIT")

    fun event(
        name: String,
        playerId: String? = null,
        ip: String? = null,
        extra: Map<String, Any?> = emptyMap()
    ) {
        val prev = MDC.get("audit")
        try {
            MDC.put("audit", "1")
            log.info("event={} player={} ip={} extra={}", name, playerId ?: "-", ip ?: "-", extra)
        } finally {
            if (prev == null) MDC.remove("audit") else MDC.put("audit", prev)
        }
    }
}
