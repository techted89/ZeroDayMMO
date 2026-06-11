package com.zeroday

import com.zeroday.config.ServerConfig
import com.zeroday.handler.ConnectionRegistry
import com.zeroday.handler.MessageRouter
import com.zeroday.handler.ServiceRegistry
import com.zeroday.handler.WebSocketHandler
import com.zeroday.handler.handlers.AdHandler
import com.zeroday.handler.handlers.AchievementHandler
import com.zeroday.handler.handlers.AuthHandler
import com.zeroday.handler.handlers.AuctionHandler
import com.zeroday.handler.handlers.BossHandler
import com.zeroday.handler.handlers.CareerHandler
import com.zeroday.handler.handlers.ChallengeHandler
import com.zeroday.handler.handlers.GameEventHandler
import com.zeroday.handler.handlers.ZoneHandler
import com.zeroday.handler.handlers.CommandHandler
import com.zeroday.handler.handlers.FactionHandler
import com.zeroday.handler.handlers.KMapHandler
import com.zeroday.handler.handlers.NexusHandler
import com.zeroday.handler.handlers.NetworkHandler
import com.zeroday.handler.handlers.NotificationHandler
import com.zeroday.handler.handlers.PlayerHandler
import com.zeroday.handler.handlers.ProfileHandler
import com.zeroday.handler.handlers.ResearchHandler
import com.zeroday.handler.handlers.SkillHandler
import com.zeroday.handler.handlers.StorylineHandler
import com.zeroday.handler.handlers.SubscriptionHandler
import com.zeroday.handler.handlers.TaskHandler
import com.zeroday.handler.handlers.WorldEventHandler
import com.zeroday.security.BcryptPasswordHasher
import com.zeroday.security.RateLimiter
import com.zeroday.handler.adminRoutes
import com.zeroday.service.*
import com.zeroday.service.AdminService
import com.zeroday.service.BanService
import com.zeroday.service.CheatDetectionService
import com.zeroday.util.AppScope
import com.zeroday.zdscript.NexusScriptEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

private val log = LoggerFactory.getLogger("com.zeroday.ZeroDayServer")

fun main() {
    val server = embeddedServer(Netty, port = ServerConfig.PORT, host = "0.0.0.0") {
        configure()
    }

    // Register a shutdown hook so we save player data and close
    // connections gracefully on SIGTERM/SIGINT (docker stop, systemctl
    // stop, Ctrl+C).
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutting down ZeroDayMMO server…")
        server.stop(gracePeriodMillis = 10_000, timeoutMillis = 30_000)
        log.info("ZeroDayMMO server stopped.")
    })

    server.start(wait = true)
}

fun Application.configure() {
    val services = buildServiceRegistry(AppScope.scope)
    val router = buildMessageRouter(services)
    val connectionRegistry = ConnectionRegistry(
        router = router,
        maxTotalConnections = ServerConfig.MAX_CONNECTIONS,
        maxConnectionsPerIp = ServerConfig.MAX_CONNECTIONS_PER_IP
    )
    router.attachConnectionRegistry(connectionRegistry)
    val webSocketHandler = WebSocketHandler(services, router, connectionRegistry)

    // Wire real-time push for world events and achievement/challenge
    // notifications. These services are created after the service registry
    // so they can reference the connectionRegistry.
    services.worldEventService.connectionRegistry = connectionRegistry
    services.gameEventWiring.connectionRegistry = connectionRegistry

    // Persistence: restore then start background flush. If the snapshot
    // is missing/corrupt we still start the flush so we *do* get a file
    // written before the next crash.
    val persistenceDir = java.io.File(ServerConfig.DATA_DIR)
    val persistence = PlayerPersistence(
        playerService = services.playerService,
        directory = persistenceDir
    )
    runBlocking { persistence.restore() }
    persistence.startBackgroundFlush()

    // Background loops
    services.taskService.start(AppScope.scope)
    services.factionService.start(AppScope.scope)
    services.worldZoneService.start(AppScope.scope)
    services.heatCascadeService.start(AppScope.scope)
    services.coopBossService.start(AppScope.scope)
    services.researchService.start(AppScope.scope)
    services.auctionService.start(AppScope.scope)
    services.gameEventService.start(AppScope.scope)
    // Server-wide scheduler for periodic stats logging and maintenance.
    com.zeroday.service.ServerScheduler(
        playerService = services.playerService,
        connectionRegistry = connectionRegistry,
        persistence = persistence
    ).start(AppScope.scope)
    // Single ticker for resource regen (one coroutine, not N).
    kotlinx.coroutines.runBlocking {
        com.zeroday.service.ResourceRegenTicker(services.playerService, intervalMs = ServerConfig.REGEN_INTERVAL_MS).start()
    }
    // Idle connection watchdog — closes stale connections.
    kotlinx.coroutines.runBlocking {
        com.zeroday.handler.ConnectionWatchdog(
            connectionRegistry,
            idleTimeoutMs = ServerConfig.IDLE_TIMEOUT_MS,
            sweepIntervalMs = ServerConfig.SWEEP_INTERVAL_MS
        ).start()
    }

    // Admin services
    val banService = BanService(services.playerService)
    val cheatDetectionService = CheatDetectionService(services.playerService, banService)
    val adminService = AdminService(services.playerService, banService, cheatDetectionService, connectionRegistry)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    install(WebSockets) {
        pingPeriod = Duration.ofMillis(15_000)
        timeout = Duration.ofMillis(30_000)
        maxFrameSize = com.zeroday.config.ServerConfig.MAX_INBOUND_BYTES.toLong()
    }

    routing {
        get("/") {
            call.respondText(
                """
                | ╔══════════════════════════════════════╗
                | ║      ${ServerConfig.SERVER_NAME.padEnd(35)}║
                | ║      MMORPG Hacking Simulator        ║
                | ╠══════════════════════════════════════╣
                | ║  Status: ONLINE                      ║
                | ║  Port: ${ServerConfig.PORT.toString().padEnd(30)}║
                | ║  Max Players: ${ServerConfig.MAX_PLAYERS.toString().padEnd(24)}║
                | ╚══════════════════════════════════════╝
                |
                | Connect via WebSocket at ws://localhost:${ServerConfig.PORT}/game
                |""".trimMargin("|"),
                ContentType.Text.Plain
            )
        }

        // Kubernetes-style liveness probe: is the process alive?
        // Returns 200 as long as the Ktor engine is running.
        get("/live") {
            call.respondText("""{"status":"alive"}""", ContentType.Application.Json)
        }

        // Readiness probe: is the server ready to accept traffic?
        // If the route is reachable, persistence has restored and services
        // are initialised, so we always return 200.
        get("/ready") {
            call.respondText("""{"status":"ready","players":${services.playerService.getOnlinePlayers().size}}""", ContentType.Application.Json)
        }

        get("/health") {
            call.respondText(
                "{\"status\":\"ok\",\"players\":${services.playerService.getOnlinePlayers().size}}",
                ContentType.Application.Json
            )
        }

        get("/stats") {
            // Extended diagnostics. Lighter than a full OpenMetrics export
            // but enough for an external monitor to scrape.
            val extra = mutableMapOf<String, Any?>(
                "watchdog" to com.zeroday.handler.ConnectionWatchdog(connectionRegistry).status()
            )
            val stats = com.zeroday.service.ServerStats(
                onlinePlayers = services.playerService.getOnlinePlayers().size,
                totalConnections = connectionRegistry.totalConnections(),
                persistence = persistence.status() + extra
            )
            call.respondText(stats.toJsonString(), ContentType.Application.Json)
        }

        route("/game") {
            webSocket {
                webSocketHandler.handle(this)
            }
        }

        adminRoutes(adminService, services.playerService, banService, cheatDetectionService)
    }

    log.info("Server starting on port {} with {} max players. WebSocket: ws://0.0.0.0:{}/game",
        ServerConfig.PORT, ServerConfig.MAX_PLAYERS, ServerConfig.PORT)
}

