package com.zeroday.service

import com.zeroday.model.Player
import com.zeroday.security.PasswordHasher
import com.zeroday.util.AppScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicLong

/**
 * Periodic JSON snapshot of the in-memory player table.
 *
 * Why a periodic snapshot and not "write on every mutation"?
 *   - Player state changes are bursty (a single `task_completed` can fire
 *     a dozen field updates). Writing on every mutation is I/O bound and
 *     would also need careful ordering to avoid torn writes.
 *   - A crash mid-session is acceptable: the next snapshot is at most
 *     [intervalMs] stale. The `inventory` / `credits` fields are also
 *     restored by replaying the event bus for any in-flight operation.
 *
 * Files written:
 *   - `$dir/players.json`             — main snapshot
 *   - `$dir/players.json.bak`         — last-known-good, used as fallback
 *
 * The write is atomic: we write to a temp file in the same directory and
 * rename it over the live snapshot, so a crash mid-write cannot leave a
 * truncated file.
 */
class PlayerPersistence(
    private val playerService: PlayerService,
    private val directory: File,
    val intervalMs: Long = 60_000L
) {
    private val log = LoggerFactory.getLogger(PlayerPersistence::class.java)
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    private val serializer = ListSerializer(Player.serializer())
    private val lastSavedCount = AtomicLong(0)
    @Volatile private var lastSnapshotAt: Long = 0
    @Volatile private var lastRestoreAt: Long = 0
    @Volatile private var lastRestoreCount: Int = 0

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun snapshotFile(): File = File(directory, "players.json")
    private fun backupFile(): File = File(directory, "players.json.bak")

    /**
     * Write the current player table to disk atomically. Returns the
     * number of players saved. Safe to call from any coroutine.
     */
    suspend fun saveNow(): Int = saveAll()

    private suspend fun saveAll(): Int {
        val players = playerService.getAllPlayers()
        val tmp = File(directory, "players.json.tmp")
        try {
            tmp.writeText(json.encodeToString(serializer, players))
            Files.move(
                tmp.toPath(),
                snapshotFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            lastSavedCount.set(players.size.toLong())
            lastSnapshotAt = System.currentTimeMillis()
            log.debug("Player snapshot saved: count={} -> {}", players.size, snapshotFile().absolutePath)
            return players.size
        } catch (e: Exception) {
            log.error("Failed to write player snapshot", e)
            runCatching { tmp.delete() }
            return -1
        }
    }

    /**
     * Restore the player table from disk. Players are inserted with
     * `createPlayerDirect` so any existing in-memory record for the same
     * id is replaced (this is the right behaviour on boot — the disk is
     * authoritative for offline players). Returns the number of players
     * restored, or 0 if no snapshot was present.
     */
    suspend fun restore(): Int {
        val src = snapshotFile().takeIf { it.exists() } ?: backupFile().takeIf { it.exists() }
            ?: run {
                log.info("No player snapshot found in {}; starting empty", directory.absolutePath)
                return 0
            }
        return try {
            val players: List<Player> = json.decodeFromString(serializer, src.readText())
            for (p in players) {
                // Reset transient fields
                p.isOnline = false
                p.lastLevelNotified = 0
                playerService.createPlayerDirect(p)
            }
            lastRestoreCount = players.size
            lastRestoreAt = System.currentTimeMillis()
            log.info("Player snapshot restored: count={} source={}", players.size, src.name)
            players.size
        } catch (e: Exception) {
            log.error("Failed to restore player snapshot from {}", src.absolutePath, e)
            0
        }
    }

    /**
     * Schedule a background coroutine that snapshots the table every
     * [intervalMs] milliseconds. Returns immediately. The loop is bound
     * to [AppScope] so it dies with the JVM.
     */
    fun startBackgroundFlush() {
        AppScope.scope.launch {
            while (isActive) {
                delay(intervalMs)
                runCatching { saveAll() }
            }
        }
        log.info("Player persistence started: interval={}ms dir={}", intervalMs, directory.absolutePath)
    }

    fun status(): Map<String, Any?> = mapOf(
        "directory" to directory.absolutePath,
        "intervalMs" to intervalMs,
        "lastSnapshotAt" to lastSnapshotAt,
        "lastRestoreAt" to lastRestoreAt,
        "lastSavedCount" to lastSavedCount.get(),
        "lastRestoreCount" to lastRestoreCount
    )
}
