package com.zeroday.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Lightweight operational stats exposed by GET /stats. Avoids a heavy
 * metrics dependency for the common "is it alive?" scrape use case.
 */
data class ServerStats(
    val onlinePlayers: Int,
    val totalConnections: Int,
    val persistence: Map<String, Any?>
) {
    fun toJsonString(): String {
        val obj = buildJsonObject {
            put("status", "ok")
            put("online_players", onlinePlayers)
            put("total_connections", totalConnections)
            put("persistence", encodeMap(persistence))
        }
        return Json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun encodeMap(map: Map<String, Any?>): JsonObject = buildJsonObject {
        for ((k, v) in map) {
            when (v) {
                is String -> put(k, v)
                is Number -> put(k, v)
                is Boolean -> put(k, v)
                is Long -> put(k, v)
                is Int -> put(k, v)
                null -> put(k, kotlinx.serialization.json.JsonNull)
                else -> put(k, v.toString())
            }
        }
    }
}
