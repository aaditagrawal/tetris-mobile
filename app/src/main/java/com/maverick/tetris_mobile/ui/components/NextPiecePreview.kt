package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maverick.tetris_mobile.game.TetrisPiece
import com.maverick.tetris_mobile.ui.theme.LocalGamePalette
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextTertiary

@Composable
fun PiecePreview(
    label: String,
    piece: TetrisPiece?,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    compact: Boolean = false
) {
    val palette = LocalGamePalette.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = InstrumentSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 2.sp,
            color = if (dimmed) TextTertiary.copy(alpha = 0.5f) else TextTertiary
        )

        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))

        val previewSize = if (compact) 56.dp else 72.dp

        Canvas(
            modifier = Modifier
                .size(previewSize)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            piece ?: return@Canvas

            val pieceColor = palette.colorFor(piece.type).let { color ->
                if (dimmed) color.copy(alpha = 0.4f) else color
            }

            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE

            piece.forEachCell { x, y ->
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minY = minOf(minY, y)
                maxY = maxOf(maxY, y)
            }

            val pieceWidth = maxX - minX + 1
            val pieceHeight = maxY - minY + 1

            val cellSize = minOf(
                size.width / 4f,
                size.height / 4f
            )
            val cornerRadius = CornerRadius(3.dp.toPx())
            val gap = 1.dp.toPx()
            val cellDrawSize = Size(cellSize - gap * 2, cellSize - gap * 2)

            val offsetX = (size.width - pieceWidth * cellSize) / 2f
            val offsetY = (size.height - pieceHeight * cellSize) / 2f

            piece.forEachCell { cellX, cellY ->
                val x = offsetX + (cellX - minX) * cellSize
                val y = offsetY + (cellY - minY) * cellSize

                drawRoundRect(
                    color = pieceColor,
                    topLeft = Offset(x + gap, y + gap),
                    size = cellDrawSize,
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
