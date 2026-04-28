package com.maverick.tetris_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.HighScoreManager
import com.maverick.tetris_mobile.ui.screens.GameScreen
import com.maverick.tetris_mobile.ui.screens.StartScreen
import com.maverick.tetris_mobile.ui.theme.TetrismobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val highScoreManager = HighScoreManager(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            TetrismobileTheme {
                var isPlaying by remember { mutableStateOf(false) }
                // Increment to force recomposition when returning to start screen
                var refreshKey by remember { mutableIntStateOf(0) }
                val viewModel: GameViewModel = viewModel()

                if (isPlaying) {
                    GameScreen(
                        viewModel = viewModel,
                        highScoreManager = highScoreManager,
                        onBackToStart = {
                            isPlaying = false
                            refreshKey++
                        }
                    )
                } else {
                    val highScores = remember(refreshKey) {
                        highScoreManager.getHighScores()
                    }
                    val scoreHistory = remember(refreshKey) {
                        highScoreManager.getScoreHistory()
                    }
                    StartScreen(
                        highScores = highScores,
                        scoreHistory = scoreHistory,
                        onStartGame = { level, fixedLevel ->
                            viewModel.startGame(level, fixedLevel)
                            isPlaying = true
                        }
                    )
                }
            }
        }
    }
}
