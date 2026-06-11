package com.zeroday.config

object ServerConfig {
    /** Port the HTTP server listens on. Override with ZERODAY_PORT. */
    val PORT: Int = envInt("ZERODAY_PORT") ?: 8080

    val MAX_PLAYERS = 1000
    const val BASE_EXP_MULTIPLIER = 1.0
    const val PARTY_EXP_BONUS = 1.2
    const val TASK_GEN_INTERVAL = 30_000L
    const val NETWORK_NODE_COUNT = 500
    const val STARTING_CPU = 100
    const val STARTING_RAM = 256
    const val STARTING_BANDWIDTH = 50
    const val STARTING_CREDITS = 0

    // Connection safety caps
    val MAX_CONNECTIONS: Int = envInt("ZERODAY_MAX_CONNECTIONS") ?: 2_000
    val MAX_CONNECTIONS_PER_IP: Int = envInt("ZERODAY_MAX_CONNECTIONS_PER_IP") ?: 8
    const val MAX_INBOUND_BYTES = 64 * 1024

    // Persistence
    val DATA_DIR: String = envStr("ZERODAY_DATA_DIR") ?: "data"
    val SNAPSHOT_INTERVAL_MS: Long = envLong("ZERODAY_SNAPSHOT_INTERVAL_MS") ?: 60_000L

    // Watchdog
    val IDLE_TIMEOUT_MS: Long = envLong("ZERODAY_IDLE_TIMEOUT_MS") ?: 300_000L
    val SWEEP_INTERVAL_MS: Long = envLong("ZERODAY_SWEEP_INTERVAL_MS") ?: 30_000L

    // Resource regen
    val REGEN_INTERVAL_MS: Long = envLong("ZERODAY_REGEN_INTERVAL_MS") ?: 5_000L

    // Server identity (used in MOTD / banner)
    val SERVER_NAME: String = envStr("ZERODAY_SERVER_NAME") ?: "ZeroDayMMO"

    // ---- helpers ----

    private fun envInt(key: String): Int? =
        System.getenv(key)?.toIntOrNull()

    private fun envLong(key: String): Long? =
        System.getenv(key)?.toLongOrNull()

    private fun envStr(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
}
