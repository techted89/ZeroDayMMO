package com.zeroday.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class ZoneFaction(val displayName: String) {
    Neutral("Neutral"),
    Syndicate("Syndicate"),
    CorpNet("CorpNet"),
    GhostNet("GhostNet"),
    FreeWorld("FreeWorld"),
    ZeroDay("ZeroDay"),
    LawEnforcement("Law Enforcement")
}

enum class ZoneState { Safe, Contested, Controlled, Warzone, Locked }

@Serializable
data class Zone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    var controllingFaction: ZoneFaction = ZoneFaction.Neutral,
    var state: ZoneState = ZoneState.Safe,
    var controlLevel: Int = 50,
    val maxControlLevel: Int = 100,
    var securityLevel: Int = 1,
    val requiredLevel: Int = 1,
    val connectedZoneIds: List<String> = emptyList(),
    var hasBoss: Boolean = false,
    var bossName: String = "",
    var bossLevel: Int = 1,
    var threatLevel: Int = 0
)

@Serializable
data class ZoneSnapshot(
    val id: String,
    val name: String,
    val description: String,
    val controllingFaction: String,
    val state: String,
    val controlLevel: Int,
    val maxControlLevel: Int,
    val securityLevel: Int,
    val requiredLevel: Int,
    val connectedZoneIds: List<String>,
    val hasBoss: Boolean,
    val bossName: String,
    val bossLevel: Int,
    val threatLevel: Int
) {
    companion object {
        fun from(zone: Zone): ZoneSnapshot = ZoneSnapshot(
            id = zone.id,
            name = zone.name,
            description = zone.description,
            controllingFaction = zone.controllingFaction.name,
            state = zone.state.name,
            controlLevel = zone.controlLevel,
            maxControlLevel = zone.maxControlLevel,
            securityLevel = zone.securityLevel,
            requiredLevel = zone.requiredLevel,
            connectedZoneIds = zone.connectedZoneIds,
            hasBoss = zone.hasBoss,
            bossName = zone.bossName,
            bossLevel = zone.bossLevel,
            threatLevel = zone.threatLevel
        )
    }
}
