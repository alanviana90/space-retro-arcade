package com.example.ui.game

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class EnemyType {
    SCOUT,     // Fast, weaves left-right
    FIGHTER,   // Medium, shoots back
    METEOR,    // Slow, heavy, splits on death
    BOSS       // Large boss ship, triple shot, moving bar
}

enum class PowerUpType {
    DOUBLE_SHOT,
    SHIELD,
    SPEED_BOOST,
    REPAIR
}

data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

data class Laser(
    var x: Float,
    var y: Float,
    val isPlayer: Boolean,
    val speedX: Float,
    val speedY: Float,
    val radius: Float = 6f,
    val color: Color = Color.Cyan
)

data class Enemy(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: EnemyType,
    var hp: Int,
    val maxHp: Int,
    val points: Int,
    val width: Float,
    val height: Float,
    var speedX: Float,
    var speedY: Float,
    var shootCooldown: Int = 0,
    var weaveOffset: Float = 0f
)

data class PowerUp(
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    val width: Float = 40f,
    val height: Float = 40f,
    val speedY: Float = 4f
)

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var life: Int,
    val maxLife: Int
)

class GameEngine {
    private val random = Random(System.currentTimeMillis())
    
    // Canvas dimensions, initialized on first layout
    var width = 1080f
    var height = 1920f
    
    // Player ship state
    var playerX = 540f
    var playerY = 1600f
    val playerWidth = 110f
    val playerHeight = 110f
    
    var playerHp = 100
    var playerMaxHp = 100
    var playerShield = 0
    var playerMaxShield = 100
    
    // Upgrades modifiers loaded from Room Database
    var fireRateUpgradeLevel = 1
    var shieldUpgradeLevel = 0
    var speedUpgradeLevel = 1
    var extraLifeUpgradeLevel = 0
    
    // Session state
    var score = 0
    var creditsEarned = 0
    var isGameOver = false
    var isVictory = false
    var currentLives = 3
    
    // Entities list
    val stars = mutableListOf<Star>()
    val lasers = mutableListOf<Laser>()
    val enemies = mutableListOf<Enemy>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    
    // Boss battle tracking
    var bossSpawned = false
    var bossHp = 0
    var bossMaxHp = 1000
    
    // Gun weapon level (upgradable via in-game PowerUp)
    var laserPowerLevel = 1 // 1: Single, 2: Double, 3: Triple!
    
    // Timing counters
    private var enemySpawnTimer = 0
    private var shootTimer = 0
    var screenShakeAmount = 0f
    
    // Id generator for enemies
    private var nextEnemyId = 1L

    init {
        // Pre-populate stars for parallax background
        for (i in 0..60) {
            stars.add(
                Star(
                    x = random.nextFloat() * 1080f,
                    y = random.nextFloat() * 1920f,
                    speed = random.nextFloat() * 8f + 2f,
                    size = random.nextFloat() * 4f + 2f,
                    color = when (random.nextInt(4)) {
                        0 -> Color(0xFF64B5F6) // Neon blue
                        1 -> Color(0xFFBA68C8) // Neon purple
                        2 -> Color(0xFFFFD54F) // Neon yellow
                        else -> Color.White
                    }
                )
            )
        }
    }

