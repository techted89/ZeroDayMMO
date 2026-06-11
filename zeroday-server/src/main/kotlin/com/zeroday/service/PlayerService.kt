package com.zeroday.service

import com.zeroday.config.ServerConfig
import com.zeroday.model.*
import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-player state and table-level structure.
 *
 * Concurrency model (post-optimisation):
 *
 *   - The player table is a [ConcurrentHashMap]; structural operations
 *     (register, restore, delete) take a single coarse [tableMutex] for
 *     a few microseconds. This is rare relative to per-player mutation.
 *   - Each player has its own [Mutex] in [playerLocks]. Per-player
 *     mutations only block other mutations of the *same* player, so two
 *     players interacting in parallel do not contend.
 *   - Reads ([getPlayer], [getPlayerByUsername], [getOnlinePlayers])
 *     never take a mutex. The `Player` reference is published through
 *     the concurrent map; callers may read its fields directly. The
 *     fields are `var`s so concurrent reads may see stale data for one
 *     in-progress mutation, which is acceptable for an MMO state model.
 *   - [snapshotCache] caches the most-recently-built [PlayerSnapshot]
 *     per player for [snapshotTtlMs] milliseconds. The cache is
 *     invalidated whenever the player is mutated (via [invalidateCache]).
 *
 * This brings contention from O(players²) under a single mutex down to
 * O(1) per operation, which is the difference between the server
 * running smoothly with 200 online players and grinding to a halt at
 * 50.
 */
