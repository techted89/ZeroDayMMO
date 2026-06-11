package com.zeroday.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Faction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tag: String,
    val description: String = "",
    val leaderId: String,
    val members: MutableList<String> = mutableListOf(),
    val joinRequests: MutableList<String> = mutableListOf(),
    val level: Int = 1,
    val experience: Long = 0,
    val mainframe: FactionMainframe = FactionMainframe(),
    val createdAt: Long = System.currentTimeMillis(),
    val isOpen: Boolean = true
)

@Serializable
data class FactionMainframe(
    var level: Int = 1,
    var cpuTotal: Long = 1000,
    var cpuUsed: Long = 0,
    var ramTotal: Long = 4096,
    var ramUsed: Long = 0,
    var bandwidthTotal: Long = 500,
    var bandwidthUsed: Long = 0,
    var creditsPool: Long = 0,
    var securityLevel: Int = 1,
    var passiveIncomeRate: Double = 1.0,
    var activeBuffs: MutableList<FactionBuff> = mutableListOf(),
    var unlockedFeatures: MutableList<String> = mutableListOf("faction_chat"),
    var upgradeProgress: MutableMap<String, Long> = mutableMapOf()
)

@Serializable
data class FactionBuff(
    val name: String,
    val description: String,
    val type: BuffType,
    val value: Double,
    val durationMs: Long,
    val expiresAt: Long = System.currentTimeMillis() + durationMs
)

@Serializable
enum class BuffType {
    EXPERIENCE_MULTIPLIER,
    CREDIT_MULTIPLIER,
    CPU_REGEN_BOOST,
    RAM_REGEN_BOOST,
    SCAN_RANGE_BOOST,
    FIREWALL_BOOST
}

@Serializable
data class FactionUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val category: UpgradeCategory,
    val levels: List<UpgradeLevel>
)

@Serializable
data class UpgradeLevel(
    val level: Int,
    val costCredits: Long,
    val costCpu: Long,
    val costRam: Long,
    val benefit: String,
    val value: Double,
    val requiredFactionLevel: Int = 1
)

@Serializable
enum class UpgradeCategory {
    CPU_CAPACITY,
    RAM_CAPACITY,
    BANDWIDTH_CAPACITY,
    SECURITY,
    PASSIVE_INCOME,
    BUFF_DURATION,
    MEMBER_LIMIT,
    FEATURE_UNLOCK
}

object FactionUpgradesRegistry {
    val allUpgrades: List<FactionUpgrade> = listOf(
        FactionUpgrade("cpu_cap", "CPU Core Cluster", "Expand mainframe processing power",
            UpgradeCategory.CPU_CAPACITY, listOf(
                UpgradeLevel(1, 5000, 0, 0, "+5000 CPU capacity", 5000.0),
                UpgradeLevel(2, 15000, 0, 0, "+10000 CPU capacity", 10000.0, 2),
                UpgradeLevel(3, 50000, 0, 0, "+25000 CPU capacity", 25000.0, 4),
                UpgradeLevel(4, 150000, 0, 0, "+50000 CPU capacity", 50000.0, 6)
            )),
        FactionUpgrade("ram_cap", "Memory Banks", "Upgrade faction RAM pool",
            UpgradeCategory.RAM_CAPACITY, listOf(
                UpgradeLevel(1, 5000, 0, 0, "+8GB RAM", 8192.0),
                UpgradeLevel(2, 15000, 0, 0, "+16GB RAM", 16384.0, 2),
                UpgradeLevel(3, 50000, 0, 0, "+32GB RAM", 32768.0, 4),
                UpgradeLevel(4, 150000, 0, 0, "+64GB RAM", 65536.0, 6)
            )),
        FactionUpgrade("bandwidth_cap", "Fiber Optic Link", "Increase faction bandwidth",
            UpgradeCategory.BANDWIDTH_CAPACITY, listOf(
                UpgradeLevel(1, 3000, 0, 0, "+500 Mbps", 500.0),
                UpgradeLevel(2, 10000, 0, 0, "+1000 Mbps", 1000.0, 2),
                UpgradeLevel(3, 30000, 0, 0, "+2500 Mbps", 2500.0, 4),
                UpgradeLevel(4, 100000, 0, 0, "+5000 Mbps", 5000.0, 6)
            )),
        FactionUpgrade("security", "ICE Security Suite", "Strengthen mainframe defenses",
            UpgradeCategory.SECURITY, listOf(
                UpgradeLevel(1, 2000, 1000, 512, "Security Level +1", 1.0),
                UpgradeLevel(2, 8000, 3000, 1024, "Security Level +2", 2.0, 2),
                UpgradeLevel(3, 25000, 10000, 4096, "Security Level +3", 3.0, 4),
                UpgradeLevel(4, 80000, 30000, 16384, "Security Level +5", 5.0, 6)
            )),
        FactionUpgrade("passive_income", "Automated Mining Rig", "Generate passive credits",
            UpgradeCategory.PASSIVE_INCOME, listOf(
                UpgradeLevel(1, 5000, 2000, 1024, "+2 credits/min per member", 2.0),
                UpgradeLevel(2, 20000, 8000, 4096, "+5 credits/min per member", 5.0, 3),
                UpgradeLevel(3, 60000, 25000, 16384, "+10 credits/min per member", 10.0, 5)
            )),
        FactionUpgrade("member_limit", "Member Expansion", "Increase faction member cap",
            UpgradeCategory.MEMBER_LIMIT, listOf(
                UpgradeLevel(1, 10000, 0, 0, "Max members: 15", 15.0),
                UpgradeLevel(2, 30000, 0, 0, "Max members: 25", 25.0, 3),
                UpgradeLevel(3, 100000, 0, 0, "Max members: 50", 50.0, 5)
            )),
        FactionUpgrade("feature_unlock", "Advanced Modules", "Unlock exclusive faction features",
            UpgradeCategory.FEATURE_UNLOCK, listOf(
                UpgradeLevel(1, 20000, 10000, 4096, "Unlocks: faction_scan", 1.0, 2),
                UpgradeLevel(2, 50000, 25000, 8192, "Unlocks: mass_ddos", 2.0, 4),
                UpgradeLevel(3, 150000, 50000, 32768, "Unlocks: faction_shield", 3.0, 6)
            ))
    )
}