private fun buildServiceRegistry(scope: kotlinx.coroutines.CoroutineScope): ServiceRegistry {
    val playerService = PlayerService(BcryptPasswordHasher())
    val gameEventBus = com.zeroday.service.GameEventBus()
    val commandService = CommandService(playerService, gameEventBus)
    val eventService = EventService(playerService, gameEventBus)
    val contractGenerator = ContractGenerator(playerService)
    val taskService = TaskService(playerService, contractGenerator, gameEventBus)
    val networkService = NetworkService(playerService, gameEventBus)
    val factionService = FactionService(playerService, gameEventBus)
    val researchService = ResearchService(playerService, gameEventBus)
    val hackToLearnService = HackToLearnService(playerService, gameEventBus)
    val worldEventService = WorldEventService(playerService, factionService, gameEventBus)
    val adRewardService = AdRewardService(playerService)
    val achievementService = AchievementService()
    val notificationService = NotificationService()
    val challengeService = ChallengeService()
    val skillService = SkillService()
    val gameEventWiring = com.zeroday.service.GameEventWiring(
        bus = gameEventBus,
        playerService = playerService,
        achievementService = achievementService,
        challengeService = challengeService,
        notificationService = notificationService,
        skillService = skillService
    )
    gameEventWiring.install()
    return ServiceRegistry(
        playerService = playerService,
        commandService = commandService,
        eventService = eventService,
        taskService = taskService,
        networkService = networkService,
        factionService = factionService,
        researchService = researchService,
        contractGenerator = contractGenerator,
        hackToLearnService = hackToLearnService,
        worldEventService = worldEventService,
        adRewardService = adRewardService,
        achievementService = achievementService,
        notificationService = notificationService,
        challengeService = challengeService,
        skillService = skillService,
        gameEventBus = gameEventBus,
        appScope = scope,
        dailyLoginService = DailyLoginService(playerService, notificationService, challengeService),
        gameEventWiring = gameEventWiring,
        careerService = CareerService(playerService, gameEventBus),
        worldZoneService = WorldZoneService(playerService, gameEventBus),
        heatCascadeService = HeatCascadeService(playerService, CareerService(playerService, gameEventBus), gameEventBus),
        coopBossService = CoopBossService(playerService, WorldZoneService(playerService, gameEventBus), gameEventBus),
        auctionService = AuctionService(playerService, gameEventBus),
        gameEventService = GameEventService(playerService, gameEventBus)
    )
}

    private fun buildMessageRouter(services: ServiceRegistry): MessageRouter {
    val authRateLimiter = RateLimiter(maxRequests = 5, windowMs = 60_000L)
    val scriptEngine = NexusScriptEngine()
    val handlers = listOf(
        AuthHandler(authRateLimiter, services.dailyLoginService),
        PlayerHandler(),
        CommandHandler(),
        StorylineHandler(),
        TaskHandler(),
        NetworkHandler(),
        FactionHandler(),
        ResearchHandler(),
        NexusHandler(scriptEngine),
        KMapHandler(),
        WorldEventHandler(),
        AdHandler(),
        AchievementHandler(services.achievementService, services.notificationService),
        ChallengeHandler(services.challengeService, services.notificationService),
        NotificationHandler(services.notificationService),
        SkillHandler(services.skillService),
        ProfileHandler(services.achievementService, services.skillService),
        SubscriptionHandler(),
        CareerHandler(),
        ZoneHandler(),
        BossHandler(),
        AuctionHandler(),
        GameEventHandler()
    )
    return MessageRouter(handlers)
}
