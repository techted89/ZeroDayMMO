package com.zeroday.util

import java.util.UUID

object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
    fun shortId(): String = UUID.randomUUID().toString().take(8)
    fun sessionId(playerId: String): String = "session_${playerId}_${System.currentTimeMillis()}"
    fun taskInstanceId(): String = "task_${shortId()}"
    fun eventInstanceId(): String = "evt_${shortId()}"
}
