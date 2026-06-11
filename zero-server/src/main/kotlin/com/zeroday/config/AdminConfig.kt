package com.zeroday.config

object AdminConfig {
    val ADMIN_TOKEN: String = envStr("ZERODAY_ADMIN_TOKEN") ?: "zeroday-admin-secret-change-me"
    val ADMIN_USERNAME: String = envStr("ZERODAY_ADMIN_USERNAME") ?: "admin"
    val ADMIN_PASSWORD: String = envStr("ZERODAY_ADMIN_PASSWORD") ?: "zeroday-admin-1337"
    val ADMIN_PORT: Int = envInt("ZERODAY_ADMIN_PORT") ?: 8081
    val LOG_RETENTION_HOURS: Long = envLong("ZERODAY_LOG_RETENTION_HOURS") ?: 168L
    val RATE_LIMIT_ADMIN: Int = envInt("ZERODAY_ADMIN_RATE_LIMIT") ?: 30

    private fun envInt(key: String): Int? = System.getenv(key)?.toIntOrNull()
    private fun envLong(key: String): Long? = System.getenv(key)?.toLongOrNull()
    private fun envStr(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() }
}
