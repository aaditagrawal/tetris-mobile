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
import androidx.compose.material3.MaterialTheme
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
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextTertiary

@Composable
fun NextPiecePreview(
    piece: TetrisPiece?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NEXT",
            fontFamily = InstrumentSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val previewSize = 72.dp

        Canvas(
            modifier = Modifier
                .size(previewSize)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            piece ?: return@Canvas

            val cells = piece.cells()
            val pieceColor = piece.type.toColor()

            // Find bounding box of piece cells relative to piece position
            val minX = cells.minOf { it.x }
            val maxX = cells.maxOf { it.x }
            val minY = cells.minOf { it.y }
            val maxY = cells.maxOf { it.y }
            val pieceWidth = maxX - minX + 1
            val pieceHeight = maxY - minY + 1

            val cellSize = minOf(
                size.width / 4f,
                size.height / 4f
            )
            val cornerRadius = 3.dp.toPx()

            // Center the piece in the canvas
            val offsetX = (size.width - pieceWidth * cellSize) / 2f
            val offsetY = (size.height - pieceHeight * cellSize) / 2f

            for (cell in cells) {
                val x = offsetX + (cell.x - minX) * cellSize
                val y = offsetY + (cell.y - minY) * cellSize
                val gap = 1.dp.toPx()

                drawRoundRect(
                    color = pieceColor,
                    topLeft = Offset(x + gap, y + gap),
                    size = Size(cellSize - gap * 2, cellSize - gap * 2),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }
}
