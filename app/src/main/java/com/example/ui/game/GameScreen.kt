package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.GameStats
import com.example.data.database.ShipUpgrades
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val upgrades by viewModel.upgradesState.collectAsStateWithLifecycle()
    
    val showBanner by viewModel.showSimulatedBanner.collectAsStateWithLifecycle()
    val showInterstitial by viewModel.showSimulatedInterstitial.collectAsStateWithLifecycle()
    val showRewarded by viewModel.showSimulatedRewardedAd.collectAsStateWithLifecycle()
    
    val newHighScore by viewModel.newHighScoreAchieved.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07050E)) // deep space background
    ) {
        // --- PRIMARY CONTENT ROUTER ---
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(400),
            label = "screen_navigation"
        ) { screen ->
            when (screen) {
                is ScreenState.Menu -> MenuScreen(
                    stats = stats,
                    onPlayClick = { viewModel.startGame() },
                    onShopClick = { viewModel.navigateTo(ScreenState.Shop) },
                    onHelpClick = { viewModel.navigateTo(ScreenState.Help) },
                    soundMuted = viewModel.soundSynthesizer.isMuted,
                    onToggleSound = { viewModel.toggleMute() }
                )
                is ScreenState.Playing -> PlayingScreen(
                    viewModel = viewModel
                )
                is ScreenState.Shop -> ShopScreen(
                    stats = stats,
                    upgrades = upgrades,
                    onUpgradeClick = { viewModel.upgradeStat(it) },
                    onBackClick = { viewModel.navigateTo(ScreenState.Menu) }
                )
                is ScreenState.GameOver -> GameOverScreen(
                    gameEngine = viewModel.gameEngine,
                    newHighScore = newHighScore,
                    onRestartClick = { viewModel.startGame() },
                    onMenuClick = { viewModel.navigateTo(ScreenState.Menu) },
                    onWatchAdClick = { viewModel.showRewardedAd() }
                )
                is ScreenState.Help -> HelpScreen(
                    onBackClick = { viewModel.navigateTo(ScreenState.Menu) }
                )
            }
        }

        // --- SIMULATED ADVERTISING LAYERS (EDUCATIONAL & MONETIZATION DEMO) ---

        // A. Bottom Banner Ad (Shown in Menu, Upgrades, Help)
        if (showBanner && currentScreen !is ScreenState.Playing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                SimulatedBannerAd()
            }
        }

        // B. Interstitial Ad (Triggered between levels/game over)
        if (showInterstitial) {
            SimulatedInterstitialAd(
                onClose = { viewModel.closeInterstitial() }
            )
        }

        // C. Rewarded Video Ad (Watches ad to revive or earn currency)
        if (showRewarded) {
            SimulatedRewardedAd(
                onAdCompleted = { viewModel.completeRewardedAd() },
                onAdDismissed = { viewModel.cancelRewardedAd() }
            )
        }
    }
}

// ==========================================
// 1. MENU SCREEN
// ==========================================
@Composable
fun MenuScreen(
    stats: GameStats,
    onPlayClick: () -> Unit,
    onShopClick: () -> Unit,
    onHelpClick: () -> Unit,
    soundMuted: Boolean,
    onToggleSound: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
    val titlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Hero Pixel Art Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.menu_bg),
                contentDescription = "Cosmic Space",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Tint Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC07050E)),
                            startY = 100f
                        )
                    )
            )
            
            // Neon badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xFF00E676), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "RETRO 8-BIT",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Title text
        Text(
            text = "COSMIC ARCADE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF00E676),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(titlePulse)
                .drawBehind {
                    // Title neon bloom effect
                    drawCircle(
                        color = Color(0x3300E676),
                        radius = size.width * 0.45f,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                }
        )
        Text(
            text = "S P A C E   S H O O T E R",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF29B6F6),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // High Score / Credits Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
            border = borderStroke(1.dp, Color(0xFF312C57)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECORDE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = "Rec",
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.highScore}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFF312C57))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CRÉDITOS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.totalCredits}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // BUTTONS BLOCK
        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("play_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "INICIAR BATALHA",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onShopClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("upgrades_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1933)),
            shape = RoundedCornerShape(12.dp),
            border = borderStroke(1.2.dp, Color(0xFF29B6F6))
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color(0xFF29B6F6))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LOJA DE UPGRADES",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sound button
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .background(Color(0xFF1B1933), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF312C57), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = if (soundMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute/Unmute",
                    tint = if (soundMuted) Color.Gray else Color(0xFF00E676)
                )
            }

            // Monetization info button
            Button(
                onClick = onHelpClick,
                modifier = Modifier
                    .weight(3.5f)
                    .padding(start = 6.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1933)),
                shape = RoundedCornerShape(10.dp),
                border = borderStroke(1.dp, Color(0xFFFFCA28))
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFFCA28), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MONETIZAÇÃO PLAY STORE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// Helper to make clean border strokes
@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

