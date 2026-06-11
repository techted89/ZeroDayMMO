package com.zeroday.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WorldEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val eventType: WorldEventType,
    val severity: WorldEventSeverity,
    val startedAt: Long = System.currentTimeMillis(),
    var endsAt: Long = 0, // 0 means no end time (permanent until resolved)
    var isActive: Boolean = true,
    val affectedNetworks: List<String> = emptyList(),
    val effects: List<WorldEventEffect> = emptyList(),
    val participatingFactions: MutableList<String> = mutableListOf(),
    val resolutionCriteria: String = "",
    var rewardPool: Long = 0,
    var rewardPerParticipant: Long = 0
)

@Serializable
enum class WorldEventType {
    NETWORK_OUTAGE,
    SECURITY_ALERT,
    DATA_BREACH,
    MALWARE_OUTBREAK,
    LAW_ENFORCEMENT_CRACKDOWN,
    BLACK_MARKET_SALE,
    TECHNOLOGY_BREAKTHROUGH,
    INTERNATIONAL_CONFLICT,
    SOLAR_FLARE,
    QUANTUM_SUPreMACY
}

@Serializable
enum class WorldEventSeverity {
    MINOR,
    MODERATE,
    MAJOR,
    CRITICAL,
    CATASTROPHIC
}

@Serializable
data class WorldEventEffect(
    val effectType: EffectType,
    val value: Double,
    val durationMs: Long = 0 // 0 means permanent until event ends
)

@Serializable
enum class EffectType {
    XP_MULTIPLIER,
    CREDIT_MULTIPLIER,
    NETWORK_SPEED_BOOST,
    SECURITY_LEVEL_INCREASE,
    COMMAND_COOLDOWN_REDUCTION,
    RESOURCE_REGEN_BOOST,
    DROP_RATE_INCREASE,
    FACTION_REP_BOOST,
    NETWORK_VISIBILITY_BOOST,
    TASK_REWARD_BOOST
}

@Serializable
data class WorldEventParticipation(
    val playerId: String,
    val factionId: String? = null,
    val contributionScore: Int = 0,
    val rewardsClaimed: Boolean = false
)

@Serializable
data class WorldEventUpdate(
    val eventId: String,
    val title: String,
    val description: String,
    val eventType: WorldEventType,
    val severity: WorldEventSeverity,
    val isActive: Boolean,
    val timeRemainingMs: Long,
    val effects: List<WorldEventEffect>,
    val participantCount: Int,
    val rewardPool: Long
)

object WorldEventTemplates {
    private val networks = listOf(
        "10.0.0.0/24", "10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24",
        "172.16.0.0/16", "192.168.0.0/16", "198.51.100.0/24", "203.0.113.0/24"
    )

    val eventTemplates: List<WorldEvent> = listOf(
        WorldEvent(
            title = "Global Network Slowdown",
            description = "Internet backbone experiencing unprecedented latency spikes. All network operations slowed.",
            eventType = WorldEventType.NETWORK_OUTAGE,
            severity = WorldEventSeverity.MAJOR,
            affectedNetworks = networks,
            effects = listOf(
                WorldEventEffect(EffectType.NETWORK_SPEED_BOOST, -0.5, 300_000), // 50% slower
                WorldEventEffect(EffectType.TASK_REWARD_BOOST, 0.2, 300_000) // 20% more reward for patience
            ),
            rewardPool = 50000,
            rewardPerParticipant = 500
        ),
        WorldEvent(
            title = "Zero-Day Market Surge",
            description = "Black market flooded with fresh zero-day exploits. Prices dropped, availability increased.",
            eventType = WorldEventType.BLACK_MARKET_SALE,
            severity = WorldEventSeverity.MODERATE,
            affectedNetworks = listOf("172.16.0.0/16"),
            effects = listOf(
                WorldEventEffect(EffectType.DROP_RATE_INCREASE, 1.0, 180_000), // Double fragment drop rate
                WorldEventEffect(EffectType.XP_MULTIPLIER, 0.3, 180_000) // 30% more XP from fragment gathering
            ),
            rewardPool = 30000,
            rewardPerParticipant = 300
        ),
        WorldEvent(
            title = "ISP Security Crackdown",
            description = "Law enforcement has partnered with ISPs to increase monitoring and trace efforts.",
            eventType = WorldEventType.LAW_ENFORCEMENT_CRACKDOWN,
            severity = WorldEventSeverity.MAJOR,
            affectedNetworks = networks,
            effects = listOf(
                WorldEventEffect(EffectType.SECURITY_LEVEL_INCREASE, 1.0, 600_000), // +1 security level
                WorldEventEffect(EffectType.RESOURCE_REGEN_BOOST, -0.4, 600_000) // 40% slower resource regen
            ),
            rewardPool = 75000,
            rewardPerParticipant = 750,
            resolutionCriteria = "Reduce network visibility below 20% for 10 minutes"
        ),
        WorldEvent(
            title = "Quantum Computing Breakthrough",
            description = "Researchers announce practical quantum computing. Encryption standards shaken.",
            eventType = WorldEventType.TECHNOLOGY_BREAKTHROUGH,
            severity = WorldEventSeverity.CRITICAL,
            affectedNetworks = networks,
            effects = listOf(
                WorldEventEffect(EffectType.XP_MULTIPLIER, 1.0, 900_000), // Double XP
                WorldEventEffect(EffectType.CREDIT_MULTIPLIER, 0.5, 900_000), // 50% more credits
                WorldEventEffect(EffectType.DROP_RATE_INCREASE, 1.5, 900_000) // 2.5x fragment drop rate
            ),
            rewardPool = 100000,
            rewardPerParticipant = 1000
        ),
        WorldEvent(
            title = "Solar Flare Storm",
            description = "Massive solar flare disrupting satellite communications and power grids.",
            eventType = WorldEventType.SOLAR_FLARE,
            severity = WorldEventSeverity.MAJOR,
            affectedNetworks = listOf("10.0.4.0/24", "10.0.5.0/24"), // Military/satellite networks
            effects = listOf(
                WorldEventEffect(EffectType.NETWORK_SPEED_BOOST, -0.7, 120_000), // 70% slower
                WorldEventEffect(EffectType.RESOURCE_REGEN_BOOST, -0.6, 120_000) // 60% slower regen
            ),
            rewardPool = 40000,
            rewardPerParticipant = 400
        )
    )
}