package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NetworkService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val networkNodes: MutableMap<String, NetworkNode> = NetworkTopology.generateInitialNodes().associateBy { it.ip }.toMutableMap()
    private val playerDiscoveries = mutableMapOf<String, MutableSet<String>>()
    private val playerCompromises = mutableMapOf<String, MutableSet<String>>()
    private val mutex = Mutex()

    init {
        playerDiscoveries["default"] = mutableSetOf("127.0.0.1", "10.0.0.1", "10.0.0.2")
    }

    suspend fun getDiscoveredNodes(playerId: String): List<NetworkNode> = mutex.withLock {
        val discovered = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }
        networkNodes.values.filter { it.ip in discovered }
    }

    suspend fun getAllNodes(): List<NetworkNode> = mutex.withLock {
        networkNodes.values.toList()
    }

    suspend fun getNode(ip: String): NetworkNode? = mutex.withLock {
        networkNodes[ip]
    }

    suspend fun discoverNode(playerId: String, ip: String): Result<NetworkNode> = mutex.withLock {
        val node = networkNodes[ip] ?: return@withLock Result.failure(Exception("Node not found: $ip"))
        val wasNew = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }.add(ip)
        if (wasNew) playerService.discoverNode(playerId, ip)
        val result = Result.success(node)
        if (wasNew && gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.NODE_DISCOVERED, 1L))
        }
        result
    }

    suspend fun scanSubnet(playerId: String, subnet: String): List<NetworkNode> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock emptyList()
        val subnetPrefix = subnet.substringBeforeLast("/")
        val cidr = subnet.substringAfterLast("/").toIntOrNull() ?: 24

        val allowedNetworks = listOf(
            "127.0.0.0/8",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "198.51.100.0/24",
            "203.0.113.0/24"
        )

        val isAllowed = allowedNetworks.any { prefix ->
            val allowedPrefix = prefix.substringBeforeLast("/")
            subnetPrefix.startsWith(allowedPrefix.substringBeforeLast("."))
        }

        if (!isAllowed && subnetPrefix !in player.discoveredNodes) {
            return@withLock emptyList()
        }

        val discoveredBefore = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }.size

        val found = networkNodes.values.filter { node ->
            val nodePrefix = node.ip.substringBeforeLast(".")
            nodePrefix == subnetPrefix.substringBeforeLast(".") &&
            !playerDiscoveries.getOrPut(playerId) { mutableSetOf() }.contains(node.ip)
        }

        found.forEach { node ->
            playerDiscoveries.getOrPut(playerId) { mutableSetOf() }.add(node.ip)
            playerService.discoverNode(playerId, node.ip)
        }

        val newlyDiscovered = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }.size - discoveredBefore
        if (newlyDiscovered > 0 && gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.NODE_DISCOVERED, newlyDiscovered.toLong()))
        }
        found.toList()
    }

    suspend fun compromiseNode(playerId: String, ip: String, level: AccessLevel): Result<NetworkNode> = mutex.withLock {
        val node = networkNodes[ip] ?: return@withLock Result.failure(Exception("Node not found: $ip"))
        val discovered = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }
        if (ip !in discovered) {
            return@withLock Result.failure(Exception("Node $ip not discovered yet. Scan the network first."))
        }

        playerCompromises.getOrPut(playerId) { mutableSetOf() }.add(ip)
        val updatedNode = node.copy(isCompromised = true, accessLevel = level)
        networkNodes[ip] = updatedNode
        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.NETWORK_COMPROMISED, 1L))
        }
        Result.success(updatedNode)
    }

    suspend fun getAccessibleNodes(playerId: String): List<NetworkNode> = mutex.withLock {
        val compromised = playerCompromises.getOrPut(playerId) { mutableSetOf() }
        networkNodes.values.filter { it.ip in compromised }
    }

    suspend fun findPath(playerId: String, targetIp: String): List<String> = mutex.withLock {
        val discovered = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }
        if (targetIp !in discovered) return@withLock emptyList()

        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(current: String, target: String): Boolean {
            if (current == target) {
                path.add(current)
                return true
            }
            if (current in visited) return false
            visited.add(current)

            val node = networkNodes[current] ?: return false
            for (neighbor in node.connectedNodes) {
                if (neighbor in discovered && dfs(neighbor, target)) {
                    path.add(0, current)
                    return true
                }
            }
            return false
        }

        dfs("127.0.0.1", targetIp)
        path
    }

    suspend fun getNetworkStats(): NetworkStats = mutex.withLock {
        NetworkStats(
            totalNodes = networkNodes.size,
            compromisedNodes = playerCompromises.values.flatten().distinct().size,
            totalDiscoveries = playerDiscoveries.values.sumOf { it.size },
            onlinePlayers = playerService.getOnlinePlayers().size
        )
    }
}

data class NetworkStats(
    val totalNodes: Int,
    val compromisedNodes: Int,
    val totalDiscoveries: Int,
    val onlinePlayers: Int
)
