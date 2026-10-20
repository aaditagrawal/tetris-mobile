package com.maverick.tetris_mobile.game

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

data class GameSnapshot(
    val state: GameState,
    val startingLevel: Int,
    val fixedLevel: Boolean,
    val manualLevelOffset: Int,
    val remainingBag: List<PieceType>
) {
    fun isValid(): Boolean {
        if (state.sessionId <= 0 || state.sessionId == Long.MAX_VALUE || startingLevel !in 1..15 ||
            state.level !in 1..15 || state.score < 0 || state.linesCleared < 0 ||
            manualLevelOffset !in -1_000_000..1_000_000 || state.lastClearCount !in 0..4 ||
            remainingBag.size > 7 || remainingBag.distinct().size != remainingBag.size ||
            state.board.size != 20 || state.board.any { it.size != 10 }) return false
        val pieces = listOfNotNull(state.currentPiece, state.nextPiece, state.holdPiece)
        if (pieces.any { it.rotation !in 0..3 || it.position.x !in -4..10 || it.position.y !in -4..20 }) return false
        return if (state.isGameOver) state.currentPiece == null
        else state.currentPiece != null && state.nextPiece != null &&
            GameEngine.isValidPosition(state.board, state.currentPiece)
    }
}

/** Versioned, bounded checkpoint with a checksum; ghost geometry is derived on restore. */
object GameSnapshotCodec {
    fun encode(snapshot: GameSnapshot): ByteArray {
        require(snapshot.isValid())
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            val state = snapshot.state
            out.writeLong(state.sessionId)
            state.board.forEach { row -> row.forEach { out.writeByte(it?.ordinal ?: -1) } }
            fun piece(piece: TetrisPiece?) {
                out.writeByte(piece?.type?.ordinal ?: -1)
                if (piece != null) {
                    out.writeInt(piece.position.x); out.writeInt(piece.position.y); out.writeInt(piece.rotation)
                }
            }
            piece(state.currentPiece); piece(state.nextPiece); piece(state.holdPiece)
            out.writeBoolean(state.canHold); out.writeInt(state.score); out.writeInt(state.level)
            out.writeInt(state.linesCleared); out.writeInt(state.lastClearCount); out.writeBoolean(state.isGameOver)
            out.writeInt(snapshot.startingLevel); out.writeBoolean(snapshot.fixedLevel)
            out.writeInt(snapshot.manualLevelOffset); out.writeInt(snapshot.remainingBag.size)
            snapshot.remainingBag.forEach { out.writeByte(it.ordinal) }
        }
        val payload = bytes.toByteArray()
        return ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { out ->
                out.writeInt(1); out.writeLong(CRC32().apply { update(payload) }.value); out.write(payload)
            }
        }.toByteArray()
    }

    fun decode(bytes: ByteArray): GameSnapshot? = try {
        require(bytes.size in 12..1024)
        val input = DataInputStream(ByteArrayInputStream(bytes))
        require(input.readInt() == 1)
        val checksum = input.readLong()
        val payload = bytes.copyOfRange(12, bytes.size)
        require(CRC32().apply { update(payload) }.value == checksum)
        val data = DataInputStream(ByteArrayInputStream(payload))
        fun type(nullable: Boolean = true): PieceType? {
            val index = data.readByte().toInt()
            if (nullable && index == -1) return null
            return requireNotNull(PieceType.entries.getOrNull(index))
        }
        fun piece(): TetrisPiece? = type()?.let {
            TetrisPiece(it, Position(data.readInt(), data.readInt()), data.readInt())
        }
        val id = data.readLong()
        val board = List(20) { List(10) { type() } }
        val current = piece(); val next = piece(); val hold = piece()
        val state = GameState(sessionId = id, board = board, currentPiece = current,
            nextPiece = next, holdPiece = hold, canHold = data.readBoolean(), score = data.readInt(),
            level = data.readInt(), linesCleared = data.readInt(), lastClearCount = data.readInt(),
            isGameOver = data.readBoolean(), isPaused = true)
        val start = data.readInt(); val fixed = data.readBoolean(); val offset = data.readInt()
        val count = data.readInt(); require(count in 0..7)
        val snapshot = GameSnapshot(state, start, fixed, offset, List(count) { requireNotNull(type(false)) })
        require(data.available() == 0 && snapshot.isValid())
        snapshot
    } catch (_: java.io.IOException) { null } catch (_: IllegalArgumentException) { null }
}
