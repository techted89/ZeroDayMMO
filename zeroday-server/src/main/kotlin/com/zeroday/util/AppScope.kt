package com.zeroday.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-wide coroutine scope. Uses a [SupervisorJob] so that one
 * failing background job (e.g. a regen loop for a disconnected player)
 * does not propagate cancellation to other long-running work.
 */
object AppScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
