package com.zeroday.handler

import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

/**
 * Minimal WebSocketSession stub used by router tests. The handlers in
 * MessageRouterTest never read or write frames, so we just need something
 * that satisfies the [WebSocketSession] contract.
 */
class FakeWebSocketSession : WebSocketSession {
    override val incoming: kotlinx.coroutines.channels.ReceiveChannel<Frame> = Channel()
    override val outgoing: kotlinx.coroutines.channels.SendChannel<Frame> = Channel()
    override val extensions: List<WebSocketExtension<*>> = emptyList()
    override var masking: Boolean = false
    override var maxFrameSize: Long = Long.MAX_VALUE
    override val coroutineContext: kotlin.coroutines.CoroutineContext
        get() = kotlinx.coroutines.Dispatchers.Unconfined + CompletableDeferred<Unit>()
    override suspend fun send(frame: Frame) {}
    override suspend fun flush() {}
    override fun terminate() {}
}
