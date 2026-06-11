package com.zeroday.service

import com.zeroday.model.AchievementEvent
import com.zeroday.model.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CareerService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus
) {
    private val careerMutex = Mutex()

    data class CareerActionResult(
        val success: Boolean,
        val message: String,
        val heatDelta: Int = 0,
        val justiceDelta: Int = 0
    )

    suspend fun chooseCareer(playerId: String, career: String): CareerActionResult {
        return playerService.withPlayerAction(playerId) { player ->
            if (player.careerPath != "undecided")
                return@withPlayerAction CareerActionResult(false, "Career already chosen as ${player.careerPath}")

            when (career.lowercase()) {
                "whitehat" -> {
                    player.careerPath = "whitehat"
                    player.justicePoints = 10
                    gameEventBus.emit(GameEvent(playerId, AchievementEvent.CAREER_CHOSEN, 10))
                    CareerActionResult(true, "You chose the White Hat path. Justice requires vigilance.", 0, 10)
                }
                "blackhat" -> {
                    player.careerPath = "blackhat"
                    player.notoriety = 10
                    gameEventBus.emit(GameEvent(playerId, AchievementEvent.CAREER_CHOSEN, 10))
                    CareerActionResult(true, "You chose the Black Hat path. The shadows welcome you.", 0, 0)
                }
                else -> CareerActionResult(false, "Invalid career: $career. Choose 'whitehat' or 'blackhat'.")
            }
        } ?: CareerActionResult(false, "Player not found")
    }

    suspend fun addHeat(playerId: String, amount: Int, reason: String = ""): CareerActionResult {
        return playerService.withPlayerAction(playerId) { player ->
            if (player.careerPath != "blackhat")
                return@withPlayerAction CareerActionResult(false, "Only Black Hats generate heat", 0, 0)

            player.heatLevel = (player.heatLevel + amount).coerceAtMost(player.maxHeatLevel)
            player.notoriety += amount / 2

            if (player.heatLevel >= 80 && player.bountyPrice == 0L) {
                player.bountyPrice = (player.heatLevel * 50).toLong()
                gameEventBus.emit(GameEvent(playerId, AchievementEvent.BOUNTY_PLACED, player.bountyPrice))
            }

            gameEventBus.emit(GameEvent(playerId, AchievementEvent.HEAT_CHANGED, amount.toLong()))

            CareerActionResult(true, "Heat +$amount: $reason", amount, 0)
        } ?: CareerActionResult(false, "Player not found")
    }

    suspend fun addJusticePoints(playerId: String, amount: Int, reason: String = ""): CareerActionResult {
        return playerService.withPlayerAction(playerId) { player ->
            if (player.careerPath != "whitehat")
                return@withPlayerAction CareerActionResult(false, "Only White Hats earn justice points", 0, 0)

            player.justicePoints += amount
            val newRank = (player.justicePoints / 50).coerceAtMost(6)
            player.whiteHatRank = newRank

            gameEventBus.emit(GameEvent(playerId, AchievementEvent.JUSTICE_CHANGED, amount.toLong()))

            CareerActionResult(true, "Justice +$amount: $reason", 0, amount)
        } ?: CareerActionResult(false, "Player not found")
    }

    suspend fun attemptArrest( arrestingPlayerId: String, targetPlayerId: String): CareerActionResult {
        val arrester = playerService.getPlayer(arrestingPlayerId) ?: return CareerActionResult(false, "Arrester not found")
        val target = playerService.getPlayer(targetPlayerId) ?: return CareerActionResult(false, "Target not found")

        if (arrester.careerPath != "whitehat")
            return CareerActionResult(false, "Only White Hats can make arrests")

        if (target.careerPath != "blackhat")
            return CareerActionResult(false, "Target is not a Black Hat")

        val successChance = 0.3 + (arrester.justicePoints * 0.001) - (target.heatLevel * 0.005)
        val success = Math.random() < successChance.coerceIn(0.05, 0.95)

        if (success) {
            val justiceEarned = 10 + target.heatLevel / 5
            val jailHours = target.heatLevel * 0.5f

            playerService.withPlayerAction(arrestingPlayerId) { arresterP ->
                arresterP.justicePoints += justiceEarned
                arresterP.whiteHatRank = (arresterP.justicePoints / 50).coerceAtMost(6)
            }
            playerService.withPlayerAction(targetPlayerId) { targetP ->
                targetP.timesArrested++
                targetP.jailTimeRemaining = jailHours * 3600f
                targetP.isInJail = true
                targetP.heatLevel = (targetP.heatLevel / 2).coerceAtMost(targetP.maxHeatLevel)
                targetP.bountyPrice = 0L
            }

            gameEventBus.emit(GameEvent(arrestingPlayerId, AchievementEvent.ARREST_EXECUTED, justiceEarned.toLong()))

            return CareerActionResult(true, "Arrested! Justice points earned.", 0, justiceEarned)
        } else {
            return CareerActionResult(false, "Target evaded capture. Success chance was ${(successChance * 100).toInt()}%")
        }
    }

    suspend fun decayHeat(): Int {
        var totalDecayed = 0
        for (player in playerService.getAllPlayers()) {
            if (player.careerPath != "blackhat" || player.heatLevel <= 0 || player.isInJail) continue
            val decayAmount = 1.coerceAtMost(player.heatLevel)
            playerService.withPlayerAction(player.id) { p ->
                p.heatLevel = (p.heatLevel - decayAmount).coerceAtLeast(0)
            }
            totalDecayed += decayAmount
        }
        return totalDecayed
    }

    suspend fun getCareerReport(playerId: String): Map<String, Any?> {
        val player = playerService.getPlayer(playerId) ?: return emptyMap()
        return mapOf(
            "careerPath" to player.careerPath,
            "heatLevel" to player.heatLevel,
            "maxHeatLevel" to player.maxHeatLevel,
            "notoriety" to player.notoriety,
            "justicePoints" to player.justicePoints,
            "bountyPrice" to player.bountyPrice,
            "timesArrested" to player.timesArrested,
            "isInJail" to player.isInJail,
            "jailTimeRemaining" to player.jailTimeRemaining,
            "whiteHatRank" to player.whiteHatRank,
            "blackHatRank" to player.blackHatRank
        )
    }
}
