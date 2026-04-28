package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maverick.tetris_mobile.ui.theme.GeistMono
import com.maverick.tetris_mobile.ui.theme.TextSecondary
import com.maverick.tetris_mobile.ui.theme.TextTertiary

@Composable
fun ScorePanel(
    score: Int,
    level: Int,
    lines: Int,
    onLevelUp: () -> Unit = {},
    onLevelDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(label = "SCORE", value = score.toString())

        // Level with +/- controls
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "–",
                fontFamily = GeistMono,
                fontSize = 18.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onLevelDown() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "+",
                fontFamily = GeistMono,
                fontSize = 18.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onLevelUp() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        StatItem(label = "LINES", value = lines.toString())
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
