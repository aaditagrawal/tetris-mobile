package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class HorizontalDir { LEFT, RIGHT }

@Composable
fun SwipeController(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onInteraction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val swipeLeft = rememberUpdatedState(onSwipeLeft)
    val swipeRight = rememberUpdatedState(onSwipeRight)
    val swipeDown = rememberUpdatedState(onSwipeDown)
    val swipeUp = rememberUpdatedState(onSwipeUp)
    val tap = rememberUpdatedState(onTap)
    val interaction = rememberUpdatedState(onInteraction)
    val density = LocalDensity.current
    val tapThreshold = with(density) { 12.dp.toPx() }
    val moveStepThreshold = with(density) { 28.dp.toPx() }
    val verticalTrigger = with(density) { 24.dp.toPx() }
    val dasDelayMs = 170L
    val arrIntervalMs = 40L
    val softDropIntervalMs = 50L

    Box(
        modifier = modifier
            .pointerInput(enabled, tapThreshold, moveStepThreshold, verticalTrigger) {
                if (!enabled) return@pointerInput
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val pointerId = down.id
                        var released = false
                        var reverseDistance = 0f
                        var totalX = 0f
                        var totalY = 0f
                        var accumX = 0f
                        var accumY = 0f
                        var directionLocked = false
                        var isHorizontal = false
                        var verticalDown = false
                        var verticalUp = false
                        var horizDir: HorizontalDir? = null

                        var dasJob: Job? = null
                        var softDropJob: Job? = null

                        fun startHorizontalRepeat(dir: HorizontalDir) {
                            dasJob?.cancel()
                            dasJob = launch {
                                delay(dasDelayMs)
                                while (isActive) {
                                    when (dir) {
                                        HorizontalDir.LEFT -> swipeLeft.value()
                                        HorizontalDir.RIGHT -> swipeRight.value()
                                    }
                                    delay(arrIntervalMs)
                                }
                            }
                        }

                        fun startSoftDropRepeat() {
                            softDropJob?.cancel()
                            softDropJob = launch {
                                delay(softDropIntervalMs)
                                while (isActive) {
                                    swipeDown.value()
                                    delay(softDropIntervalMs)
                                }
                            }
                        }

                        try {
                            while (isActive) {
                                val event = awaitPointerEvent()
                                val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId } ?: break

                                if (change.changedToUp()) {
                                    released = true
                                    change.consume()
                                    break
                                }

                                if (!change.pressed) break

                                val delta = change.positionChange()
                                change.consume()

                                totalX += delta.x
                                totalY += delta.y
                                accumX += delta.x
                                accumY += delta.y

                                if (!directionLocked && (abs(totalX) > tapThreshold || abs(totalY) > tapThreshold)) {
                                    directionLocked = true
                                    isHorizontal = abs(totalX) > abs(totalY)
                                    interaction.value()

                                    if (isHorizontal) {
                                        horizDir = if (totalX >= 0) HorizontalDir.RIGHT else HorizontalDir.LEFT
                                        when (horizDir) {
                                            HorizontalDir.LEFT -> swipeLeft.value()
                                            HorizontalDir.RIGHT -> swipeRight.value()
                                        }
                                        startHorizontalRepeat(horizDir)
                                    }
                                }

                                if (directionLocked) {
                                    if (isHorizontal) {
                                        val motionDir = when {
                                            delta.x > 0f -> HorizontalDir.RIGHT
                                            delta.x < 0f -> HorizontalDir.LEFT
                                            else -> null
                                        }
                                        if (motionDir != null && motionDir != horizDir) {
                                            reverseDistance += abs(delta.x)
                                            if (reverseDistance > tapThreshold) {
                                                horizDir = motionDir
                                                accumX = (reverseDistance - tapThreshold) *
                                                    if (motionDir == HorizontalDir.RIGHT) 1f else -1f
                                                reverseDistance = 0f
                                                when (motionDir) {
                                                    HorizontalDir.LEFT -> swipeLeft.value()
                                                    HorizontalDir.RIGHT -> swipeRight.value()
                                                }
                                                interaction.value()
                                                startHorizontalRepeat(motionDir)
                                            }
                                        } else if (motionDir != null) {
                                            reverseDistance = 0f
                                        }
                                        while (accumX > moveStepThreshold) {
                                            swipeRight.value()
                                            accumX -= moveStepThreshold
                                            interaction.value()
                                        }
                                        while (accumX < -moveStepThreshold) {
                                            swipeLeft.value()
                                            accumX += moveStepThreshold
                                            interaction.value()
                                        }
                                    } else if (!verticalDown && !verticalUp) {
                                        if (accumY > verticalTrigger) {
                                            verticalDown = true
                                            swipeDown.value()
                                            interaction.value()
                                            startSoftDropRepeat()
                                        } else if (accumY < -verticalTrigger) {
                                            verticalUp = true
                                            swipeUp.value()
                                            interaction.value()
                                        }
                                    }
                                }
                            }
                        } finally {
                            dasJob?.cancel()
                            softDropJob?.cancel()
                        }

                        if (!released) return@awaitEachGesture

                        if (!directionLocked) {
                            tap.value()
                            interaction.value()
                        } else if (!isHorizontal && !verticalDown && !verticalUp) {
                            if (totalY > tapThreshold) {
                                swipeDown.value()
                                interaction.value()
                            } else if (totalY < -tapThreshold) {
                                swipeUp.value()
                                interaction.value()
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
