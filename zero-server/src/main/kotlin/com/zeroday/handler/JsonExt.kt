package com.zeroday.handler

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Helpers for pulling typed values out of a `JsonObject` payload without
 * littering handler code with `?.jsonPrimitive?.content ?: default` chains.
 */
internal fun JsonObject.str(key: String, default: String? = null): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: default

internal fun JsonObject.reqStr(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull
        ?: error("Missing or non-string field '$key'")

internal fun JsonObject.long(key: String, default: Long = 0L): Long =
    (this[key] as? JsonPrimitive)?.longOrNull ?: default

internal fun JsonObject.int(key: String, default: Int = 0): Int =
    (this[key] as? JsonPrimitive)?.longOrNull?.toInt() ?: default

internal fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

internal fun JsonObject.double(key: String, default: Double = 0.0): Double =
    (this[key] as? JsonPrimitive)?.doubleOrNull ?: default

internal fun JsonObject.obj(key: String): JsonObject? =
    (this[key] as? JsonObject)

internal fun JsonObject.elements(): List<JsonElement> = values.toList()
