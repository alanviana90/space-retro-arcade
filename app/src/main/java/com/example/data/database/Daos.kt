package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getStats(): Flow<GameStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    suspend fun getStatsDirect(): GameStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: GameStats)
}

@Dao
interface ShipUpgradesDao {
    @Query("SELECT * FROM ship_upgrades WHERE id = 1 LIMIT 1")
    fun getUpgrades(): Flow<ShipUpgrades?>

    @Query("SELECT * FROM ship_upgrades WHERE id = 1 LIMIT 1")
    suspend fun getUpgradesDirect(): ShipUpgrades?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(upgrades: ShipUpgrades)
}