// ==========================================
// 2. PLAYING SCREEN (CANVAS RUNTIME)
// ==========================================
@Composable
fun PlayingScreen(
    viewModel: GameViewModel
) {
    val gameEngine = viewModel.gameEngine
    val state = remember { mutableStateOf(0) } // trigger recompositions manually
    
    // Setup Tick Update
    LaunchedEffect(key1 = true) {
        while (viewModel.currentScreen.value is ScreenState.Playing) {
            state.value++
            delay(16L) // repaint rate matches viewmodel updates
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    gameEngine.movePlayer(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        gameEngine.configureLayout(w, h)

        // Draw Canvas Game Screen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shakeX = if (gameEngine.screenShakeAmount > 0) {
                (Math.random() * gameEngine.screenShakeAmount * 2 - gameEngine.screenShakeAmount).toFloat()
            } else 0f
            val shakeY = if (gameEngine.screenShakeAmount > 0) {
                (Math.random() * gameEngine.screenShakeAmount * 2 - gameEngine.screenShakeAmount).toFloat()
            } else 0f

            // Offset the drawing matrix according to Screen Shake
            drawContext.canvas.save()
            drawContext.canvas.translate(shakeX, shakeY)

            // A. Draw Stars Background
            gameEngine.stars.forEach { star ->
                drawCircle(
                    color = star.color,
                    radius = star.size,
                    center = Offset(star.x, star.y)
                )
            }

            // B. Draw Power Ups
            gameEngine.powerUps.forEach { pu ->
                val glowColor = when (pu.type) {
                    PowerUpType.DOUBLE_SHOT -> Color(0xFF00E676)
                    PowerUpType.SHIELD -> Color(0xFF29B6F6)
                    PowerUpType.SPEED_BOOST -> Color(0xFFFFCA28)
                    PowerUpType.REPAIR -> Color(0xFFE57373)
                }
                
                // Outer Glow ring
                drawCircle(
                    color = glowColor.copy(alpha = 0.35f),
                    radius = pu.width * 0.9f,
                    center = Offset(pu.x, pu.y)
                )
                // Solid Inner Square
                drawRoundRect(
                    color = glowColor,
                    topLeft = Offset(pu.x - pu.width / 2f, pu.y - pu.height / 2f),
                    size = Size(pu.width, pu.height),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Symbol Indicator (like 8-bit text)
                val indicator = when (pu.type) {
                    PowerUpType.DOUBLE_SHOT -> "W"
                    PowerUpType.SHIELD -> "S"
                    PowerUpType.SPEED_BOOST -> "V"
                    PowerUpType.REPAIR -> "H"
                }
                // Draw small symbol placeholder inside
                drawRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(pu.x - pu.width * 0.25f, pu.y - pu.height * 0.25f),
                    size = Size(pu.width * 0.5f, pu.height * 0.5f)
                )
            }

            // C. Draw Particles (Explosions)
            gameEngine.particles.forEach { p ->
                val alpha = p.life.toFloat() / p.maxLife
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = 3f + (5f * (1f - alpha)), // particles expand slightly
                    center = Offset(p.x, p.y)
                )
            }

            // D. Draw Lasers
            gameEngine.lasers.forEach { laser ->
                // Outer glow
                drawLine(
                    color = laser.color.copy(alpha = 0.4f),
                    start = Offset(laser.x, laser.y),
                    end = Offset(laser.x - laser.speedX * 1.5f, laser.y - laser.speedY * 1.5f),
                    strokeWidth = laser.radius * 2.8f,
                    cap = StrokeCap.Round
                )
                // Core bright line
                drawLine(
                    color = Color.White,
                    start = Offset(laser.x, laser.y),
                    end = Offset(laser.x - laser.speedX * 1.1f, laser.y - laser.speedY * 1.1f),
                    strokeWidth = laser.radius,
                    cap = StrokeCap.Round
                )
            }

            // E. Draw Enemies
            gameEngine.enemies.forEach { enemy ->
                when (enemy.type) {
                    EnemyType.SCOUT -> {
                        // Fast scout: Red delta wing pointing down
                        val path = Path().apply {
                            moveTo(enemy.x, enemy.y + enemy.height / 2f)
                            lineTo(enemy.x - enemy.width / 2f, enemy.y - enemy.height / 2f)
                            lineTo(enemy.x, enemy.y - enemy.height * 0.1f)
                            lineTo(enemy.x + enemy.width / 2f, enemy.y - enemy.height / 2f)
                            close()
                        }
                        drawPath(path = path, color = Color(0xFFFF5252))
                        // Wing highlights
                        drawCircle(Color.White, 6f, Offset(enemy.x, enemy.y - enemy.height * 0.1f))
                    }
                    EnemyType.FIGHTER -> {
                        // fighter: Sleek pink space disk
                        drawRoundRect(
                            color = Color(0xFFF48FB1),
                            topLeft = Offset(enemy.x - enemy.width / 2f, enemy.y - enemy.height * 0.25f),
                            size = Size(enemy.width, enemy.height * 0.5f),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        // Turret dome
                        drawCircle(
                            color = Color(0xFFFF1744),
                            radius = enemy.width * 0.25f,
                            center = Offset(enemy.x, enemy.y)
                        )
                    }
                    EnemyType.METEOR -> {
                        // Meteor: Jagged circle slate gray
                        drawCircle(
                            color = Color(0xFF78909C),
                            radius = enemy.width / 2f,
                            center = Offset(enemy.x, enemy.y)
                        )
                        // Dark crater dots
                        drawCircle(Color(0xFF455A64), enemy.width * 0.15f, Offset(enemy.x - enemy.width * 0.18f, enemy.y - enemy.height * 0.18f))
                        drawCircle(Color(0xFF455A64), enemy.width * 0.10f, Offset(enemy.x + enemy.width * 0.22f, enemy.y + enemy.height * 0.10f))
                    }
                    EnemyType.BOSS -> {
                        // BOSS: Massive heavy battleship fortress
                        // Draw outer wings
                        drawRoundRect(
                            color = Color(0xFF880E4F),
                            topLeft = Offset(enemy.x - enemy.width / 2f, enemy.y - enemy.height * 0.3f),
                            size = Size(enemy.width, enemy.height * 0.5f),
                            cornerRadius = CornerRadius(20f, 20f)
                        )
                        // Center hull fortress
                        drawRect(
                            color = Color(0xFFD50000),
                            topLeft = Offset(enemy.x - enemy.width * 0.25f, enemy.y - enemy.height * 0.45f),
                            size = Size(enemy.width * 0.5f, enemy.height * 0.9f)
                        )
                        // Glowing cores
                        drawCircle(Color.Cyan, 32f, Offset(enemy.x, enemy.y))
                        drawCircle(Color.White, 12f, Offset(enemy.x, enemy.y))
                    }
                }
            }

            // F. Draw Player Ship
            // Draw thruster exhaust flame with animated flickering height
            val exhaustPulse = 20f + (Math.random() * 25f).toFloat()
            val exhaustPath = Path().apply {
                moveTo(gameEngine.playerX - 20f, gameEngine.playerY + gameEngine.playerHeight / 2.5f)
                lineTo(gameEngine.playerX, gameEngine.playerY + gameEngine.playerHeight / 2.5f + exhaustPulse)
                lineTo(gameEngine.playerX + 20f, gameEngine.playerY + gameEngine.playerHeight / 2.5f)
                close()
            }
            drawPath(path = exhaustPath, color = Color(0xFFFF9100)) // Inner fire

            // Main spaceship polygon pointing upwards
            val playerPath = Path().apply {
                moveTo(gameEngine.playerX, gameEngine.playerY - gameEngine.playerHeight / 2f) // Nose cone
                lineTo(gameEngine.playerX - gameEngine.playerWidth / 2f, gameEngine.playerY + gameEngine.playerHeight / 2.5f) // Left wing
                lineTo(gameEngine.playerX - gameEngine.playerWidth * 0.15f, gameEngine.playerY + gameEngine.playerHeight * 0.15f) // inner left
                lineTo(gameEngine.playerX + gameEngine.playerWidth * 0.15f, gameEngine.playerY + gameEngine.playerHeight * 0.15f) // inner right
                lineTo(gameEngine.playerX + gameEngine.playerWidth / 2f, gameEngine.playerY + gameEngine.playerHeight / 2.5f) // Right wing
                close()
            }
            drawPath(path = playerPath, color = Color(0xFF00E5FF)) // Vibrant cyan hull

            // Cockpit dome (glowing neon blue)
            drawCircle(
                color = Color.White,
                radius = gameEngine.playerWidth * 0.13f,
                center = Offset(gameEngine.playerX, gameEngine.playerY - gameEngine.playerHeight * 0.05f)
            )

            // Draw Energy Shield if active
            if (gameEngine.playerShield > 0) {
                val shieldAlpha = (0.2f + (gameEngine.playerShield.toFloat() / gameEngine.playerMaxShield) * 0.4f).coerceIn(0.1f, 0.6f)
                drawCircle(
                    color = Color(0xFF29B6F6).copy(alpha = shieldAlpha),
                    radius = gameEngine.playerWidth * 0.85f,
                    center = Offset(gameEngine.playerX, gameEngine.playerY),
                    style = Stroke(width = 8f)
                )
            }

            drawContext.canvas.restore()
        }

        // --- HUD OVERLAYS (PLAYING OVERLAY) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // First row: Lives & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score text
                Column {
                    Text(
                        text = "SCORE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gameEngine.score}",
                        color = Color(0xFF00E676),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Lives indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Vidas",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "x ${gameEngine.currentLives}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HP Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(0.55f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HP ",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2C2538))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(gameEngine.playerHp.toFloat() / gameEngine.playerMaxHp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF1744), Color(0xFF00E676))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Shield Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(0.55f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHD",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2C2538))
                ) {
                    if (gameEngine.playerMaxShield > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(gameEngine.playerShield.toFloat() / gameEngine.playerMaxShield)
                                .background(Color(0xFF29B6F6))
                        )
                    }
                }
            }

            // Boss HP HUD
            if (gameEngine.bossSpawned && gameEngine.bossHp > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BOSS: NAVALHA CÓSMICA",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF330000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(gameEngine.bossHp.toFloat() / gameEngine.bossMaxHp)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. SHOP / UPGRADES SCREEN
// ==========================================
@Composable
fun ShopScreen(
    stats: GameStats,
    upgrades: ShipUpgrades,
    onUpgradeClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color(0xFF1B1933), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "OFICINA DE UPGRADES",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // Available Credits Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1933)),
            border = borderStroke(1.2.dp, Color(0xFF29B6F6))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SEUS CRÉDITOS:",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${stats.totalCredits}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Upgrades List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Weapon speed upgrade
            item {
                UpgradeItem(
                    title = "Cadência de Tiro",
                    description = "Garante canhão laser de carregamento super veloz.",
                    level = upgrades.fireRateLevel,
                    maxLevel = 5,
                    cost = upgrades.fireRateLevel * 150,
                    availableCredits = stats.totalCredits,
                    onBuy = { onUpgradeClick("fireRate") },
                    icon = Icons.Filled.FlashOn,
                    iconColor = Color(0xFFFFCA28),
                    tag = "buy_upgrade_fire_rate"
                )
            }

            // Shield Capacity upgrade
            item {
                UpgradeItem(
                    title = "Escudo de Força",
                    description = "Amplia e fortalece a barreira de escudo de plasma azul.",
                    level = upgrades.shieldLevel,
                    maxLevel = 5,
                    cost = (upgrades.shieldLevel + 1) * 120,
                    availableCredits = stats.totalCredits,
                    onBuy = { onUpgradeClick("shield") },
                    icon = Icons.Filled.Security,
                    iconColor = Color(0xFF29B6F6),
                    tag = "buy_upgrade_shield"
                )
            }

            // Thrusters Speed upgrade
            item {
                UpgradeItem(
                    title = "Motores Traseiros",
                    description = "Aumenta consideravelmente a mobilidade e velocidade da nave.",
                    level = upgrades.speedLevel,
                    maxLevel = 5,
                    cost = upgrades.speedLevel * 100,
                    availableCredits = stats.totalCredits,
                    onBuy = { onUpgradeClick("speed") },
                    icon = Icons.Filled.Speed,
                    iconColor = Color(0xFF00E676),
                    tag = "buy_upgrade_speed"
                )
            }

            // Extra Life upgrade
            item {
                UpgradeItem(
                    title = "Módulo Extra de Sobrevida",
                    description = "Inicie todas as partidas com vidas adicionais garantidas.",
                    level = upgrades.extraLifeLevel,
                    maxLevel = 3,
                    cost = (upgrades.extraLifeLevel + 1) * 250,
                    availableCredits = stats.totalCredits,
                    onBuy = { onUpgradeClick("extraLife") },
                    icon = Icons.Filled.Favorite,
                    iconColor = Color(0xFFFF1744),
                    tag = "buy_upgrade_extra_life"
                )
            }
        }
    }
}