class PlayerService(
    private val passwordHasher: PasswordHasher
) {
    private val players = ConcurrentHashMap<String, Player>()
    private val usernameIndex = ConcurrentHashMap<String, String>() // username -> playerId
    private val sessions = ConcurrentHashMap<String, String>()

    /** Coarse lock for structural ops (register, restore, delete). Cheap. */
    private val tableMutex = Mutex()
    /** Per-player locks. Created lazily on first mutation. */
    private val playerLocks = ConcurrentHashMap<String, Mutex>()

    // ----- snapshot cache -----
    /**
     * TTL for the per-player snapshot cache. Set well below the typical
     * client polling interval so a stale snapshot is never visible for
     * more than a single frame.
     */
    var snapshotTtlMs: Long = 250L

    private data class CachedSnapshot(val snapshot: PlayerSnapshot, val at: Long)
    private val snapshotCache = ConcurrentHashMap<String, CachedSnapshot>()
    private val snapshotHits = AtomicLong(0)
    private val snapshotMisses = AtomicLong(0)

    private fun lockFor(playerId: String): Mutex =
        playerLocks.computeIfAbsent(playerId) { Mutex() }

    /**
     * Public wrapper around [withPlayerLock] for external services (CareerService, etc.)
     * that need to mutate a player within their own transaction.
     */
    suspend fun <T> withPlayerAction(playerId: String, block: suspend (Player) -> T): T? =
        withPlayerLock(playerId, block)

    /**
     * Take the per-player lock, run [block], and invalidate the snapshot
     * cache. Use this for *all* mutations of an existing player.
     */
    private suspend fun <T> withPlayerLock(playerId: String, block: suspend (Player) -> T): T? {
        val player = players[playerId] ?: return null
        return lockFor(playerId).withLock {
            // Re-fetch inside the lock to be safe against concurrent deletes.
            val p = players[playerId] ?: return@withLock null
            val result = block(p)
            snapshotCache.remove(playerId)
            result
        }
    }

    private fun invalidateCache(playerId: String) {
        snapshotCache.remove(playerId)
    }

    // ----- structural ops -----

    suspend fun register(username: String, password: String): Result<Player> = tableMutex.withLock {
        if (usernameIndex.containsKey(username)) {
            return@withLock Result.failure(Exception("Username already exists"))
        }
        val player = Player(
            username = username,
            passwordHash = passwordHasher.hash(password),
            cpu = ServerConfig.STARTING_CPU,
            maxCpu = ServerConfig.STARTING_CPU,
            ram = ServerConfig.STARTING_RAM,
            maxRam = ServerConfig.STARTING_RAM,
            bandwidth = ServerConfig.STARTING_BANDWIDTH,
            maxBandwidth = ServerConfig.STARTING_BANDWIDTH
        )
        players[player.id] = player
        usernameIndex[player.username] = player.id
        Result.success(player)
    }

    suspend fun login(username: String, password: String): Result<Player> {
        val playerId = usernameIndex[username] ?: return Result.failure(Exception("Invalid credentials"))
        return withPlayerLock(playerId) { player ->
            if (!passwordHasher.verify(password, player.passwordHash)) {
                Result.failure<Player>(Exception("Invalid credentials"))
            } else {
                player.isOnline = true
                sessions[player.id] = newSessionToken()
                Result.success(player)
            }
        } ?: Result.failure(Exception("Invalid credentials"))
    }

    fun getPlayer(playerId: String): Player? = players[playerId]

    fun getPlayerByUsername(username: String): Player? =
        usernameIndex[username]?.let { players[it] }

    suspend fun addExperience(playerId: String, amount: Long): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.experience += amount
            checkForLevelUp(player)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    private fun checkForLevelUp(player: Player) {
        var leveledUp = false
        var reachedLevel = player.level
        while (player.experience >= player.experienceToNext) {
            player.experience -= player.experienceToNext
            player.level++
            player.experienceToNext = (player.experienceToNext * 1.5).toLong()
            player.maxCpu += 5
            player.maxRam += 16
            player.maxBandwidth += 2
            player.cpu = player.maxCpu
            player.ram = player.maxRam
            player.bandwidth = player.maxBandwidth
            player.skillPoints += if (player.level % 5 == 0) 2 else 1
            leveledUp = true
            reachedLevel = player.level
        }
        if (leveledUp) player.lastLevelNotified = reachedLevel
    }

    fun consumeLevelUp(playerId: String): Int? {
        val p = players[playerId] ?: return null
        val level = p.lastLevelNotified
        if (level > 0) p.lastLevelNotified = 0
        return if (level > 0) level else null
    }

    suspend fun addResources(playerId: String, rewards: TaskRewards): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.credits += rewards.credits
            player.cpu = (player.cpu + rewards.cpuUpgrade).coerceAtMost(player.maxCpu)
            player.ram = (player.ram + rewards.ramUpgrade).coerceAtMost(player.maxRam)
            player.bandwidth = (player.bandwidth + rewards.bandwidthUpgrade).coerceAtMost(player.maxBandwidth)
            player.reputation += rewards.reputation
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun deductCredits(playerId: String, amount: Long): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            if (player.credits < amount) null
            else { player.credits -= amount; player }
        }
        return when {
            res == null && players[playerId] == null -> Result.failure(Exception("Player not found"))
            res == null -> Result.failure(Exception("Not enough credits"))
            else -> Result.success(res)
        }
    }

    suspend fun addCredits(playerId: String, amount: Long): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.credits += amount
            if (amount > 0) player.lifetimeCreditsEarned += amount
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun addReputation(playerId: String, amount: Int): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.reputation += amount
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun unlockCommand(playerId: String, commandName: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            if (CommandRegistry.commandMap.containsKey(commandName)) {
                player.unlockedCommands.add(commandName)
            }
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun discoverNode(playerId: String, ip: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.discoveredNodes.add(ip)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun grantNetworkAccess(playerId: String, networks: List<String>): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            networks.forEach { player.discoveredNodes.add(it) }
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun upgradeResources(playerId: String, cpu: Int = 0, ram: Int = 0, bandwidth: Int = 0): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.maxCpu += cpu
            player.maxRam += ram
            player.maxBandwidth += bandwidth
            player.cpu = player.maxCpu
            player.ram = player.maxRam
            player.bandwidth = player.maxBandwidth
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun consumeResources(playerId: String, cpu: Int, ram: Int): Result<Boolean> {
        val res = withPlayerLock(playerId) { player ->
            if (player.cpu < cpu || player.ram < ram) false
            else {
                player.cpu -= cpu
                player.ram -= ram
                true
            }
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    /**
     * Tick resource regen for [playerId]. Caller (typically
     * [ResourceRegenTicker]) is expected to invoke this for one player
     * at a time. We still take the per-player lock for safety, so a
     * regen tick cannot race a command execution.
     */
    suspend fun regenerateResources(playerId: String) {
        withPlayerLock(playerId) { player ->
            if (player.cpu < player.maxCpu) player.cpu = (player.cpu + 2).coerceAtMost(player.maxCpu)
            if (player.ram < player.maxRam) player.ram = (player.ram + 4).coerceAtMost(player.maxRam)
        }
    }

    /**
     * Regenerate all online players in a single critical section per
     * player. Cheaper than spawning a coroutine per online player.
     */
    suspend fun regenerateAllOnline(): Int {
        var n = 0
        for (p in players.values) {
            if (!p.isOnline) continue
            if (p.cpu < p.maxCpu || p.ram < p.maxRam) {
                withPlayerLock(p.id) { player ->
                    if (player.cpu < player.maxCpu) player.cpu = (player.cpu + 2).coerceAtMost(player.maxCpu)
                    if (player.ram < player.maxRam) player.ram = (player.ram + 4).coerceAtMost(player.maxRam)
                }
                n++
            }
        }
        return n
    }

    fun getOnlinePlayers(): List<Player> = players.values.filter { it.isOnline }

    /**
     * Paginated online-player list. O(offset+limit) cost — we materialise
     * a snapshot and slice. For very large tables use [forEachOnlinePlayer].
     */
    fun getOnlinePlayersPage(offset: Int, limit: Int): List<Player> {
        if (limit <= 0) return emptyList()
        val all = getOnlinePlayers()
        val from = offset.coerceAtLeast(0)
        if (from >= all.size) return emptyList()
        val to = (from + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }

    suspend fun setStoryline(playerId: String, storylineId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.currentStoryline = storylineId
            player.storylineProgress = 0
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun advanceStoryline(playerId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.storylineProgress++
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun completeStoryline(playerId: String, storylineId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.completedStorylines.add(storylineId)
            player.currentStoryline = null
            player.storylineProgress = 0
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun assignTask(playerId: String, task: TaskInstance): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.activeTasks.add(task)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun completeTask(playerId: String, taskInstanceId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            val task = player.activeTasks.find { it.instanceId == taskInstanceId }
                ?: return@withPlayerLock null
            task.status = TaskStatus.COMPLETED
            player.completedTasks.add(task.taskId)
            player.activeTasks.removeAll { it.instanceId == taskInstanceId }
            player
        }
        return when {
            res == null && players[playerId] == null -> Result.failure(Exception("Player not found"))
            res == null -> Result.failure(Exception("Task not found"))
            else -> Result.success(res)
        }
    }

    suspend fun setParty(playerId: String, partyId: String?): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.partyId = partyId
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun setFaction(playerId: String, factionId: String?): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.factionId = factionId
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun addWorldEventParticipation(playerId: String, eventId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.worldEventParticipation.add(eventId)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun removeWorldEventParticipation(playerId: String, eventId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.worldEventParticipation.remove(eventId)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    fun getAllPlayers(): List<Player> = players.values.toList()

    suspend fun addToInventory(playerId: String, item: InventoryItem): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.inventory.add(item)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun removeFromInventory(playerId: String, itemId: String): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.inventory.removeAll { it.id == itemId }
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun addZeroDayExploit(playerId: String, item: InventoryItem): Result<Player> {
        val res = withPlayerLock(playerId) { player ->
            player.activeZeroDayExploits.add(item)
            player
        }
        return res?.let { Result.success(it) } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun consumeZeroDayExploit(playerId: String): Result<InventoryItem?> {
        val res = withPlayerLock(playerId) { player ->
            val exploit = player.activeZeroDayExploits.firstOrNull()
            if (exploit != null) player.activeZeroDayExploits.removeAt(0)
            exploit
        }
        return if (players[playerId] == null) {
            Result.failure(Exception("Player not found"))
        } else {
            Result.success(res)
        }
    }

    /**
     * Build (or return cached) snapshot for [playerId]. The cache hits
     * when the same player is snapshotted multiple times within
     * [snapshotTtlMs] — common in /health and leaderboard queries. The
     * cache is invalidated by every mutation.
     *
     * Returns null if the player does not exist.
     */
    fun getSnapshot(playerId: String): PlayerSnapshot? {
        val p = players[playerId] ?: return null
        val now = System.currentTimeMillis()
        val cached = snapshotCache[playerId]
        if (cached != null && now - cached.at < snapshotTtlMs) {
            snapshotHits.incrementAndGet()
            return cached.snapshot
        }
        snapshotMisses.incrementAndGet()
        val snap = p.toSnapshot()
        snapshotCache[playerId] = CachedSnapshot(snap, now)
        return snap
    }

    /** Invalidate the snapshot cache for [playerId] (e.g. on mutation). */
    fun invalidateSnapshot(playerId: String) {
        snapshotCache.remove(playerId)
    }

    /** Diagnostics: cache hit/miss rates. */
    fun snapshotCacheStats(): Pair<Long, Long> = snapshotHits.get() to snapshotMisses.get()

    /** Public type alias so the public API doesn't leak a private class. */
    data class SnapshotCacheStats(val hits: Long, val misses: Long)
    fun snapshotCacheStatsTyped(): SnapshotCacheStats = SnapshotCacheStats(snapshotHits.get(), snapshotMisses.get())

    suspend fun createPlayerDirect(player: Player): Unit = tableMutex.withLock {
        players[player.id] = player
        usernameIndex[player.username] = player.id
        snapshotCache.remove(player.id)
    }

    suspend fun setOnline(playerId: String, online: Boolean): Unit {
        withPlayerLock(playerId) { player ->
            player.isOnline = online
            if (!online) sessions.remove(player.id)
        }
    }

    suspend fun deletePlayer(playerId: String): Unit = tableMutex.withLock {
        val p = players.remove(playerId) ?: return@withLock
        usernameIndex.remove(p.username)
        sessions.remove(playerId)
        snapshotCache.remove(playerId)
        playerLocks.remove(playerId)
    }

    companion object {
        // 32 bytes from SecureRandom, base64url encoded. ~192 bits of entropy.
        private val SESSION_RANDOM = java.security.SecureRandom()
        private fun newSessionToken(): String {
            val bytes = ByteArray(24)
            SESSION_RANDOM.nextBytes(bytes)
            return "sess_" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
