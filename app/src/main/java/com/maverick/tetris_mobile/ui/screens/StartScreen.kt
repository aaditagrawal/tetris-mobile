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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maverick.tetris_mobile.game.HighScoreEntry
import com.maverick.tetris_mobile.ui.components.RefreshRatePicker
import com.maverick.tetris_mobile.ui.refresh.RefreshRateCapabilities
import com.maverick.tetris_mobile.ui.components.PalettePicker
import com.maverick.tetris_mobile.ui.theme.GamePalette
import com.maverick.tetris_mobile.ui.theme.Background
import com.maverick.tetris_mobile.ui.theme.GeistMono
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.PieceYellow
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextPrimary
import com.maverick.tetris_mobile.ui.theme.TextSecondary
import com.maverick.tetris_mobile.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartScreen(
    highScores: List<HighScoreEntry>,
    scoreHistory: List<HighScoreEntry>,
    onStartGame: (level: Int, fixedLevel: Boolean) -> Unit,
    palette: GamePalette = GamePalette.MUTED,
    onSelectPalette: (GamePalette) -> Unit = {},
    refreshRateHz: Float = 0f,
    refreshCapabilities: RefreshRateCapabilities = RefreshRateCapabilities(),
    onSelectRefreshRate: (Float) -> Unit = {},
    hasSavedGame: Boolean = false,
    canStart: Boolean = true,
    onContinue: () -> Unit = {},
    saveError: String? = null,
    onRetrySave: () -> Unit = {}
) {
    var selectedLevel by rememberSaveable { mutableIntStateOf(1) }
    var fixedLevel by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "tetris",
                style = MaterialTheme.typography.displayLarge,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Level selector
            Text(
                text = "LEVEL",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\u2013",
                    fontFamily = GeistMono,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    color = if (selectedLevel > 1) TextSecondary else TextTertiary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { if (selectedLevel > 1) selectedLevel-- }
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = selectedLevel.toString(),
                    fontFamily = GeistMono,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    color = TextPrimary,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "+",
                    fontFamily = GeistMono,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    color = if (selectedLevel < 15) TextSecondary else TextTertiary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { if (selectedLevel < 15) selectedLevel++ }
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto / Fixed toggle
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                val autoColor = if (!fixedLevel) TextPrimary else TextTertiary
                val fixedColor = if (fixedLevel) TextPrimary else TextTertiary
                val autoBg = if (!fixedLevel) Surface else Background
                val fixedBg = if (fixedLevel) Surface else Background
                val autoBorder = if (!fixedLevel) SurfaceBorder else Background
                val fixedBorder = if (fixedLevel) SurfaceBorder else Background

                Text(
                    text = "auto",
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 1.sp,
                    color = autoColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(autoBg)
                        .border(1.dp, autoBorder, RoundedCornerShape(10.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { fixedLevel = false }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "fixed",
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 1.sp,
                    color = fixedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(fixedBg)
                        .border(1.dp, fixedBorder, RoundedCornerShape(10.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { fixedLevel = true }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Play button
            Text(
                text = "play",
                fontFamily = GeistMono,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 1.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = canStart,
                    ) { onStartGame(selectedLevel, fixedLevel) }
                    .padding(vertical = 14.dp)
            )

            if (hasSavedGame) {
                Spacer(Modifier.height(12.dp))
                Text("continue", fontFamily = GeistMono, fontSize = 16.sp, lineHeight = 24.sp,
                    color = TextPrimary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = canStart) { onContinue() }.padding(vertical = 14.dp))
                Text("Play starts a new game. Continue restores your progress.", fontFamily = GeistMono,
                    fontSize = 10.sp, lineHeight = 16.sp, color = TextTertiary,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
            if (saveError != null) {
                Text(saveError, fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 18.sp,
                    color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                Text("retry save", fontFamily = GeistMono, color = TextPrimary,
                    modifier = Modifier.clickable { onRetrySave() }.padding(12.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            PalettePicker(palette = palette, onSelect = onSelectPalette)
            RefreshRatePicker(refreshRateHz, refreshCapabilities, onSelectRefreshRate)

            Spacer(modifier = Modifier.height(32.dp))

            // High Scores
            if (highScores.isNotEmpty()) {
                Text(
                    text = "high scores",
                    fontFamily = InstrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    highScores.forEachIndexed { index, entry ->
                        HighScoreRow(rank = index + 1, entry = entry)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (scoreHistory.isNotEmpty()) {
                val timestampFormatter = remember {
                    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                }

                Text(
                    text = "score history",
                    fontFamily = InstrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    scoreHistory.forEach { entry ->
                        ScoreHistoryRow(
                            entry = entry,
                            formattedTimestamp = timestampFormatter.format(Date(entry.timestamp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Instructions
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "how to play",
                    fontFamily = InstrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                InstructionRow("swipe left / right", "move")
                InstructionRow("swipe down (hold)", "soft drop")
                InstructionRow("swipe up / slam button", "hard drop")
                InstructionRow("tap", "rotate")
                InstructionRow("hold button", "store piece")
                InstructionRow("back", "pause / menu")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScoreHistoryRow(entry: HighScoreEntry, formattedTimestamp: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.score.toString(),
                fontFamily = GeistMono,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "L${entry.level}",
                fontFamily = GeistMono,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextTertiary,
                modifier = Modifier.padding(end = 12.dp)
            )

            Text(
                text = "${entry.lines}ln",
                fontFamily = GeistMono,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formattedTimestamp,
            fontFamily = GeistMono,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun HighScoreRow(rank: Int, entry: HighScoreEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            fontFamily = GeistMono,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (rank == 1) PieceYellow else TextTertiary,
            modifier = Modifier.width(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = entry.score.toString(),
            fontFamily = GeistMono,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = if (rank == 1) TextPrimary else TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "L${entry.level}",
            fontFamily = GeistMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = TextTertiary,
            modifier = Modifier.padding(end = 12.dp)
        )

        Text(
            text = "${entry.lines}ln",
            fontFamily = GeistMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun InstructionRow(action: String, result: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = action,
            fontFamily = GeistMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            color = TextTertiary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = result,
            fontFamily = GeistMono,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            color = TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