@Composable
fun UpgradeItem(
    title: String,
    description: String,
    level: Int,
    maxLevel: Int,
    cost: Int,
    availableCredits: Int,
    onBuy: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    tag: String
) {
    val isMax = level >= maxLevel
    val canAfford = availableCredits >= cost && !isMax

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
        border = borderStroke(1.dp, if (canAfford) Color(0x6600E676) else Color(0xFF252147)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = description, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                
                // Indicators dots
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    for (i in 1..maxLevel) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i <= level) iconColor else Color(0xFF2C274E)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMax) "MAX" else "NÍVEL $level/$maxLevel",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Buy Button
            Button(
                onClick = onBuy,
                enabled = canAfford,
                modifier = Modifier
                    .width(90.dp)
                    .height(42.dp)
                    .testTag(tag),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    disabledContainerColor = Color(0xFF2C274E)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isMax) {
                    Text("FULL", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = if (canAfford) Color.Black else Color.Gray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$cost",
                            color = if (canAfford) Color.Black else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. GAME OVER SCREEN
// ==========================================
@Composable
fun GameOverScreen(
    gameEngine: GameEngine,
    newHighScore: Boolean,
    onRestartClick: () -> Unit,
    onMenuClick: () -> Unit,
    onWatchAdClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Defeat Title
        Text(
            text = if (gameEngine.isVictory) "VITÓRIA EXCEPCIONAL!" else "FIM DE JOGO",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (gameEngine.isVictory) Color(0xFF00E676) else Color(0xFFFF1744),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (gameEngine.isVictory) "VOCÊ SALVOU A GALÁXIA DO IMPÉRIO!" else "SUA NAVE FOI DESTRUÍDA NAS ESTRELAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // High Score Beacon Badge
        if (newHighScore) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFCA28), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "NOVO RECORDE GLOBAL!",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
            border = borderStroke(1.2.dp, Color(0xFF252147)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PONTUAÇÃO OBTIDA", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(
                    text = "${gameEngine.score}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = Color(0xFF252147))

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRÉDITOS RESGATADOS", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+${gameEngine.creditsEarned}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Monetization hook: Revive by watching an Ad! (Only if not Victory)
        if (!gameEngine.isVictory) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1933)),
                border = borderStroke(1.2.dp, Color(0xFFFFCA28)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUER CONTINUAR O JOGO?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Assista a um vídeo rápido e ganhe +1 Vida extra!",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = onWatchAdClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("watch_ad_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCA28)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ASSISTIR VÍDEO (REVIVER)",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Action Buttons
        Button(
            onClick = onRestartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("JOGAR NOVAMENTE", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onMenuClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("back_to_menu_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1933)),
            shape = RoundedCornerShape(10.dp),
            border = borderStroke(1.dp, Color(0xFF312C57))
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("VOLTAR AO MENU", color = Color.White, fontSize = 14.sp)
        }
    }
}

// ==========================================
// 5. EDUCATIONAL / HELP SCREEN
// ==========================================
@Composable
fun HelpScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color(0xFF1B1933), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "GUIA DE MONETIZAÇÃO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
                    border = borderStroke(1.dp, Color(0xFFFFCA28))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFFFCA28))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ganhando Dinheiro na Play Store", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "Este jogo foi inteiramente desenvolvido com alta performance em Kotlin nativo para maximizar taxas de aprovação rápida do algoritmo do Google Play Services. Ele possui uma simulação interativa completa do ciclo de AdMob (Banners, Intermitentes e Vídeos Recompensados) para fins educativos e para você ver como as mecânicas funcionam antes de subir a versão real.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            item {
                Text("Passo a Passo para Publicar com AdMob", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
            }

            item {
                StepItem(
                    stepNumber = "1",
                    title = "Crie sua Conta de Desenvolvedor Google e AdMob",
                    description = "Acesse o site do Google Play Console e do Google AdMob. Crie suas contas correspondentes. No painel do AdMob, cadastre este app Android e gere três Ad Unit IDs (um para Banner, um para Interstitial e um para Rewarded Video)."
                )
            }

            item {
                StepItem(
                    stepNumber = "2",
                    title = "Importe a SDK de Anúncios Google (Mobile Ads SDK)",
                    description = "No arquivo build.gradle.kts (módulo :app), adicione a biblioteca oficial do Google Ads:\n`implementation(\"com.google.android.gms:play-services-ads:23.x.x\")`"
                )
            }

            item {
                StepItem(
                    stepNumber = "3",
                    title = "Inicialize e Vincule no ViewModel",
                    description = "No seu MainActivity.kt, inicialize a Mobile Ads SDK:\n`MobileAds.initialize(this) {}`.\nDepois, nos locais marcados no GameViewModel onde as simulações rodam, chame os carregadores reais da SDK AdMob como `AdRequest` e as funções `InterstitialAd.load` ou `RewardedAd.load` passando seus Ad IDs reais obtidos no passo 1."
                )
            }

            item {
                StepItem(
                    stepNumber = "4",
                    title = "Suba na Play Store para Testar",
                    description = "Crie o pacote de liberação unificado (.AAB) diretamente em AI Studio nas configurações e suba no painel do Console para trilhas de testes internos. Quando aprovado, lance em produção de forma orgânica e comece a faturar com cliques!"
                )
            }
        }
    }
}