    fun configureLayout(canvasWidth: Float, canvasHeight: Float) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return
        val firstTime = (width == 1080f && height == 1920f)
        width = canvasWidth
        height = canvasHeight
        if (firstTime) {
            resetSession()
        }
    }

    fun resetSession() {
        playerX = width / 2f
        playerY = height - 300f
        playerHp = playerMaxHp
        playerMaxShield = 50 + (shieldUpgradeLevel * 20)
        playerShield = playerMaxShield
        currentLives = 1 + extraLifeUpgradeLevel
        
        score = 0
        creditsEarned = 0
        isGameOver = false
        isVictory = false
        bossSpawned = false
        laserPowerLevel = 1
        
        lasers.clear()
        enemies.clear()
        powerUps.clear()
        particles.clear()
        
        enemySpawnTimer = 0
        shootTimer = 0
        screenShakeAmount = 0f
        
        // Reposition stars
        stars.forEach {
            it.x = random.nextFloat() * width
            it.y = random.nextFloat() * height
        }
    }

    // Handles finger dragging controls
    fun movePlayer(deltaX: Float, deltaY: Float) {
        if (isGameOver || isVictory) return
        val multiplier = 1f + (speedUpgradeLevel * 0.15f)
        playerX = (playerX + deltaX * multiplier).coerceIn(playerWidth / 2f, width - playerWidth / 2f)
        playerY = (playerY + deltaY * multiplier).coerceIn(height / 2f, height - 100f)
    }

    // Main physics frame update (target 60fps)
    fun updateFrame(onSoundEffect: (String) -> Unit) {
        if (isGameOver || isVictory) {
            // Stars still scroll for ambient motion
            updateStars()
            updateParticles()
            return
        }

        // 1. Screen Shake Decay
        if (screenShakeAmount > 0) {
            screenShakeAmount *= 0.9f
            if (screenShakeAmount < 0.5f) screenShakeAmount = 0f
        }

        // 2. Stars
        updateStars()

        // 3. Fire Weapons (Auto-Shoot)
        shootTimer++
        val fireCooldown = (30 - (fireRateUpgradeLevel * 3)).coerceAtLeast(10)
        if (shootTimer >= fireCooldown) {
            shootTimer = 0
            firePlayerLaser()
            onSoundEffect("laser")
        }

        // 4. Update Lasers
        updateLasers()

        // 5. Update Enemies & Spawn
        updateEnemies(onSoundEffect)

        // 6. Spawn Power-ups & Update
        updatePowerUps(onSoundEffect)

        // 7. Update Explosion Particles
        updateParticles()

        // 8. Collision Detections
        checkCollisions(onSoundEffect)
    }

    private fun updateStars() {
        stars.forEach {
            it.y += it.speed
            if (it.y > height) {
                it.y = -20f
                it.x = random.nextFloat() * width
            }
        }
    }

    private fun firePlayerLaser() {
        val speedY = -22f
        val bulletColor = Color(0xFF00E676) // Bright toxic neon green
        
        when (laserPowerLevel) {
            1 -> {
                // Single shot from center
                lasers.add(Laser(playerX, playerY - 40f, isPlayer = true, 0f, speedY, color = bulletColor))
            }
            2 -> {
                // Double shots from left & right wings
                lasers.add(Laser(playerX - 35f, playerY - 10f, isPlayer = true, 0f, speedY, color = bulletColor))
                lasers.add(Laser(playerX + 35f, playerY - 10f, isPlayer = true, 0f, speedY, color = bulletColor))
            }
            else -> {
                // Triple shots (central + diagonal angled)
                lasers.add(Laser(playerX, playerY - 40f, isPlayer = true, 0f, speedY, color = bulletColor))
                lasers.add(Laser(playerX - 35f, playerY - 10f, isPlayer = true, -4f, speedY - 1f, color = bulletColor))
                lasers.add(Laser(playerX + 35f, playerY - 10f, isPlayer = true, 4f, speedY - 1f, color = bulletColor))
            }
        }
    }

    private fun updateLasers() {
        val iterator = lasers.iterator()
        while (iterator.hasNext()) {
            val laser = iterator.next()
            laser.x += laser.speedX
            laser.y += laser.speedY
            
            // Out of bounds cleanup
            if (laser.y < -50f || laser.y > height + 50f || laser.x < -50f || laser.x > width + 50f) {
                iterator.remove()
            }
        }
    }

    private fun updateEnemies(onSoundEffect: (String) -> Unit) {
        // Spawn logic
        enemySpawnTimer++
        
        // As score goes up, spawn rate increases
        val spawnRate = (120 - (score / 350)).coerceAtLeast(45)
        
        // Spawn condition (only if Boss isn't active)
        if (!bossSpawned && enemySpawnTimer >= spawnRate) {
            enemySpawnTimer = 0
            
            // If score is over 2500 and Boss hasn't spawned yet, spawn the epic BOSS!
            if (score >= 2500 && !bossSpawned) {
                spawnBoss()
            } else {
                spawnRandomEnemy()
            }
        }

        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            
            // Handle movement paths based on type
            when (enemy.type) {
                EnemyType.SCOUT -> {
                    enemy.weaveOffset += 0.08f
                    enemy.x += sin(enemy.weaveOffset) * 6f
                    enemy.y += enemy.speedY
                }
                EnemyType.FIGHTER -> {
                    enemy.y += enemy.speedY
                    // Occasionally slides left and right
                    if (random.nextFloat() < 0.02f) {
                        enemy.speedX = (random.nextFloat() * 6f - 3f)
                    }
                    enemy.x = (enemy.x + enemy.speedX).coerceIn(enemy.width / 2f, width - enemy.width / 2f)
                    
                    // Shoots back!
                    enemy.shootCooldown--
                    if (enemy.shootCooldown <= 0) {
                        enemy.shootCooldown = random.nextInt(70, 130)
                        lasers.add(
                            Laser(
                                x = enemy.x,
                                y = enemy.y + enemy.height / 2f,
                                isPlayer = false,
                                speedX = 0f,
                                speedY = 12f,
                                color = Color(0xFFFF1744) // Toxic Hot Pink Laser
                            )
                        )
                    }
                }
                EnemyType.METEOR -> {
                    enemy.y += enemy.speedY
                    enemy.x += enemy.speedX
                    // bounce off screen edges
                    if (enemy.x < enemy.width / 2f || enemy.x > width - enemy.width / 2f) {
                        enemy.speedX *= -1
                    }
                }
                EnemyType.BOSS -> {
                    // Boss moves smoothly left/right at the top of the screen
                    enemy.x += enemy.speedX
                    if (enemy.x < 150f || enemy.x > width - 150f) {
                        enemy.speedX *= -1
                    }
                    // Moves slightly down until it reaches 350f
                    if (enemy.y < 300f) {
                        enemy.y += 2f
                    }
                    
                    // Triple Shoot
                    enemy.shootCooldown--
                    if (enemy.shootCooldown <= 0) {
                        enemy.shootCooldown = random.nextInt(40, 75)
                        onSoundEffect("laser")
                        lasers.add(Laser(enemy.x, enemy.y + 120f, isPlayer = false, 0f, 13f, color = Color.Red))
                        lasers.add(Laser(enemy.x - 60f, enemy.y + 100f, isPlayer = false, -3f, 11f, color = Color.Red))
                        lasers.add(Laser(enemy.x + 60f, enemy.y + 100f, isPlayer = false, 3f, 11f, color = Color.Red))
                    }
                }
            }

            // Clean up out of screen bounds
            if (enemy.y > height + 200f) {
                iterator.remove()
            }
        }
    }

    private fun spawnRandomEnemy() {
        val type = when (random.nextInt(100)) {
            in 0..45 -> EnemyType.SCOUT
            in 46..75 -> EnemyType.FIGHTER
            else -> EnemyType.METEOR
        }

        val eWidth: Float
        val eHeight: Float
        val hp: Int
        val speedY: Float
        val points: Int

        when (type) {
            EnemyType.SCOUT -> {
                eWidth = 80f
                eHeight = 80f
                hp = 1
                speedY = random.nextFloat() * 4f + 5f
                points = 50
            }
            EnemyType.FIGHTER -> {
                eWidth = 90f
                eHeight = 90f
                hp = 2
                speedY = random.nextFloat() * 2f + 4f
                points = 100
            }
            else -> { // METEOR
                eWidth = random.nextFloat() * 50f + 70f
                eHeight = eWidth
                hp = (eWidth / 30f).toInt().coerceAtLeast(3)
                speedY = random.nextFloat() * 1.5f + 2f
                points = 150
            }
        }

        enemies.add(
            Enemy(
                id = nextEnemyId++,
                x = random.nextFloat() * (width - 160f) + 80f,
                y = -100f,
                type = type,
                hp = hp,
                maxHp = hp,
                points = points,
                width = eWidth,
                height = eHeight,
                speedX = random.nextFloat() * 4f - 2f,
                speedY = speedY,
                shootCooldown = random.nextInt(30, 90),
                weaveOffset = random.nextFloat() * 100f
            )
        )
    }

    private fun spawnBoss() {
        bossSpawned = true
        bossMaxHp = 1000 + (score / 10) // Boss scales slightly with player expertise
        bossHp = bossMaxHp
        
        enemies.add(
            Enemy(
                id = 9999L,
                x = width / 2f,
                y = -300f,
                type = EnemyType.BOSS,
                hp = bossHp,
                maxHp = bossMaxHp,
                points = 2000,
                width = 300f,
                height = 240f,
                speedX = 4f,
                speedY = 2f,
                shootCooldown = 60
            )
        )
    }

    private fun updatePowerUps(onSoundEffect: (String) -> Unit) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val pu = iterator.next()
            pu.y += pu.speedY
            
            // Clean up off screen
            if (pu.y > height + 50f) {
                iterator.remove()
            }
        }
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life--
            if (p.life <= 0) {
                iterator.remove()
            }
        }
    }

    private fun createExplosion(x: Float, y: Float, color: Color, numParticles: Int = 12) {
        for (i in 0 until numParticles) {
            val angle = random.nextDouble() * 2.0 * Math.PI
            val speed = random.nextFloat() * 8f + 2f
            val maxLife = random.nextInt(15, 30)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    color = color,
                    life = maxLife,
                    maxLife = maxLife
                )
            )
        }
    }

    private fun checkCollisions(onSoundEffect: (String) -> Unit) {
        // A. PLAYER LASERS vs ENEMIES
        val laserIterator = lasers.iterator()
        while (laserIterator.hasNext()) {
            val laser = laserIterator.next()
            if (!laser.isPlayer) continue // Skip enemy lasers
            
            val enemyIterator = enemies.iterator()
            var laserHit = false
            while (enemyIterator.hasNext()) {
                val enemy = enemyIterator.next()
                
                // Simple box collision
                if (laser.x >= enemy.x - enemy.width / 2f &&
                    laser.x <= enemy.x + enemy.width / 2f &&
                    laser.y >= enemy.y - enemy.height / 2f &&
                    laser.y <= enemy.y + enemy.height / 2f
                ) {
                    laserHit = true
                    enemy.hp -= 1
                    
                    // Create tiny splash sparks
                    createExplosion(laser.x, laser.y, Color.Yellow, 3)
                    
                    if (enemy.hp <= 0) {
                        // Enemy Destroyed!
                        createExplosion(enemy.x, enemy.y, getEnemyColor(enemy.type), 18)
                        onSoundEffect("explosion")
                        
                        score += enemy.points
                        
                        // Every point adds to upgrade credits
                        val creditsEarnedThisShip = (enemy.points / 10).coerceAtLeast(1)
                        creditsEarned += creditsEarnedThisShip
                        
                        // Handle Meteor Split feature!
                        if (enemy.type == EnemyType.METEOR && enemy.width > 85f) {
                            splitMeteor(enemy)
                        }
                        
                        // Handle Boss Victory
                        if (enemy.type == EnemyType.BOSS) {
                            isVictory = true
                            createExplosion(enemy.x, enemy.y, Color.Red, 50)
                            createExplosion(enemy.x - 50f, enemy.y - 40f, Color.Yellow, 25)
                            createExplosion(enemy.x + 50f, enemy.y + 40f, Color.Cyan, 25)
                        }
                        
                        // Roll for Power-up drops
                        if (random.nextFloat() < 0.15f) {
                            spawnPowerUp(enemy.x, enemy.y)
                        }
                        
                        enemyIterator.remove()
                    } else if (enemy.type == EnemyType.BOSS) {
                        // Keep Boss HP synchronized
                        bossHp = enemy.hp
                    }
                    break // Exit inner loop, laser can hit only one enemy
                }
            }
            if (laserHit) {
                laserIterator.remove()
            }
        }

        // B. ENEMY LASERS vs PLAYER
        val laserIter = lasers.iterator()
        while (laserIter.hasNext()) {
            val laser = laserIter.next()
            if (laser.isPlayer) continue
            
            // Check player box collision
            if (laser.x >= playerX - playerWidth / 2f &&
                laser.x <= playerX + playerWidth / 2f &&
                laser.y >= playerY - playerHeight / 2f &&
                laser.y <= playerY + playerHeight / 2f
            ) {
                damagePlayer(15, onSoundEffect)
                createExplosion(laser.x, laser.y, Color.Red, 6)
                laserIter.remove()
            }
        }

        // C. ENEMIES vs PLAYER (Direct crashes)
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            
            val distanceX = Math.abs(enemy.x - playerX)
            val distanceY = Math.abs(enemy.y - playerY)
            val combinedHalfWidth = (enemy.width + playerWidth) / 2.2f
            val combinedHalfHeight = (enemy.height + playerHeight) / 2.2f
            
            if (distanceX < combinedHalfWidth && distanceY < combinedHalfHeight) {
                // Crash!
                val damage = if (enemy.type == EnemyType.BOSS) 100 else 30
                damagePlayer(damage, onSoundEffect)
                
                if (enemy.type != EnemyType.BOSS) {
                    createExplosion(enemy.x, enemy.y, getEnemyColor(enemy.type), 15)
                    enemyIter.remove()
                } else {
                    createExplosion(playerX, playerY, Color.Yellow, 10)
                }
            }
        }

        // D. POWERUPS vs PLAYER
        val puIter = powerUps.iterator()
        while (puIter.hasNext()) {
            val pu = puIter.next()
            
            val distanceX = Math.abs(pu.x - playerX)
            val distanceY = Math.abs(pu.y - playerY)
            val combinedHalfWidth = (pu.width + playerWidth) / 2f
            val combinedHalfHeight = (pu.height + playerHeight) / 2f
            
            if (distanceX < combinedHalfWidth && distanceY < combinedHalfHeight) {
                // Collect power up
                onSoundEffect("powerup")
                when (pu.type) {
                    PowerUpType.DOUBLE_SHOT -> {
                        laserPowerLevel = (laserPowerLevel + 1).coerceAtMost(3)
                        createExplosion(playerX, playerY, Color(0xFF00E676), 15)
                    }
                    PowerUpType.SHIELD -> {
                        playerShield = playerMaxShield
                        createExplosion(playerX, playerY, Color(0xFF29B6F6), 15)
                    }
                    PowerUpType.SPEED_BOOST -> {
                        // Grant active speed boost & credits bonus
                        creditsEarned += 50
                        createExplosion(playerX, playerY, Color(0xFFFFCA28), 15)
                    }
                    PowerUpType.REPAIR -> {
                        playerHp = (playerHp + 40).coerceAtMost(playerMaxHp)
                        createExplosion(playerX, playerY, Color(0xFF66BB6A), 15)
                    }
                }
                puIter.remove()
            }
        }
    }

    private fun splitMeteor(parent: Enemy) {
        val debrisCount = 2
        for (i in 0 until debrisCount) {
            val scale = parent.width * 0.5f
            enemies.add(
                Enemy(
                    id = nextEnemyId++,
                    x = parent.x + random.nextFloat() * 40f - 20f,
                    y = parent.y + random.nextFloat() * 40f - 20f,
                    type = EnemyType.METEOR,
                    hp = 1,
                    maxHp = 1,
                    points = 50,
                    width = scale,
                    height = scale,
                    speedX = parent.speedX + (if (i == 0) -2.5f else 2.5f),
                    speedY = parent.speedY * 1.3f,
                    shootCooldown = 999
                )
            )
        }
    }

    private fun spawnPowerUp(x: Float, y: Float) {
        val type = when (random.nextInt(4)) {
            0 -> PowerUpType.DOUBLE_SHOT
            1 -> PowerUpType.SHIELD
            2 -> PowerUpType.SPEED_BOOST
            else -> PowerUpType.REPAIR
        }
        powerUps.add(PowerUp(x, y, type))
    }

    private fun damagePlayer(amount: Int, onSoundEffect: (String) -> Unit) {
        screenShakeAmount = 25f
        
        if (playerShield > 0) {
            playerShield -= amount
            if (playerShield < 0) {
                playerHp += playerShield // subtract excess damage from hp
                playerShield = 0
            }
        } else {
            playerHp -= amount
        }

        onSoundEffect("explosion")

        if (playerHp <= 0) {
            // Lose a life!
            currentLives--
            if (currentLives > 0) {
                // Respawn with brief shield
                playerHp = playerMaxHp
                playerShield = playerMaxShield
                playerX = width / 2f
                playerY = height - 300f
                laserPowerLevel = 1 // reset guns
                createExplosion(playerX, playerY, Color.White, 25)
            } else {
                // Dead
                isGameOver = true
                onSoundEffect("defeat")
                createExplosion(playerX, playerY, Color(0xFFFF1744), 35)
            }
        }
    }

    private fun getEnemyColor(type: EnemyType): Color {
        return when (type) {
            EnemyType.SCOUT -> Color(0xFFE57373)   // Light red
            EnemyType.FIGHTER -> Color(0xFFF48FB1) // Pink purple
            EnemyType.METEOR -> Color(0xFFB0BEC5)  // Slate gray
            EnemyType.BOSS -> Color(0xFFD32F2F)    // Crimson red
        }
    }
}
