package com.zeroday.service

import com.zeroday.model.EnhancedContract
import com.zeroday.model.MissionModifiers
import com.zeroday.model.ModifierType
import com.zeroday.model.TaskDifficulty
import com.zeroday.model.TaskTargetType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

class ContractGenerator(
    private val playerService: PlayerService
) {
    data class ContractTemplate(
        val id: String,
        val titleTemplate: String,
        val descriptionTemplate: String,
        val targetType: TaskTargetType,
        val suggestedCommands: List<String>
    )

    private val mutex = Mutex()

    private val contractTemplates = listOf(
        ContractTemplate("data_breach", "Data Breach: {company}",
            "Infiltrate {target} and exfiltrate the {data_type} database. {difficulty_note}",
            TaskTargetType.DATA_THEFT, listOf("exploit", "ssh", "cat")),
        ContractTemplate("pen_test", "Penetration Test: {company}",
            "Perform a full security audit of {target}. Identify and exploit vulnerabilities. {difficulty_note}",
            TaskTargetType.PENETRATION_TEST, listOf("nmap", "exploit", "backdoor")),
        ContractTemplate("ddos_job", "Service Disruption: {company}",
            "Take {target} offline for {duration} seconds. Coordinate with available resources. {difficulty_note}",
            TaskTargetType.SERVICE_DISRUPTION, listOf("overload", "botnet")),
        ContractTemplate("recon", "Reconnaissance: {target_net}",
            "Map out the {target_net} subnet. Identify all hosts, open ports, and running services. {difficulty_note}",
            TaskTargetType.NETWORK_RECON, listOf("nmap", "ping", "traceroute")),
        ContractTemplate("crypto_job", "Cryptographic Challenge: {company}",
            "Intercept and decrypt {data_type} communications from {target}. Multiple encryption layers. {difficulty_note}",
            TaskTargetType.CRYPTOGRAPHY, listOf("decrypt", "sniff", "crack")),
        ContractTemplate("cleanup", "Log Cleanup: {company}",
            "Erase all traces of {prev_attacker} intrusion from {target}. Remove logs and backdoors. {difficulty_note}",
            TaskTargetType.FORENSICS, listOf("firewall", "trace", "worm")),
        ContractTemplate("botnet_recruit", "Botnet Recruitment Drive",
            "Infect {node_count} systems in {target_net} with backdoor payload. Build your botnet. {difficulty_note}",
            TaskTargetType.BOTNET_OPERATION, listOf("backdoor", "botnet", "worm")),
        ContractTemplate("social_engineer", "Social Engineering: {company}",
            "Set up a phishing campaign targeting {company} employees. Harvest {credential_count} credentials. {difficulty_note}",
            TaskTargetType.SOCIAL_ENGINEERING, listOf("proxy", "spoof", "encrypt")),
        ContractTemplate("bug_bounty", "Bug Bounty: {company}",
            "Find and document {vuln_count} vulnerabilities in {target}. Submit responsible disclosure. {difficulty_note}",
            TaskTargetType.BUG_BOUNTY, listOf("sqlmap", "exploit", "nmap"))
    )

    private val companyNames = listOf(
        "NexusCorp", "ApexGlobal", "ByteDynamics", "QuantumLeap", "FusionTech",
        "OmniData", "StratoSys", "PulseNet", "VertexLogic", "CipherStack",
        "MatrixHoldings", "EchoSystems", "DeltaForce Tech", "Sigma Solutions",
        "Omega Labs", "Phoenix Cyber", "Titan Industries", "Aurora Networks"
    )

    private val dataTypes = listOf("customer", "employee", "financial", "medical", "research", "classified")
    private val difficultyNotes: Map<TaskDifficulty, String> = mapOf(
        TaskDifficulty.TRIVIAL to "A cakewalk. Even a script kiddie could do this.",
        TaskDifficulty.EASY to "Entry-level contract. Basic security measures.",
        TaskDifficulty.MEDIUM to "Standard corporate security. Proceed with caution.",
        TaskDifficulty.HARD to "Enterprise-grade defenses. Bring your A-game.",
        TaskDifficulty.EXPERT to "Military-grade encryption and monitoring. Extreme caution advised.",
        TaskDifficulty.LEGENDARY to "Three-letter-agency level. You didn't see this. Good luck."
    )

    suspend fun generateContract(playerId: String): EnhancedContract? = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock null

        val template = contractTemplates.random()
        val difficulty = rollDifficulty(player.level)
        val company = companyNames.random()
        val targetIp = generateTargetIp(player.level)

        val fillParams: Map<String, String> = mapOf(
            "company" to company,
            "target" to targetIp,
            "target_net" to "${targetIp.substringBeforeLast(".")}.0/24",
            "data_type" to dataTypes.random(),
            "duration" to "${(30 * difficulty.multiplier).toInt() + 10}",
            "node_count" to "${(5 * difficulty.multiplier).toInt() + 2}",
            "credential_count" to "${(5 * difficulty.multiplier).toInt()}",
            "vuln_count" to "${(2 * difficulty.multiplier).toInt().coerceAtLeast(2)}",
            "prev_attacker" to listOf("Anonymous", "GhostNet", "DarkOverlord", "LulzSec").random(),
            "difficulty_note" to (difficultyNotes[difficulty] ?: "")
        )

        val modifierCount = Random.nextInt(4)
        val modifiers = if (modifierCount > 0) {
            MissionModifiers.getRandomModifier(modifierCount)
        } else emptyList()

        var finalDifficulty = difficulty
        var rewardMultiplier = 1.0
        for (modifier in modifiers) {
            when (modifier.modifierType) {
                ModifierType.INCREASED_DIFFICULTY -> {
                    val currentIndex = finalDifficulty.ordinal
                    if (currentIndex < TaskDifficulty.entries.size - 1) {
                        finalDifficulty = TaskDifficulty.entries[currentIndex + 1]
                    }
                }
                ModifierType.DECREASED_DIFFICULTY -> {
                    val currentIndex = finalDifficulty.ordinal
                    if (currentIndex > 0) {
                        finalDifficulty = TaskDifficulty.entries[currentIndex - 1]
                    }
                }
                ModifierType.BOOSTED_REWARDS -> rewardMultiplier *= 1.5
                ModifierType.REDUCED_REWARDS -> rewardMultiplier *= 0.7
                ModifierType.SPEED_RUN -> rewardMultiplier *= 2.0
                ModifierType.TIME_LIMIT -> {}
                else -> {}
            }
        }

        val xpBase = (50 * difficulty.multiplier * player.level).toLong()
        val creditsBase = (100 * difficulty.multiplier * player.level).toLong()
        val finalXp = (xpBase * rewardMultiplier).toLong()
        val finalCredits = (creditsBase * rewardMultiplier).toLong()

        val title = fillTemplate(template.titleTemplate, fillParams)
        val description = fillTemplate(template.descriptionTemplate, fillParams)

        return@withLock EnhancedContract(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            targetType = template.targetType,
            targetIp = targetIp,
            requiredCommands = template.suggestedCommands,
            difficulty = finalDifficulty,
            suggestedLevel = player.level,
            baseXp = xpBase,
            baseCredits = creditsBase,
            modifiers = modifiers,
            finalRewardMultiplier = rewardMultiplier,
            finalXp = finalXp,
            finalCredits = finalCredits
        )
    }

    private fun rollDifficulty(playerLevel: Int): TaskDifficulty {
        val maxDiff = when {
            playerLevel < 3 -> 0
            playerLevel < 5 -> 1
            playerLevel < 7 -> 2
            playerLevel < 10 -> 3
            playerLevel < 14 -> 4
            else -> 5
        }
        val roll = Random.nextInt(maxDiff + 2)
        return TaskDifficulty.entries[roll.coerceAtMost(TaskDifficulty.entries.size - 1)]
    }

    private fun generateTargetIp(playerLevel: Int): String {
        val base = when {
            playerLevel < 3 -> "10.0.0"
            playerLevel < 5 -> "10.0.1"
            playerLevel < 7 -> "10.0.2"
            playerLevel < 10 -> "10.0.3"
            playerLevel < 14 -> "172.16.0"
            playerLevel < 18 -> "198.51.100"
            else -> "203.0.113"
        }
        return "$base.${Random.nextInt(2, 255)}"
    }

    private fun fillTemplate(template: String, params: Map<String, String>): String {
        var result = template
        params.forEach { (key, value) -> result = result.replace("{$key}", value) }
        return result
    }
}
