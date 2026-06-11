package com.zeroday.model

import kotlinx.serialization.Serializable

@Serializable
data class GameTask(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: TaskDifficulty,
    val requiredLevel: Int,
    val requiredCommands: List<String> = emptyList(),
    val targetType: TaskTargetType,
    val targetIp: String? = null,
    val objective: String,
    val timeLimitMs: Long = 300_000L,
    val rewards: TaskRewards,
    val isSoloable: Boolean = true,
    val maxPartySize: Int = 4,
    val tags: List<String> = emptyList()
)

@Serializable
enum class TaskDifficulty(val displayName: String, val color: String, val multiplier: Double) {
    TRIVIAL("Trivial", "#808080", 0.5),
    EASY("Easy", "#00FF00", 1.0),
    MEDIUM("Medium", "#FFFF00", 1.5),
    HARD("Hard", "#FF6600", 2.5),
    EXPERT("Expert", "#FF0000", 4.0),
    LEGENDARY("Legendary", "#FF00FF", 8.0)
}

@Serializable
enum class TaskTargetType {
    PENETRATION_TEST,
    DATA_THEFT,
    SERVICE_DISRUPTION,
    DEFENSE_SETUP,
    CRYPTOGRAPHY,
    SOCIAL_ENGINEERING,
    BOTNET_OPERATION,
    NETWORK_RECON,
    FORENSICS,
    BUG_BOUNTY
}

@Serializable
data class TaskRewards(
    val experience: Long = 0,
    val credits: Long = 0,
    val reputation: Int = 0,
    val cpuUpgrade: Int = 0,
    val ramUpgrade: Int = 0,
    val bandwidthUpgrade: Int = 0,
    val commandUnlock: String? = null
)

@Serializable
data class TaskInstance(
    val taskId: String,
    val instanceId: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val difficulty: TaskDifficulty,
    val targetType: TaskTargetType,
    val targetIp: String?,
    val objective: String,
    val timeLimitMs: Long,
    val rewards: TaskRewards,
    val createdAt: Long = System.currentTimeMillis(),
    var claimedBy: List<String> = emptyList(),
    var status: TaskStatus = TaskStatus.AVAILABLE,
    var completedBy: String? = null
)

@Serializable
enum class TaskStatus {
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED
}

fun TaskInstance.toMap(): Map<String, Any?> = mapOf(
    "taskId" to taskId,
    "instanceId" to instanceId,
    "title" to title,
    "description" to description,
    "difficulty" to difficulty.name,
    "targetType" to targetType.name,
    "targetIp" to targetIp,
    "objective" to objective,
    "timeLimitMs" to timeLimitMs,
    "status" to status.name,
    "rewards" to mapOf(
        "experience" to rewards.experience,
        "credits" to rewards.credits,
        "reputation" to rewards.reputation
    )
)

