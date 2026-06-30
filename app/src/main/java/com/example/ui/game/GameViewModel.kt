package com.example.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.GameStats
import com.example.data.database.ShipUpgrades
import com.example.data.repository.GameRepository
import com.example.ui.audio.SoundSynthesizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Menu : ScreenState()
    object Playing : ScreenState()
    object Shop : ScreenState()
    object GameOver : ScreenState()
    object Help : ScreenState()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = GameRepository(
        database.gameStatsDao(),
        database.shipUpgradesDao()
    )

    // Sound Synthesizer instance
    val soundSynthesizer = SoundSynthesizer()

    // Database persistent flows
    val statsState: StateFlow<GameStats> = repository.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameStats()
    )

    val upgradesState: StateFlow<ShipUpgrades> = repository.upgrades.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShipUpgrades()
    )

    // UI Navigation State
    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Menu)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Game engine reference
    val gameEngine = GameEngine()

    // Game loop control job
    private var gameLoopJob: Job? = null

    // Simulated Advertising States
    private val _showSimulatedBanner = MutableStateFlow(true)
    val showSimulatedBanner = _showSimulatedBanner.asStateFlow()

    private val _showSimulatedInterstitial = MutableStateFlow(false)
    val showSimulatedInterstitial = _showSimulatedInterstitial.asStateFlow()

    private val _showSimulatedRewardedAd = MutableStateFlow(false)
    val showSimulatedRewardedAd = _showSimulatedRewardedAd.asStateFlow()

    private val _interstitialCloseCallback = MutableStateFlow<(() -> Unit)?>(null)

    // To track newly beaten high score
    private val _newHighScoreAchieved = MutableStateFlow(false)
    val newHighScoreAchieved = _newHighScoreAchieved.asStateFlow()

    init {
        // Sync database configurations into Game Engine on load
        viewModelScope.launch {
            upgradesState.collect { upgrades ->
                gameEngine.fireRateUpgradeLevel = upgrades.fireRateLevel
                gameEngine.shieldUpgradeLevel = upgrades.shieldLevel
                gameEngine.speedUpgradeLevel = upgrades.speedLevel
                gameEngine.extraLifeUpgradeLevel = upgrades.extraLifeLevel
            }
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
        if (screen is ScreenState.Menu) {
            _showSimulatedBanner.value = true
        }
    }

    fun toggleMute() {
        soundSynthesizer.isMuted = !soundSynthesizer.isMuted
    }

    // --- GAME ENGINE WRAPPERS ---

    fun startGame() {
        // Increment games played
        viewModelScope.launch {
            repository.incrementGamesPlayed()
        }

        _newHighScoreAchieved.value = false
        gameEngine.resetSession()
        navigateTo(ScreenState.Playing)
        _showSimulatedBanner.value = false // hide banner for full immersion during game
        
        // Start Loop
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_currentScreen.value == ScreenState.Playing) {
                gameEngine.updateFrame { soundType ->
                    when (soundType) {
                        "laser" -> soundSynthesizer.playLaser()
                        "explosion" -> soundSynthesizer.playExplosion()
                        "powerup" -> soundSynthesizer.playPowerUp()
                        "defeat" -> soundSynthesizer.playDefeat()
                    }
                }

                // Check Game Over
                if (gameEngine.isGameOver) {
                    onGameEnded()
                    break
                }
                
                // Check Boss Victory
                if (gameEngine.isVictory) {
                    onGameEnded()
                    break
                }

                delay(16L) // ~60 FPS update
            }
        }
    }

    private fun onGameEnded() {
        gameLoopJob?.cancel()
        
        // Save score and credit updates
        viewModelScope.launch {
            _newHighScoreAchieved.value = repository.saveHighScoreIfBeaten(gameEngine.score)
            repository.addCredits(gameEngine.creditsEarned)
        }

        // Trigger Interstitial Ad after game ends! (50% chance to simulate real monetizing flow)
        if (Math.random() < 0.60) {
            showInterstitial {
                navigateTo(ScreenState.GameOver)
            }
        } else {
            navigateTo(ScreenState.GameOver)
        }
    }

    // --- STORE / UPGRADES LOGIC ---

    fun upgradeStat(statType: String) {
        val currentUpgrades = upgradesState.value
        val currentStats = statsState.value
        
        // Check upgrade costs (100 credits * level)
        val cost: Int
        var newUpgrades: ShipUpgrades? = null

        when (statType) {
            "fireRate" -> {
                if (currentUpgrades.fireRateLevel < 5) {
                    cost = currentUpgrades.fireRateLevel * 150
                    if (currentStats.totalCredits >= cost) {
                        newUpgrades = currentUpgrades.copy(fireRateLevel = currentUpgrades.fireRateLevel + 1)
                        deductCreditsAndSave(cost, newUpgrades)
                    }
                }
            }
            "shield" -> {
                if (currentUpgrades.shieldLevel < 5) {
                    cost = (currentUpgrades.shieldLevel + 1) * 120
                    if (currentStats.totalCredits >= cost) {
                        newUpgrades = currentUpgrades.copy(shieldLevel = currentUpgrades.shieldLevel + 1)
                        deductCreditsAndSave(cost, newUpgrades)
                    }
                }
            }
            "speed" -> {
                if (currentUpgrades.speedLevel < 5) {
                    cost = currentUpgrades.speedLevel * 100
                    if (currentStats.totalCredits >= cost) {
                        newUpgrades = currentUpgrades.copy(speedLevel = currentUpgrades.speedLevel + 1)
                        deductCreditsAndSave(cost, newUpgrades)
                    }
                }
            }
            "extraLife" -> {
                if (currentUpgrades.extraLifeLevel < 3) {
                    cost = (currentUpgrades.extraLifeLevel + 1) * 250
                    if (currentStats.totalCredits >= cost) {
                        newUpgrades = currentUpgrades.copy(extraLifeLevel = currentUpgrades.extraLifeLevel + 1)
                        deductCreditsAndSave(cost, newUpgrades)
                    }
                }
            }
        }
    }

    private fun deductCreditsAndSave(cost: Int, upgrades: ShipUpgrades) {
        viewModelScope.launch {
            val currentStats = statsState.value
            repository.saveStats(currentStats.copy(totalCredits = currentStats.totalCredits - cost))
            repository.saveUpgrades(upgrades)
            soundSynthesizer.playPowerUp()
        }
    }

    // --- MONETIZATION: INTERSTITIALS & REWARDED AD SIMULATIONS ---

    // Interstitial Ad Flow
    fun showInterstitial(onAdClosed: () -> Unit) {
        _interstitialCloseCallback.value = onAdClosed
        _showSimulatedInterstitial.value = true
    }

    fun closeInterstitial() {
        _showSimulatedInterstitial.value = false
        _interstitialCloseCallback.value?.invoke()
        _interstitialCloseCallback.value = null
    }

    // Rewarded Video Ad Flow: Play ad to get 1 Extra Life or bonus credits
    fun showRewardedAd() {
        _showSimulatedRewardedAd.value = true
    }

    fun completeRewardedAd() {
        _showSimulatedRewardedAd.value = false
        // Revive flow: grant extra life and restart playing loops!
        if (currentScreen.value is ScreenState.GameOver && !gameEngine.isVictory) {
            viewModelScope.launch {
                gameEngine.isGameOver = false
                gameEngine.currentLives = 1 // give one extra life
                gameEngine.playerHp = gameEngine.playerMaxHp
                gameEngine.playerShield = gameEngine.playerMaxShield
                
                navigateTo(ScreenState.Playing)
                startGameLoop()
                soundSynthesizer.playPowerUp()
            }
        } else {
            // If in shop, grant 250 free credits
            viewModelScope.launch {
                repository.addCredits(250)
                soundSynthesizer.playPowerUp()
            }
        }
    }

    fun cancelRewardedAd() {
        _showSimulatedRewardedAd.value = false
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
