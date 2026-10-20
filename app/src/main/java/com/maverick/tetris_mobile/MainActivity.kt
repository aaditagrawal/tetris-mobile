package com.maverick.tetris_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maverick.tetris_mobile.ui.refresh.RefreshRateViewModel
import com.maverick.tetris_mobile.ui.refresh.rememberRefreshRateCapabilities
import com.maverick.tetris_mobile.ui.refresh.resolveRefreshRate
import com.maverick.tetris_mobile.ui.refresh.GameRefreshRateRequest
import com.maverick.tetris_mobile.ui.theme.PaletteViewModel
import com.maverick.tetris_mobile.game.GameSessionViewModel
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.HighScoreEntry
import com.maverick.tetris_mobile.game.HighScoreManager
import com.maverick.tetris_mobile.game.ScoreSaveViewModel
import com.maverick.tetris_mobile.ui.screens.GameScreen
import com.maverick.tetris_mobile.ui.screens.StartScreen
import com.maverick.tetris_mobile.ui.theme.TetrismobileTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val refreshRateViewModel: RefreshRateViewModel = viewModel()
            val selectedRefreshRate by refreshRateViewModel.selectedRateHz.collectAsStateWithLifecycle()
            val refreshCapabilities = rememberRefreshRateCapabilities(this@MainActivity)
            val refreshRate = resolveRefreshRate(selectedRefreshRate, refreshCapabilities.ratesHz)
            var refreshActive by remember { mutableStateOf(false) }
            GameRefreshRateRequest(window, refreshRate, refreshActive)
            val paletteViewModel: PaletteViewModel = viewModel()
            val palette by paletteViewModel.palette.collectAsStateWithLifecycle()
            TetrismobileTheme(palette = palette) {
                val highScoreManager = remember { HighScoreManager(applicationContext) }
                val gameViewModel: GameViewModel = viewModel()
                val scoreSaveViewModel: ScoreSaveViewModel = viewModel()
                val sessionViewModel: GameSessionViewModel = viewModel()
                val sessionReady by sessionViewModel.ready.collectAsStateWithLifecycle()
                val saveError by sessionViewModel.saveError.collectAsStateWithLifecycle()
                val resumable = remember(gameViewModel) {
                    gameViewModel.gameState.map { it.sessionId > 0 && !it.isGameOver }.distinctUntilChanged()
                }
                val hasSavedGame by resumable.collectAsStateWithLifecycle(initialValue = false)
                val menuScope = rememberCoroutineScope()
                var menuPending by remember { mutableStateOf(false) }
                var menuError by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(gameViewModel) { sessionViewModel.bind(gameViewModel) }
                var gameVisible by rememberSaveable { mutableStateOf(false) }
                // Increment to force recomposition when returning to start screen
                var refreshKey by remember { mutableIntStateOf(0) }
                var highScores by remember { mutableStateOf(emptyList<HighScoreEntry>()) }
                var scoreHistory by remember { mutableStateOf(emptyList<HighScoreEntry>()) }

                LaunchedEffect(refreshKey, sessionReady) {
                    val scores = withContext(Dispatchers.IO) {
                        highScoreManager.getHighScores() to highScoreManager.getScoreHistory()
                    }
                    highScores = scores.first
                    scoreHistory = scores.second
                }

                // Cold launches offer Continue; recreation retains the current screen.
                if (gameVisible && gameViewModel.sessionId != 0L) {
                    GameScreen(
                        viewModel = gameViewModel,
                        scoreSaveViewModel = scoreSaveViewModel,
                        highScoreManager = highScoreManager,
                        refreshRateHz = refreshRate,
                        onSaveProgress = { sessionViewModel.saveNow(gameViewModel) },
                        saveError = saveError,
                        onRefreshRateActive = { refreshActive = it },
                        onBackToStart = {
                            gameVisible = false
                            refreshKey++
                        }
                    )
                } else {
                    StartScreen(
                        hasSavedGame = sessionReady && hasSavedGame,
                        canStart = sessionReady && !menuPending,
                        saveError = menuError ?: saveError,
                        onContinue = { gameVisible = true },
                        onRetrySave = {
                            menuScope.launch {
                                try { sessionViewModel.retrySave(gameViewModel); menuError = null; refreshKey++ }
                                catch (_: java.io.IOException) { menuError = "Couldn't save. Please try again." }
                            }
                        },
                        highScores = highScores,
                        scoreHistory = scoreHistory,
                        palette = palette,
                        onSelectPalette = paletteViewModel::select,
                        refreshRateHz = selectedRefreshRate,
                        refreshCapabilities = refreshCapabilities,
                        onSelectRefreshRate = refreshRateViewModel::select,
                        onStartGame = { level, fixedLevel ->
                            if (sessionReady && !menuPending) {
                                menuPending = true
                                menuScope.launch {
                                    try {
                                        if (gameViewModel.sessionId > 0) {
                                            scoreSaveViewModel.saveScore(highScoreManager, gameViewModel.gameState.value)
                                        }
                                        gameViewModel.startGame(level, fixedLevel)
                                        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                            gameViewModel.pauseGame()
                                        }
                                        sessionViewModel.saveNow(gameViewModel)
                                        gameVisible = true
                                        menuError = null
                                    } catch (_: java.io.IOException) {
                                        gameViewModel.pauseGame()
                                        menuError = "Couldn't save. Please try again."
                                    } finally { menuPending = false }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
