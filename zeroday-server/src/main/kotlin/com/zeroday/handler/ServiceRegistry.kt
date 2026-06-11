package com.zeroday.handler

import com.zeroday.service.*
import kotlinx.coroutines.CoroutineScope

/**
 * Aggregate reference to every service the WebSocket layer needs. We pass
 * a single value object to handler constructors (instead of N individual
 * services) to keep handler signatures manageable and to make it trivial to
 * mock services in tests.
 *
 * Use [ServiceRegistry.minimal] for tests that only need a [PlayerService].
 */
class ServiceRegistry(
    val playerService: PlayerService,
    val commandService: CommandService,
    val eventService: EventService,
    val taskService: TaskService,
    val networkService: NetworkService,
    val factionService: FactionService,
    val researchService: ResearchService,
    val contractGenerator: ContractGenerator,
    val hackToLearnService: HackToLearnService,
    val worldEventService: WorldEventService,
    val adRewardService: AdRewardService,
    val achievementService: AchievementService,
    val notificationService: NotificationService,
    val challengeService: ChallengeService,
    val skillService: SkillService,
    val gameEventBus: com.zeroday.service.GameEventBus,
    val appScope: CoroutineScope,
    val dailyLoginService: DailyLoginService = DailyLoginService(playerService, notificationService, challengeService),
    val gameEventWiring: GameEventWiring = GameEventWiring(
        bus = gameEventBus, playerService = playerService,
        achievementService = achievementService, challengeService = challengeService,
        notificationService = notificationService, skillService = skillService
    ),
    val careerService: CareerService = CareerService(playerService, gameEventBus),
    val worldZoneService: WorldZoneService = WorldZoneService(playerService, gameEventBus),
    val heatCascadeService: HeatCascadeService = HeatCascadeService(playerService, careerService, gameEventBus),
    val coopBossService: CoopBossService = CoopBossService(playerService, worldZoneService, gameEventBus),
    val auctionService: AuctionService = AuctionService(playerService, gameEventBus),
    val gameEventService: GameEventService = GameEventService(playerService, gameEventBus),
    val banService: BanService = BanService(playerService),
    val cheatDetectionService: CheatDetectionService = CheatDetectionService(playerService, banService)
) {
    companion object {
        /**
         * Create the simplest valid registry for tests that only touch
         * [PlayerService]. All other services are no-op stubs.
         */
        fun minimal(playerService: PlayerService, scope: CoroutineScope): ServiceRegistry {
            val bus = GameEventBus()
            val cs = CommandService(playerService, bus)
            val cg = ContractGenerator(playerService)
            return ServiceRegistry(
                playerService = playerService,
                commandService = cs,
                eventService = EventService(playerService, bus),
                taskService = TaskService(playerService, cg, bus),
                networkService = NetworkService(playerService, bus),
                factionService = FactionService(playerService, bus),
                researchService = ResearchService(playerService, bus),
                contractGenerator = cg,
                hackToLearnService = HackToLearnService(playerService, bus),
                worldEventService = WorldEventService(playerService, FactionService(playerService, bus), bus),
                adRewardService = AdRewardService(playerService),
                achievementService = AchievementService(),
                notificationService = NotificationService(),
                challengeService = ChallengeService(),
                skillService = SkillService(),
                gameEventBus = bus,
                appScope = scope,
                careerService = CareerService(playerService, bus),
                worldZoneService = WorldZoneService(playerService, bus),
                heatCascadeService = HeatCascadeService(playerService, CareerService(playerService, bus), bus),
                coopBossService = CoopBossService(playerService, WorldZoneService(playerService, bus), bus),
                auctionService = AuctionService(playerService, bus),
                gameEventService = GameEventService(playerService, bus)
            )
        }
    }
}
