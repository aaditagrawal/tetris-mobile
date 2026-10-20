package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SwipeControllerTest {
    @get:Rule
    val compose = createComposeRule()

    private val left = AtomicInteger()
    private val right = AtomicInteger()
    private val down = AtomicInteger()
    private val up = AtomicInteger()
    private val taps = AtomicInteger()

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag("controls").performTouchInput(block)
        compose.waitForIdle()
    }

    private fun showController() {
        compose.setContent {
            SwipeController(
                onSwipeLeft = { left.incrementAndGet() },
                onSwipeRight = { right.incrementAndGet() },
                onSwipeDown = { down.incrementAndGet() },
                onSwipeUp = { up.incrementAndGet() },
                onTap = { taps.incrementAndGet() },
                modifier = Modifier.size(320.dp).testTag("controls")
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun reversingDragSwitchesRepeatDirection() {
        showController()
        touch {
            down(center)
            moveTo(center + Offset(100f, 0f))
        }
        val initialRight = right.get()
        compose.waitUntil(2_000) { right.get() > initialRight }

        touch {
            moveTo(center - Offset(100f, 0f))
        }
        val rightAfterReversal = right.get()
        val leftAfterReversal = left.get()
        compose.waitUntil(2_000) { left.get() > leftAfterReversal }
        assertEquals(rightAfterReversal, right.get())
        touch { up() }
        assertEquals(0, taps.get())
    }

    @Test
    fun secondaryFingerDoesNotMoveOrReleaseTheInitiatingGesture() {
        showController()
        touch {
            down(0, center)
            down(1, center + Offset(0f, 40f))
            moveTo(1, center + Offset(100f, 40f))
            up(1)
        }
        assertEquals(0, right.get())
        assertEquals(0, taps.get())
        touch { up(0) }
        assertEquals(1, taps.get())
    }

    @Test
    fun cancellingTouchDoesNotRotate() {
        showController()
        touch {
            down(center)
            cancel()
        }
        assertEquals(0, taps.get())
    }

    @Test
    fun disablingControllerCancelsHeldRepeat() {
        val enabled = mutableStateOf(true)
        compose.setContent {
            SwipeController(
                onSwipeLeft = {},
                onSwipeRight = { right.incrementAndGet() },
                onSwipeDown = {},
                onSwipeUp = {},
                onTap = { taps.incrementAndGet() },
                modifier = Modifier.size(320.dp).testTag("controls"),
                enabled = enabled.value
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
        touch {
            down(center)
            moveTo(center + Offset(100f, 0f))
        }
        val beforeRepeat = right.get()
        compose.waitUntil(2_000) { right.get() > beforeRepeat }
        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        val afterDisable = right.get()
        // Wait longer than the initial repeat delay to detect a leaked coroutine.
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        assertEquals(afterDisable, right.get())
        touch { up() }
        assertEquals(0, taps.get())
    }

    @Test
    fun recompositionUpdatesCallbacksWithoutRestartingHeldGesture() {
        val useNewCallback = mutableStateOf(false)
        val oldCallback = AtomicInteger()
        val newCallback = AtomicInteger()
        compose.setContent {
            val currentRight = if (useNewCallback.value) newCallback else oldCallback
            SwipeController(
                onSwipeLeft = {},
                onSwipeRight = { currentRight.incrementAndGet() },
                onSwipeDown = {},
                onSwipeUp = {},
                onTap = {},
                modifier = Modifier.size(320.dp).testTag("controls")
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
        touch {
            down(center)
            moveTo(center + Offset(100f, 0f))
        }
        assertTrue(oldCallback.get() > 0)
        compose.runOnIdle { useNewCallback.value = true }
        compose.waitUntil(2_000) { newCallback.get() > 0 }
        touch { up() }
    }
}
