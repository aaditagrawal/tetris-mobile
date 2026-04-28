package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maverick.tetris_mobile.game.GameEngine
import com.maverick.tetris_mobile.game.GameState
import com.maverick.tetris_mobile.game.PieceType
import com.maverick.tetris_mobile.ui.theme.GhostPiece
import com.maverick.tetris_mobile.ui.theme.PieceBlue
import com.maverick.tetris_mobile.ui.theme.PieceCyan
import com.maverick.tetris_mobile.ui.theme.PieceGreen
import com.maverick.tetris_mobile.ui.theme.PieceOrange
import com.maverick.tetris_mobile.ui.theme.PiecePurple
import com.maverick.tetris_mobile.ui.theme.PieceRed
import com.maverick.tetris_mobile.ui.theme.PieceYellow
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder

fun PieceType.toColor(): Color = when (this) {
    PieceType.I -> PieceCyan
    PieceType.O -> PieceYellow
    PieceType.T -> PiecePurple
    PieceType.S -> PieceGreen
    PieceType.Z -> PieceRed
    PieceType.J -> PieceBlue
    PieceType.L -> PieceOrange
}

@Composable
fun GameBoard(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val boardWidth = GameState.BOARD_WIDTH
    val boardHeight = GameState.BOARD_HEIGHT

    Box(
        modifier = modifier
            .aspectRatio(0.5f)
            .background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellGap = 2.dp.toPx()
            val cellCorner = 4.dp.toPx()

            val totalGapX = cellGap * (boardWidth + 1)
            val totalGapY = cellGap * (boardHeight + 1)
            val cellWidth = (size.width - totalGapX) / boardWidth
            val cellHeight = (size.height - totalGapY) / boardHeight

            // Compute ghost piece cells
            val ghostCells = gameState.currentPiece?.let { piece ->
                GameEngine.getDropPosition(gameState.board, piece).cells()
            } ?: emptyList()

            // Compute current piece cells
            val currentCells = gameState.currentPiece?.cells() ?: emptyList()
            val currentColor = gameState.currentPiece?.type?.toColor()

            for (row in 0 until boardHeight) {
                for (col in 0 until boardWidth) {
                    val x = cellGap + col * (cellWidth + cellGap)
                    val y = cellGap + row * (cellHeight + cellGap)

                    val boardCell = gameState.board[row][col]
                    val isCurrentPiece = currentCells.any { it.x == col && it.y == row }
                    val isGhostPiece = ghostCells.any { it.x == col && it.y == row }

                    val cellColor = when {
                        isCurrentPiece && currentColor != null -> currentColor
                        boardCell != null -> boardCell.toColor()
                        isGhostPiece -> GhostPiece
                        else -> null
                    }

                    if (cellColor != null) {
                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, y),
                            size = Size(cellWidth, cellHeight),
                            cornerRadius = CornerRadius(cellCorner, cellCorner)
                        )
                    }
                }
            }
        }
    }
}