@Composable
fun StepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
        border = borderStroke(1.dp, Color(0xFF252147))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF29B6F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNumber, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Justify)
            }
        }
    }
}

// ==========================================
// 6. SIMULATED ADS RENDERING MODULE
// ==========================================

@Composable
fun SimulatedBannerAd() {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(50.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212B)),
        border = borderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF81C784))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Anúncio AdMob", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Cresça sua Nave Espacial no jogo!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Banner Simulado 320x50. Clique para ver.", fontSize = 8.sp, color = Color.Gray)
            }
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.padding(end = 8.dp).size(14.dp)
            )
        }
    }
}

@Composable
fun SimulatedInterstitialAd(
    onClose: () -> Unit
) {
    var countdown by remember { mutableStateOf(4) }
    
    LaunchedEffect(key1 = true) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {}, // eat clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(300.dp)
                .background(Color(0xFF131024), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF1744))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("INTERSTITIAL", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                // Countdown/Close button
                if (countdown > 0) {
                    Text("Fechar em ${countdown}s", color = Color.Gray, fontSize = 11.sp)
                } else {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cool Fake Game ad cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF211D3C)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = Color(0xFFFF1744), modifier = Modifier.size(42.dp))
                    Text("SPACE TYCOON II", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Construa sua própria base lunar grátis!", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Simulated download click */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("INSTALAR AGORA", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimulatedRewardedAd(
    onAdCompleted: () -> Unit,
    onAdDismissed: () -> Unit
) {
    var countdown by remember { mutableStateOf(5) }
    var showRewardClaim by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        showRewardClaim = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f))
            .clickable(enabled = false) {}, // block taps
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(320.dp)
                .background(Color(0xFF131024), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFFFCA28), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFCA28))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("VÍDEO RECOMPENSADO", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // Dismiss button before completion? Let's show a standard Ad style Close option
                IconButton(
                    onClick = onAdDismissed,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Skip", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large TV Screen Simulated Ad Trailer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F1B3A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFCA28), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("CRÉDITOS PREMIUM ADMOB", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                    Text("Assista o final para dobrar suas moedas!", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!showRewardClaim) {
                // Loader count
                CircularProgressIndicator(
                    progress = { (5f - countdown.toFloat()) / 5f },
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFFFFCA28),
                    strokeWidth = 4.dp,
                )
                Text(
                    text = "Aguarde ${countdown}s para a recompensa...",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                // Claim reward button!
                Button(
                    onClick = onAdCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESGATAR RECOMPENSA AGORA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
