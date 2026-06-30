package com.example.data.repository

import com.example.data.database.GameStats
import com.example.data.database.GameStatsDao
import com.example.data.database.ShipUpgrades
import com.example.data.database.ShipUpgradesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(
    private val statsDao: GameStatsDao,
    private val upgradesDao: ShipUpgradesDao
) {
    val stats: Flow<GameStats> = statsDao.getStats().map { it ?: GameStats() }
    val upgrades: Flow<ShipUpgrades> = upgradesDao.getUpgrades().map { it ?: ShipUpgrades() }

    suspend fun getStatsDirect(): GameStats {
        return statsDao.getStatsDirect() ?: GameStats()
    }

    suspend fun getUpgradesDirect(): ShipUpgrades {
        return upgradesDao.getUpgradesDirect() ?: ShipUpgrades()
    }

    suspend fun saveStats(gameStats: GameStats) {
        statsDao.insertOrUpdate(gameStats)
    }

    suspend fun saveUpgrades(shipUpgrades: ShipUpgrades) {
        upgradesDao.insertOrUpdate(shipUpgrades)
    }

    suspend fun addCredits(amount: Int) {
        val current = getStatsDirect()
        saveStats(current.copy(totalCredits = current.totalCredits + amount))
    }

    suspend fun saveHighScoreIfBeaten(score: Int): Boolean {
        val current = getStatsDirect()
        if (score > current.highScore) {
            saveStats(current.copy(highScore = score))
            return true
        }
        return false
    }

    suspend fun incrementGamesPlayed() {
        val current = getStatsDirect()
        saveStats(current.copy(gamesPlayed = current.gamesPlayed + 1))
    }
}
