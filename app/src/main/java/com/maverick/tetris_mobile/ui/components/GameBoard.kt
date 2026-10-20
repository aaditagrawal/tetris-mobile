package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import com.maverick.tetris_mobile.game.GameState
import com.maverick.tetris_mobile.game.PieceType
import com.maverick.tetris_mobile.game.TetrisPiece
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.LocalGamePalette

@Composable
fun GameBoard(
    board: List<List<PieceType?>>,
    currentPiece: TetrisPiece?,
    ghostPiece: TetrisPiece?,
    modifier: Modifier = Modifier
) {
    val palette = LocalGamePalette.current
    val boardState = rememberUpdatedState(board)
    val currentPieceState = rememberUpdatedState(currentPiece)
    val ghostPieceState = rememberUpdatedState(ghostPiece)
    val boardWidth = GameState.BOARD_WIDTH
    val boardHeight = GameState.BOARD_HEIGHT

    Box(
        modifier = modifier
            .testTag("game-board")
            .semantics { contentDescription = "Game board" }
            .aspectRatio(0.5f)
            .background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        Spacer(
            modifier = Modifier.fillMaxSize().drawWithCache {
                val cellGap = 2.dp.toPx()
                val cellCorner = CornerRadius(4.dp.toPx())
                val cellWidth = (size.width - cellGap * (boardWidth + 1)) / boardWidth
                val cellHeight = (size.height - cellGap * (boardHeight + 1)) / boardHeight
                val cellSize = Size(cellWidth, cellHeight)
                val cellOrigins = List(boardWidth * boardHeight) { index ->
                    Offset(
                        cellGap + (index % boardWidth) * (cellWidth + cellGap),
                        cellGap + (index / boardWidth) * (cellHeight + cellGap)
                    )
                }
                fun cellOrigin(x: Int, y: Int): Offset =
                    if (x in 0 until boardWidth && y in 0 until boardHeight) {
                        cellOrigins[y * boardWidth + x]
                    } else {
                        // Preserve safe drawing if a caller supplies a cell outside the visible board.
                        Offset(cellGap + x * (cellWidth + cellGap), cellGap + y * (cellHeight + cellGap))
                    }
                val gridColor = palette.textTertiary.copy(alpha = 0.08f)
                // The empty grid never changes during a run. Record its geometry once,
                // so movement draws one cached path rather than 200 separate rectangles.
                val gridPath = Path().apply {
                    cellOrigins.forEach { origin ->
                        addRoundRect(RoundRect(Rect(origin, cellSize), cellCorner))
                    }
                }

                // Piece state is read during drawing so movement retains the geometry cache.
                onDrawBehind {
                    drawPath(gridPath, gridColor)
                    val settledBoard = boardState.value
                    for (row in 0 until boardHeight) {
                        for (col in 0 until boardWidth) {
                            val type = settledBoard[row][col] ?: continue
                            drawRoundRect(
                                color = palette.colorFor(type),
                                topLeft = cellOrigins[row * boardWidth + col],
                                size = cellSize,
                                cornerRadius = cellCorner
                            )
                        }
                    }

                    ghostPieceState.value?.let { ghost ->
                        val ghostColor = palette.colorFor(ghost.type).copy(alpha = 0.28f)
                        ghost.forEachCell { x, y ->
                            drawRoundRect(
                                color = ghostColor,
                                topLeft = cellOrigin(x, y),
                                size = cellSize,
                                cornerRadius = cellCorner
                            )
                        }
                    }

                    currentPieceState.value?.let { piece ->
                        val currentColor = palette.colorFor(piece.type)
                        piece.forEachCell { x, y ->
                            drawRoundRect(
                                color = currentColor,
                                topLeft = cellOrigin(x, y),
                                size = cellSize,
                                cornerRadius = cellCorner
                            )
                        }
                    }
                }
            }
        )
    }
}
