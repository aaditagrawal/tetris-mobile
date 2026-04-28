package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun SwipeController(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val tapThreshold = with(density) { 12.dp.toPx() }
    val moveStepThreshold = with(density) { 28.dp.toPx() }
    val verticalTrigger = with(density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        var totalX = 0f
                        var totalY = 0f
                        var accumX = 0f
                        var accumY = 0f
                        var directionLocked = false
                        var isHorizontal = false
                        var verticalFired = false

                        while (isActive) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }

                            val delta = change.positionChange()
                            change.consume()

                            totalX += delta.x
                            totalY += delta.y
                            accumX += delta.x
                            accumY += delta.y

                            if (!directionLocked && (abs(totalX) > tapThreshold || abs(totalY) > tapThreshold)) {
                                directionLocked = true
                                isHorizontal = abs(totalX) > abs(totalY)
                            }

                            if (directionLocked) {
                                if (isHorizontal) {
                                    while (accumX > moveStepThreshold) {
                                        onSwipeRight()
                                        accumX -= moveStepThreshold
                                    }
                                    while (accumX < -moveStepThreshold) {
                                        onSwipeLeft()
                                        accumX += moveStepThreshold
                                    }
                                } else if (!verticalFired) {
                                    if (accumY > verticalTrigger) {
                                        onSwipeDown()
                                        verticalFired = true
                                    } else if (accumY < -verticalTrigger) {
                                        onSwipeUp()
                                        verticalFired = true
                                    }
                                }
                            }
                        }

                        if (!directionLocked) {
                            onTap()
                        } else if (!isHorizontal && !verticalFired) {
                            if (totalY > tapThreshold) onSwipeDown()
                            else if (totalY < -tapThreshold) onSwipeUp()
                        }
                    }
                }
            }
    ) {
        content()
    }
}
