package com.zeroday.service

import com.zeroday.model.CommandRegistry
import com.zeroday.zdscript.NexusScript
import com.zeroday.zdscript.NexusScriptEngine
import com.zeroday.zdscript.ScriptContext
import com.zeroday.zdscript.NexusValue
import com.zeroday.zdscript.KnowledgeFragment
import com.zeroday.zdscript.KnowledgeCategory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HackToLearnService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val engine = NexusScriptEngine()
    private val playerKnowledge = mutableMapOf<String, MutableSet<String>>()
    private val playerScripts = mutableMapOf<String, MutableList<NexusScript>>()
    private val playerDiscoveredCommands = mutableMapOf<String, MutableSet<String>>()
    private val mutex = Mutex()

    suspend fun initializePlayer(playerId: String) = mutex.withLock {
        playerKnowledge.getOrPut(playerId) { mutableSetOf() }
        playerScripts.getOrPut(playerId) { mutableListOf() }
        playerDiscoveredCommands.getOrPut(playerId) { mutableSetOf() }
    }

    suspend fun learnKnowledge(playerId: String, fragmentId: String): Boolean = mutex.withLock {
        val knowledge = playerKnowledge.getOrPut(playerId) { mutableSetOf() }
        knowledge.add(fragmentId)
    }

    suspend fun getPlayerKnowledge(playerId: String): Set<String> = mutex.withLock {
        playerKnowledge[playerId]?.toSet() ?: emptySet()
    }

    suspend fun validateScript(code: String): NexusScriptEngine.ValidationResult =
        engine.validate(code)

    suspend fun saveScript(playerId: String, name: String, code: String, description: String): Result<NexusScript> = mutex.withLock {
        val validation = engine.validate(code)
        if (!validation.valid) {
            return@withLock Result.failure(Exception("Script validation failed: ${validation.errors.joinToString("; ")}"))
        }
        val script = NexusScript(
            name = name,
            authorId = playerId,
            code = code,
            description = description,
            validated = true
        )
        playerScripts.getOrPut(playerId) { mutableListOf() }.add(script)
        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.SCRIPT_SAVED, 1L))
        }
        Result.success(script)
    }

    suspend fun getPlayerScripts(playerId: String): List<NexusScript> = mutex.withLock {
        playerScripts[playerId]?.toList() ?: emptyList()
    }

    suspend fun deleteScript(playerId: String, scriptId: String): Result<Unit> = mutex.withLock {
        val removed = playerScripts[playerId]?.removeAll { it.id == scriptId } ?: false
        if (removed) Result.success(Unit)
        else Result.failure(Exception("Script not found"))
    }

    suspend fun discoverFragment(playerId: String, fragmentId: String): Result<KnowledgeFragment> = mutex.withLock {
        val fragment = knowledgeFragments.find { it.id == fragmentId }
            ?: return@withLock Result.failure(Exception("Knowledge fragment not found"))

        val known = playerKnowledge.getOrPut(playerId) { mutableSetOf() }
        if (fragmentId in known) {
            return@withLock Result.failure(Exception("Already discovered this fragment"))
        }
        if (!fragment.requiredFragments.all { it in known }) {
            return@withLock Result.failure(Exception("Required fragments: ${fragment.requiredFragments.joinToString(", ")}"))
        }
        known.add(fragmentId)

        fragment.unlocksCommand?.let { cmd ->
            playerDiscoveredCommands.getOrPut(playerId) { mutableSetOf() }.add(cmd)
            playerService.unlockCommand(playerId, cmd)
        }
        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.FRAGMENT_GATHERED, 1L))
        }
        Result.success(fragment)
    }

    suspend fun getAvailableFragments(playerId: String): List<KnowledgeFragment> = mutex.withLock {
        val known = playerKnowledge.getOrPut(playerId) { mutableSetOf() }
        knowledgeFragments.filter { f ->
            f.id !in known && f.requiredFragments.all { it in known }
        }
    }

    suspend fun getKnowledgeMap(playerId: String): KnowledgeMapData = mutex.withLock {
        val known = playerKnowledge.getOrPut(playerId) { mutableSetOf() }
        val discoveredCmds = playerDiscoveredCommands.getOrPut(playerId) { mutableSetOf() }

        KnowledgeMapData(
            totalFragments = knowledgeFragments.size,
            discoveredFragments = known.size,
            unlockedCommands = discoveredCmds.size,
            totalCommands = CommandRegistry.allCommands.size,
            fragments = knowledgeFragments.map { f ->
                FragmentStatus(
                    id = f.id, name = f.name, description = f.description,
                    category = f.category.name, rarity = f.rarity,
                    unlocked = f.id in known,
                    available = f.id !in known && f.requiredFragments.all { it in known },
                    requiredFragments = f.requiredFragments,
                    unlocksCommand = f.unlocksCommand,
                    sourceHint = if (f.id in known) "" else f.sourceHint
                )
            }
        )
    }

    suspend fun runCode(playerId: String, code: String): Result<NexusScriptEngine.ScriptResult> = mutex.withLock {
        val validation = engine.validate(code)
        if (!validation.valid) {
            return@withLock Result.failure(Exception(validation.errors.joinToString("; ")))
        }
        val player = playerService.getPlayer(playerId)
            ?: return@withLock Result.failure(Exception("Player not found"))
        val context = ScriptContext(playerId, player.level)
        val script = NexusScript(name = "inline", authorId = playerId, code = code, validated = true)
        val result = engine.execute(script, context)
        if (result.success) Result.success(result)
        else Result.failure(Exception(result.error))
    }

    suspend fun executeScript(playerId: String, scriptId: String, scriptArgs: Map<String, String> = emptyMap()): Result<NexusScriptEngine.ScriptResult> = mutex.withLock {
        val script = playerScripts[playerId]?.find { it.id == scriptId }
            ?: return@withLock Result.failure(Exception("Script not found"))
        val player = playerService.getPlayer(playerId)
            ?: return@withLock Result.failure(Exception("Player not found"))
        val context = ScriptContext(playerId, player.level)
        scriptArgs.forEach { (k, v) -> context.variables[k] = NexusValue.StrVal(v) }
        val result = engine.execute(script, context)
        if (result.success) Result.success(result)
        else Result.failure(Exception(result.error))
    }

    companion object {
        private val knowledgeFragments: List<KnowledgeFragment> = listOf(
            KnowledgeFragment("recon_basics", "Recon Basics", "Foundational reconnaissance techniques",
                KnowledgeCategory.RECONNAISSANCE, listOf(), "ping", "common", "Run 'ping' or 'scan' to discover this"),
            KnowledgeFragment("recon_advanced", "Advanced Recon", "Deep network probing and enumeration",
                KnowledgeCategory.RECONNAISSANCE, listOf("recon_basics"), "nmap", "uncommon", "Run 'nmap' on a target"),
            KnowledgeFragment("exploit_basics", "Exploit Fundamentals", "Basic exploitation techniques",
                KnowledgeCategory.EXPLOITATION, listOf("recon_advanced"), "exploit", "rare", "Run 'exploit' on a target"),
            KnowledgeFragment("crypto_basics", "Crypto Foundations", "Understanding encryption and hashing",
                KnowledgeCategory.CRYPTOGRAPHY, listOf(), "decrypt", "common", "Run 'decrypt' on a file"),
            KnowledgeFragment("crypto_advanced", "Crypto Mastery", "Breaking strong encryption",
                KnowledgeCategory.CRYPTOGRAPHY, listOf("crypto_basics"), "crack", "epic", "Run 'crack' on a hash"),
            KnowledgeFragment("defense_basics", "Defense Foundations", "Hardening systems against attack",
                KnowledgeCategory.DEFENSE, listOf(), "firewall", "uncommon", "Run 'firewall' command"),
            KnowledgeFragment("stealth_basics", "Stealth Tactics", "Avoiding detection",
                KnowledgeCategory.STEALTH, listOf(), "spoof", "uncommon", "Run 'spoof' command"),
            KnowledgeFragment("stealth_advanced", "Ghost Protocol", "Become invisible on the wire",
                KnowledgeCategory.STEALTH, listOf("stealth_basics"), "proxy", "rare", "Chain 'proxy' for anonymity"),
            KnowledgeFragment("network_basics", "Network Fundamentals", "Understanding the network layer",
                KnowledgeCategory.NETWORKING, listOf(), "ifconfig", "common", "Run 'ifconfig'"),
            KnowledgeFragment("persistence", "Persistence Techniques", "Maintaining access to a system",
                KnowledgeCategory.EXPLOITATION, listOf("exploit_basics"), "backdoor", "epic", "Run 'backdoor' on a target"),
            KnowledgeFragment("propagation", "Worm Propagation", "Self-replicating payloads",
                KnowledgeCategory.EXPLOITATION, listOf("persistence"), "worm", "legendary", "Run 'worm' with --spread"),
            KnowledgeFragment("ai_basics", "AI Assistance", "Leveraging AI for hacking",
                KnowledgeCategory.GENERAL, listOf("crypto_advanced", "stealth_advanced"), "ai-assist", "legendary", "Run 'ai-assist'")
        )
    }
}

data class KnowledgeMapData(
    val totalFragments: Int,
    val discoveredFragments: Int,
    val unlockedCommands: Int,
    val totalCommands: Int,
    val fragments: List<FragmentStatus>
)

data class FragmentStatus(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val unlocked: Boolean,
    val available: Boolean,
    val requiredFragments: List<String>,
    val unlocksCommand: String?,
    val sourceHint: String
)
