package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val totalCredits: Int = 0,
    val gamesPlayed: Int = 0
)

@Entity(tableName = "ship_upgrades")
data class ShipUpgrades(
    @PrimaryKey val id: Int = 1,
    val fireRateLevel: Int = 1, // 1 to 5
    val shieldLevel: Int = 0,   // 0 to 5
    val speedLevel: Int = 1,    // 1 to 5
    val extraLifeLevel: Int = 0 // 0 to 3
)
