package com.maverick.tetris_mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maverick.tetris_mobile.game.TetrisPiece
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.ScoreSaveViewModel
import com.maverick.tetris_mobile.game.HighScoreManager
import com.maverick.tetris_mobile.ui.components.GameBoard
import com.maverick.tetris_mobile.ui.components.PiecePreview
import com.maverick.tetris_mobile.ui.components.ScorePanel
import com.maverick.tetris_mobile.ui.components.SwipeController
import com.maverick.tetris_mobile.ui.theme.Background
import com.maverick.tetris_mobile.ui.theme.GeistMono
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.PieceYellow
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextPrimary
import com.maverick.tetris_mobile.ui.theme.TextSecondary
import com.maverick.tetris_mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    scoreSaveViewModel: ScoreSaveViewModel,
    highScoreManager: HighScoreManager,
    onBackToStart: () -> Unit,
    refreshRateHz: Float = 0f,
    onRefreshRateActive: (Boolean) -> Unit = {},
    onSaveProgress: suspend () -> Unit = {},
    saveError: String? = null
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    var foregroundActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var refreshForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.pauseGame()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { refreshForeground = true; foregroundActive = true }
                Lifecycle.Event.ON_PAUSE -> {
                    refreshForeground = false
                    foregroundActive = false
                    viewModel.pauseGame()
                }
                Lifecycle.Event.ON_START -> foregroundActive = true
                Lifecycle.Event.ON_STOP -> {
                    foregroundActive = false
                    viewModel.pauseGame()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!viewModel.gameState.value.isGameOver) viewModel.pauseGame()
        }
    }

    val refreshActive = foregroundActive && refreshForeground && !gameState.isPaused && !gameState.isGameOver
    val reportRefreshActive by rememberUpdatedState(onRefreshRateActive)
    DisposableEffect(refreshActive) {
        reportRefreshActive(refreshActive)
        onDispose { reportRefreshActive(false) }
    }

    var highScoreRank by remember(gameState.sessionId) { mutableIntStateOf(-1) }
    var transitionPending by remember(gameState.sessionId) { mutableStateOf(false) }
    var hintsHidden by remember { mutableStateOf(false) }
    var transitionError by remember { mutableStateOf<String?>(null) }

    suspend fun saveCurrentScoreIfNeeded() {
        // The job belongs to a retained ViewModel, so leaving this screen cannot cancel it.
        highScoreRank = scoreSaveViewModel.saveScore(
            highScoreManager,
            gameState
        )
    }

    fun restartWithSave() {
        if (transitionPending) return
        transitionPending = true
        scope.launch {
            try {
                saveCurrentScoreIfNeeded()
                viewModel.restartGame()
                onSaveProgress()
                highScoreRank = -1
            } catch (_: java.io.IOException) {
                viewModel.pauseGame()
                transitionError = "Couldn't save. Please try again."
                transitionPending = false
            }
        }
    }

    fun menuWithSave() {
        if (transitionPending) return
        transitionPending = true
        scope.launch {
            try {
                if (gameState.isGameOver) saveCurrentScoreIfNeeded()
                onSaveProgress()
                onBackToStart()
            } catch (_: java.io.IOException) {
                viewModel.pauseGame()
                transitionError = "Couldn't save. Please try again."
                transitionPending = false
            }
        }
    }

    fun resumeIfReady() {
        if (!transitionPending) viewModel.resumeGame()
    }

    BackHandler {
        when {
            gameState.isGameOver || gameState.isPaused -> menuWithSave()
            else -> viewModel.pauseGame()
        }
    }

    LaunchedEffect(gameState.sessionId, gameState.isGameOver) {
        if (gameState.isGameOver) {
            try { saveCurrentScoreIfNeeded() } catch (_: java.io.IOException) {
                transitionError = "Couldn't save score. Please try again."
            }
        }
    }

    LaunchedEffect(gameState.linesCleared, gameState.lastClearCount) {
        if (gameState.lastClearCount > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val lightHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val strongHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxHeight < 760.dp
        val dense = maxHeight < 500.dp || (fontScale > 1.4f && maxHeight < 800.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScorePanel(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.linesCleared,
                modifier = Modifier.widthIn(max = 600.dp)
            )

            Spacer(modifier = Modifier.weight(0.05f))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val compactButtonSize = maxOf(48f, 36f * fontScale + 8f).dp
                val sidebarWidth = when {
                    dense -> maxOf(128.dp, compactButtonSize * 2 + 16.dp)
                    compact -> maxOf(56.dp, compactButtonSize)
                    else -> maxOf(72f, 42f * fontScale + 8f).dp
                }
                val boardWidth = minOf(maxWidth - sidebarWidth - 16.dp, maxHeight / 2)
                Row(
                    modifier = Modifier.width(boardWidth + sidebarWidth + 16.dp).height(boardWidth * 2),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    SwipeController(
                        enabled = foregroundActive && !gameState.isPaused && !gameState.isGameOver,
                        onSwipeLeft = {
                            if (viewModel.moveLeft()) lightHaptic()
                        },
                        onSwipeRight = {
                            if (viewModel.moveRight()) lightHaptic()
                        },
                        onSwipeDown = { viewModel.softDrop() },
                        onSwipeUp = {
                            viewModel.hardDrop()
                            strongHaptic()
                        },
                        onTap = {
                            if (viewModel.rotate()) lightHaptic()
                        },
                        onInteraction = { hintsHidden = true },
                        modifier = Modifier.width(boardWidth)
                    ) {
                        GameBoard(
                            board = gameState.board,
                            currentPiece = gameState.currentPiece,
                            ghostPiece = gameState.ghostPiece,
                            modifier = Modifier.fillMaxWidth().then(
                                if (refreshActive && refreshRateHz > 0f) Modifier.preferredFrameRate(refreshRateHz)
                                else Modifier
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    GameSidebar(
                        holdPiece = gameState.holdPiece,
                        nextPiece = gameState.nextPiece,
                        canHold = gameState.canHold,
                        compact = compact,
                        dense = dense,
                        modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
                        onRotate = { if (viewModel.rotate()) lightHaptic() },
                        onSlam = { viewModel.hardDrop(); strongHaptic() },
                        onHold = { viewModel.hold(); lightHaptic() },
                        onPause = { viewModel.pauseGame() }
                    )
                }
            }

            if (!hintsHidden && !dense) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlHint("\u2190 \u2192", "move")
                    ControlHint("\u2193", "soft drop")
                    ControlHint("\u2191", "slam")
                    ControlHint("tap", "rotate")
                }
            }
        }

        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "game over",
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = InstrumentSerif,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = gameState.score.toString(),
                        fontFamily = GeistMono,
                        fontSize = 36.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "POINTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (highScoreRank in 1..HighScoreManager.MAX_ENTRIES) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "#$highScoreRank high score",
                            fontFamily = InstrumentSerif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            color = PieceYellow
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat("LEVEL", gameState.level.toString())
                        MiniStat("LINES", gameState.linesCleared.toString())
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    OverlayButton(
                        text = "play again",
                        primary = true,
                        onClick = { restartWithSave() }
                    )

                    if (transitionError != null || saveError != null) {
                        Text(transitionError ?: saveError.orEmpty(), color = TextSecondary,
                            fontFamily = GeistMono, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OverlayButton(
                        text = "menu",
                        primary = false,
                        onClick = { menuWithSave() }
                    )
                }
            }
        }

        if (gameState.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.85f))