object TaskTemplates {
    val taskPool: List<GameTask> = listOf(
        GameTask("t1", "Scan Corporate Network", "Perform a full reconnaissance scan of the target corporate network and identify all active hosts.",
            TaskDifficulty.EASY, 2, listOf("nmap"), TaskTargetType.NETWORK_RECON, "10.0.1.0/24",
            "Identify all hosts on the subnet", 300_000L, TaskRewards(80, 200, 10, 5, 10, 5)),

        GameTask("t2", "Extract Customer Database", "Breach the target server and exfiltrate the customer database.",
            TaskDifficulty.MEDIUM, 5, listOf("exploit", "ssh"), TaskTargetType.DATA_THEFT, "10.0.2.50",
            "Locate and cat the database file", 600_000L, TaskRewards(300, 800, 25, 15, 25, 10)),

        GameTask("t3", "Deploy Ransomware Simulation", "Simulate a ransomware attack by encrypting files on target and leaving a note.",
            TaskDifficulty.HARD, 8, listOf("exploit", "backdoor", "encrypt"), TaskTargetType.SERVICE_DISRUPTION, "10.0.3.100",
            "Encrypt 3 critical files", 900_000L, TaskRewards(800, 2000, 50, 25, 40, 20)),

        GameTask("t4", "Botnet Recruitment Drive", "Infect 20 systems across the darknet with a backdoor and add them to your botnet.",
            TaskDifficulty.HARD, 12, listOf("backdoor", "botnet"), TaskTargetType.BOTNET_OPERATION, null,
            "Build a botnet of 20+ nodes", 1_200_000L, TaskRewards(1500, 4000, 100, 40, 60, 30)),

        GameTask("t5", "Crack Government Cipher", "Decrypt intercepted government communications. Multiple layers of encryption.",
            TaskDifficulty.EXPERT, 14, listOf("decrypt", "crack"), TaskTargetType.CRYPTOGRAPHY, null,
            "Decrypt all 5 message layers", 1_500_000L, TaskRewards(3000, 8000, 200, 50, 80, 40)),

        GameTask("t6", "Zero-Day Auction Hijack", "Intercept a zero-day exploit auction on the darknet and steal the payload.",
            TaskDifficulty.LEGENDARY, 18, listOf("sniff", "spoof", "zero-day"), TaskTargetType.DATA_THEFT, "203.0.113.50",
            "Steal the zero-day payload before the auction ends", 1_800_000L,
            TaskRewards(8000, 25000, 500, 100, 150, 60, "zero-day")),

        GameTask("t7", "Defend Against APT", "A sophisticated attacker is targeting your infrastructure. Set up defenses and trace them.",
            TaskDifficulty.MEDIUM, 9, listOf("firewall", "trace", "honeypot"), TaskTargetType.DEFENSE_SETUP, null,
            "Block 50+ attack attempts and trace the source", 600_000L, TaskRewards(500, 1500, 40, 20, 30, 15)),

        GameTask("t8", "Darknet Forum Heist", "Extract user credentials from a darknet forum database.",
            TaskDifficulty.HARD, 10, listOf("sqlmap", "exploit"), TaskTargetType.DATA_THEFT, "172.16.0.50",
            "Dump the forum user table", 900_000L, TaskRewards(1200, 3500, 80, 30, 50, 25)),

        GameTask("t9", "Fireside Chat Intercept", "Intercept VoIP communications between two corporate executives.",
            TaskDifficulty.EXPERT, 15, listOf("sniff", "decrypt", "spoof"), TaskTargetType.NETWORK_RECON, "10.0.5.100",
            "Capture and decrypt 5 VoIP packets", 1_200_000L, TaskRewards(4000, 10000, 250, 60, 100, 40)),

        GameTask("t10", "Worm Containment", "A worm is spreading through the network. Create a vaccine and contain it.",
            TaskDifficulty.EXPERT, 16, listOf("firewall", "trace", "worm"), TaskTargetType.FORENSICS, null,
            "Contain the worm and reverse-engineer its signature", 1_500_000L, TaskRewards(6000, 15000, 300, 80, 120, 50)),

        GameTask("t11", "Simple Data Grab", "Grab a file from an unsecured server. Practice target.",
            TaskDifficulty.TRIVIAL, 1, listOf(), TaskTargetType.DATA_THEFT, "10.0.0.5",
            "Connect and cat flag.txt", 180_000L, TaskRewards(30, 50, 5, 2, 5, 2)),

        GameTask("t12", "Brute Force Challenge", "Brute force the admin panel of a local business server.",
            TaskDifficulty.EASY, 5, listOf("bruteforce"), TaskTargetType.PENETRATION_TEST, "10.0.1.200",
            "Find admin credentials via brute force", 300_000L, TaskRewards(150, 400, 15, 10, 15, 5)),

        GameTask("t13", "Phishing Campaign", "Set up a phishing page and harvest credentials from corporate employees.",
            TaskDifficulty.MEDIUM, 7, listOf("proxy", "spoof"), TaskTargetType.SOCIAL_ENGINEERING, null,
            "Collect 10 sets of credentials", 600_000L, TaskRewards(400, 1000, 30, 15, 25, 10)),

        GameTask("t14", "DDoS Coordination", "Coordinate a distributed denial of service attack using your botnet.",
            TaskDifficulty.HARD, 13, listOf("botnet", "overload"), TaskTargetType.SERVICE_DISRUPTION, "198.51.100.10",
            "Take the target offline for 60 seconds", 1_000_000L, TaskRewards(2000, 5000, 120, 35, 50, 25)),

        GameTask("t15", "Bug Bounty: E-commerce", "Find and exploit 3 vulnerabilities in an e-commerce platform.",
            TaskDifficulty.MEDIUM, 8, listOf("sqlmap", "exploit"), TaskTargetType.BUG_BOUNTY, "10.0.3.200",
            "Find SQLi, XSS, and LFI vulnerabilities", 900_000L, TaskRewards(600, 2000, 60, 20, 30, 15))
    )
}
