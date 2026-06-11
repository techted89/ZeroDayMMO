package com.zeroday.handler

import com.zeroday.security.BcryptPasswordHasher
import com.zeroday.service.*
import com.zeroday.util.AppScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the message router that exercise routing, the
 * authentication gate, and the error-envelope contract using stub
 * handlers. We use real service instances because they're cheap to
 * construct and the tests don't actually exercise them.
 */
class MessageRouterTest {

    private val services: ServiceRegistry = run {
        val playerService = PlayerService(BcryptPasswordHasher())
        val commandService = CommandService(playerService)
        val eventService = EventService(playerService)
        val contractGenerator = ContractGenerator(playerService)
        val taskService = TaskService(playerService, contractGenerator)
        val networkService = NetworkService(playerService)
        val factionService = FactionService(playerService)
        val researchService = ResearchService(playerService)
        val hackToLearnService = HackToLearnService(playerService)
        val worldEventService = WorldEventService(playerService, factionService)
        val adRewardService = AdRewardService(playerService)
        val achievementService = com.zeroday.service.AchievementService()
        val notificationService = com.zeroday.service.NotificationService()
        val challengeService = com.zeroday.service.ChallengeService()
        val skillService = com.zeroday.service.SkillService()
        val gameEventBus = com.zeroday.service.GameEventBus()
        ServiceRegistry(
            playerService, commandService, eventService, taskService, networkService,
            factionService, researchService, contractGenerator, hackToLearnService,
            worldEventService, adRewardService, achievementService, notificationService,
            challengeService, skillService, gameEventBus, AppScope.scope
        )
    }

    private class EchoHandler(
        override val handledTypes: Set<String>,
        val requiresAuthFlag: Boolean = true
    ) : MessageHandler {
        override val requiresAuth: Boolean = requiresAuthFlag
        override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult =
            HandlerResult.Ok("echo", mapOf("received_type" to type, "authed" to ctx.connection.isAuthenticated()))
    }

    private class ThrowingHandler : MessageHandler {
        override val handledTypes: Set<String> = setOf("boom")
        override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
            throw IllegalStateException("kaboom")
        }
    }

    @Test
    fun `router dispatches to the handler registered for the type`() = runTest {
        val router = MessageRouter(listOf(EchoHandler(setOf("ping"), requiresAuthFlag = false)))
        val envelope = router.dispatch("ping", JsonObject(emptyMap()), ctx(playerId = null))
        val obj = Json.parseToJsonElement(envelope).jsonObject
        assertEquals("echo", obj["type"]?.jsonPrimitive?.content)
        assertEquals("ping", obj["received_type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `router rejects unknown message types with a validation error`() = runTest {
        val router = MessageRouter(emptyList())
        val envelope = router.dispatch("nope", JsonObject(emptyMap()), ctx(playerId = null))
        val obj = Json.parseToJsonElement(envelope).jsonObject
        assertEquals("error", obj["type"]?.jsonPrimitive?.content)
        assertEquals("VALIDATION", obj["error"]?.jsonObject?.get("category")?.jsonPrimitive?.content)
    }

    @Test
    fun `router blocks auth-required handlers when the player is not logged in`() = runTest {
        val router = MessageRouter(listOf(EchoHandler(setOf("secret"), requiresAuthFlag = true)))
        val envelope = router.dispatch("secret", JsonObject(emptyMap()), ctx(playerId = null))
        val obj = Json.parseToJsonElement(envelope).jsonObject
        assertEquals("error", obj["type"]?.jsonPrimitive?.content)
        assertEquals("AUTHENTICATION", obj["error"]?.jsonObject?.get("category")?.jsonPrimitive?.content)
    }

    @Test
    fun `router allows non-auth handlers for unauthenticated connections`() = runTest {
        val router = MessageRouter(listOf(EchoHandler(setOf("open"), requiresAuthFlag = false)))
        val envelope = router.dispatch("open", JsonObject(emptyMap()), ctx(playerId = null))
        val obj = Json.parseToJsonElement(envelope).jsonObject
        assertEquals("echo", obj["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `router catches handler exceptions and returns an internal error envelope`() = runTest {
        val router = MessageRouter(listOf(ThrowingHandler()))
        val envelope = router.dispatch("boom", JsonObject(emptyMap()), ctx(playerId = "p1"))
        val obj = Json.parseToJsonElement(envelope).jsonObject
        assertEquals("error", obj["type"]?.jsonPrimitive?.content)
        assertEquals("INTERNAL", obj["error"]?.jsonObject?.get("category")?.jsonPrimitive?.content)
        val message = obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        assertTrue(message?.contains("kaboom") == true, "expected the exception message in the envelope, got: $message")
    }

    private fun ctx(playerId: String?): HandlerContext {
        val conn = ConnectionInfo(sessionId = "s_test", playerId = playerId)
        val router = MessageRouter(emptyList())
        return HandlerContext(FakeWebSocketSession(), conn, services, router)
    }
}