,
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* consume taps on card */ }
                        .padding(horizontal = 48.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = "paused",
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = InstrumentSerif,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (transitionPending) "saving progress…" else "progress saves automatically",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = GeistMono,
                        color = TextTertiary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OverlayButton(
                        text = "resume",
                        primary = true,
                        onClick = { resumeIfReady() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OverlayButton(
                        text = "restart",
                        primary = false,
                        onClick = { restartWithSave() }
                    )

                    if (transitionError != null || saveError != null) {
                        Text(transitionError ?: saveError.orEmpty(), color = TextSecondary,
                            fontFamily = GeistMono, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OverlayButton(
                        text = "menu",
                        primary = false,
                        onClick = { menuWithSave() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "–",
                            fontFamily = GeistMono,
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.adjustLevel(-1) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        Text(
                            text = "LEVEL ${gameState.level}",
                            fontFamily = GeistMono,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = TextTertiary
                        )

                        Text(
                            text = "+",
                            fontFamily = GeistMono,
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.adjustLevel(1) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameSidebar(
    holdPiece: TetrisPiece?,
    nextPiece: TetrisPiece?,
    canHold: Boolean,
    compact: Boolean,
    dense: Boolean,
    modifier: Modifier,
    onRotate: () -> Unit,
    onSlam: () -> Unit,
    onHold: () -> Unit,
    onPause: () -> Unit
) {
    val gap = if (compact) 8.dp else 16.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (dense) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PiecePreview("HOLD", holdPiece, dimmed = !canHold, compact = true)
                PiecePreview("NEXT", nextPiece, compact = true)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SideButton("\u21BB", "ROTATE", onRotate, compact = true)
                SideButton("\u21E9", "SLAM", onSlam, compact = true)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SideButton("\u21C4", "HOLD", onHold, enabled = canHold, compact = true)
                SideButton("\u2759\u2759", "PAUSE", onPause, compact = true)
            }
        } else {
            PiecePreview("HOLD", holdPiece, modifier = Modifier.padding(top = 4.dp),
                dimmed = !canHold, compact = compact)
            Spacer(Modifier.height(gap))
            PiecePreview("NEXT", nextPiece, compact = compact)
            Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
            SideButton("\u21BB", "ROTATE", onRotate, compact = compact)
            Spacer(Modifier.height(gap))
            SideButton("\u21E9", "SLAM", onSlam, compact = compact)
            Spacer(Modifier.height(gap))
            SideButton("\u21C4", "HOLD", onHold, enabled = canHold, compact = compact)
            Spacer(Modifier.weight(1f))
            SideButton("\u2759\u2759", "PAUSE", onPause, compact = compact)
        }
    }
}

@Composable
private fun SideButton(
    text: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val fontScale = LocalDensity.current.fontScale
    val buttonSize = if (compact) maxOf(48f, 36f * fontScale + 8f).dp
        else maxOf(48f, 24f * fontScale + 12f).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = text, fontSize = if (compact) 18.sp else 20.sp,
                    lineHeight = if (compact) 20.sp else 24.sp,
                    color = TextSecondary, textAlign = TextAlign.Center)
                if (compact) {
                    Text(text = label, fontFamily = GeistMono, fontSize = 9.sp,
                        lineHeight = 12.sp, letterSpacing = 0.5.sp, color = TextTertiary)
                }
            }
        }
        if (!compact) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontFamily = GeistMono, fontSize = 9.sp,
                lineHeight = 12.sp, letterSpacing = 1.5.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun OverlayButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontFamily = GeistMono,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
        color = if (primary) TextPrimary else TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) Surface else Background)
            .border(1.dp, if (primary) SurfaceBorder else Background, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        Text(
            text = value,
            fontFamily = GeistMono,
            fontSize = 18.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ControlHint(gesture: String, action: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = gesture,
            fontFamily = GeistMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = TextTertiary
        )
        Text(
            text = action,
            fontFamily = GeistMono,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.sp,
            color = TextTertiary.copy(alpha = 0.6f)
        )
    }
}
