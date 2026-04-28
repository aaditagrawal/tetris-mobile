package com.maverick.tetris_mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.HighScoreManager
import com.maverick.tetris_mobile.ui.components.GameBoard
import com.maverick.tetris_mobile.ui.components.NextPiecePreview
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

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    highScoreManager: HighScoreManager,
    onBackToStart: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()

    // Track high score rank when game ends
    var highScoreRank by remember { mutableIntStateOf(-1) }
    var hasSavedScoreForCurrentRun by remember { mutableStateOf(false) }

    fun saveCurrentScoreIfNeeded() {
        if (hasSavedScoreForCurrentRun) return
        highScoreRank = highScoreManager.addScore(
            gameState.score,
            gameState.level,
            gameState.linesCleared
        )
        hasSavedScoreForCurrentRun = true
    }

    fun restartWithSave() {
        saveCurrentScoreIfNeeded()
        viewModel.restartGame()
        highScoreRank = -1
    }

    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            saveCurrentScoreIfNeeded()
        }
    }

    LaunchedEffect(gameState.isGameOver, gameState.score, gameState.linesCleared) {
        if (!gameState.isGameOver && gameState.score == 0 && gameState.linesCleared == 0) {
            highScoreRank = -1
            hasSavedScoreForCurrentRun = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScorePanel(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.linesCleared,
                onLevelUp = { viewModel.adjustLevel(1) },
                onLevelDown = { viewModel.adjustLevel(-1) }
            )

            Spacer(modifier = Modifier.weight(0.05f))

            SwipeController(
                onSwipeLeft = { viewModel.moveLeft() },
                onSwipeRight = { viewModel.moveRight() },
                onSwipeDown = { viewModel.softDrop() },
                onSwipeUp = { viewModel.hardDrop() },
                onTap = { viewModel.rotate() },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    GameBoard(
                        gameState = gameState,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NextPiecePreview(
                            piece = gameState.nextPiece,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Rotate button
                        SideButton(
                            text = "\u21BB",
                            label = "ROTATE",
                            onClick = { viewModel.rotate() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slam (hard drop) button
                        SideButton(
                            text = "\u21E9",
                            label = "SLAM",
                            onClick = { viewModel.hardDrop() }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Pause button
                        SideButton(
                            text = "\u2759\u2759",
                            label = "PAUSE",
                            onClick = { viewModel.pauseGame() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Restart button
                        SideButton(
                            text = "\u21BA",
                            label = "RESTART",
                            onClick = { restartWithSave() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // End game button
                        SideButton(
                            text = "\u25A0",
                            label = "STOP",
                            onClick = { viewModel.endGame() }
                        )
                    }
                }
            }

            // Control hints
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlHint("\u2190 \u2192", "move")
                ControlHint("\u2193", "drop")
                ControlHint("\u2191", "slam")
                ControlHint("tap", "rotate")
            }
        }

        // Game Over overlay
        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Text(
                        text = "game over",
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = InstrumentSerif,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Final score
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

                    // High score badge
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

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat("LEVEL", gameState.level.toString())
                        MiniStat("LINES", gameState.linesCleared.toString())
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Action buttons
                    Text(
                        text = "play again",
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { restartWithSave() }
                            .padding(vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "menu",
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onBackToStart() }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Paused overlay
        if (gameState.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background.copy(alpha = 0.85f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        viewModel.resumeGame()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "paused",
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = InstrumentSerif,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = "tap to resume",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = GeistMono,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SideButton(text: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            fontSize = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = GeistMono,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            color = TextTertiary
        )
    }
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
            color = TextTertiary
        )
        Text(
            text = action,
            fontFamily = GeistMono,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            color = TextTertiary.copy(alpha = 0.6f)
        )
    }
}
