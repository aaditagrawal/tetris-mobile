package com.maverick.tetris_mobile

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.ui.refresh.RefreshRateViewModel
import com.maverick.tetris_mobile.ui.refresh.refreshRateLabel
import com.maverick.tetris_mobile.ui.refresh.resolveRefreshRate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class RefreshRateTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun restoreAutomatic() {
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[RefreshRateViewModel::class.java].select(0f)
        }
        waitForSavedRate(0f)
    }

    @Test
    fun supportedSelectionPersistsAndUnavailableStandardsAreDisabled() {
        val supported = supportedRates()
        val selected = resolveRefreshRate(60f, supported).takeIf { it > 0f } ?: supported.first()
        compose.onNodeWithText("refresh rate ·", substring = true).performScrollTo().performClick()
        listOf(60f, 90f, 120f, 144f).filter { resolveRefreshRate(it, supported) == 0f }.forEach {
            compose.onNodeWithText(refreshRateLabel(it)).performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithText(refreshRateLabel(selected)).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("done").assertIsDisplayed().performClick()
        waitForSavedRate(selected)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("refresh rate · ${refreshRateLabel(selected)}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun refreshRequestIsReleasedOnPauseBackgroundAndMenu() {
        val selected = supportedRates().first()
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[RefreshRateViewModel::class.java].select(selected)
        }
        compose.onNodeWithText("play").performScrollTo().performClick()
        assertRequestedRate(selected)
        compose.onNodeWithText("\u2759\u2759", useUnmergedTree = true).performClick()
        assertRequestedRate(0f)
        compose.onNodeWithText("resume").performClick()
        assertRequestedRate(selected)

        val window = compose.activity.window
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(0f, window.attributes.preferredRefreshRate, 0f)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("paused").assertIsDisplayed()
        assertRequestedRate(0f)
        compose.onNodeWithText("resume").performClick()
        assertRequestedRate(selected)
        compose.onNodeWithText("\u2759\u2759", useUnmergedTree = true).performClick()
        compose.onNodeWithText("menu").performClick()
        assertRequestedRate(0f)
    }

    @Test
    fun unsupportedSavedSelectionUsesAutomaticAndGameOverReleasesManualRequest() {
        val selected = supportedRates().first()
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[RefreshRateViewModel::class.java].select(999f)
        }
        compose.onNodeWithText("refresh rate · auto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("play").performScrollTo().performClick()
        assertRequestedRate(0f)
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[RefreshRateViewModel::class.java].select(selected)
        }
        assertRequestedRate(selected)
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[GameViewModel::class.java].endGame()
        }
        assertRequestedRate(0f)
    }

    @Test
    fun composeFrameRateVoteFollowsActiveGameplay() {
        if (Build.VERSION.SDK_INT < 35) return
        val selected = supportedRates().first()
        val requestedVoteSeen = AtomicBoolean(false)
        val lastVote = AtomicReference(Float.NaN)
        val observedDraws = AtomicInteger(0)
        val root = compose.activity.window.decorView
        val listener = ViewTreeObserver.OnDrawListener {
            root.post {
                if (Build.VERSION.SDK_INT >= 35) {
                    val rates = composeHostViews(root).map { it.requestedFrameRate }
                    if (rates.any { it.isFinite() && abs(it - selected) < 0.01f }) {
                        requestedVoteSeen.set(true)
                    }
                    lastVote.set(rates.firstOrNull { it.isFinite() && it > 0f } ?: Float.NaN)
                    observedDraws.incrementAndGet()
                }
            }
        }
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[RefreshRateViewModel::class.java].select(selected)
            root.viewTreeObserver.addOnDrawListener(listener)
        }
        try {
            compose.onNodeWithText("play").performScrollTo().performClick()
            compose.waitUntil(5_000) { requestedVoteSeen.get() }
            val drawsBeforePause = observedDraws.get()
            compose.onNodeWithText("\u2759\u2759", useUnmergedTree = true).performClick()
            compose.waitUntil(5_000) {
                observedDraws.get() > drawsBeforePause && !lastVote.get().isFinite()
            }
            requestedVoteSeen.set(false)
            compose.onNodeWithText("resume").performClick()
            compose.waitUntil(5_000) { requestedVoteSeen.get() }
        } finally {
            compose.runOnIdle { root.viewTreeObserver.removeOnDrawListener(listener) }
        }
    }

    private fun composeHostViews(view: View): List<View> {
        if (view.javaClass.simpleName == "AndroidComposeView") return listOf(view)
        if (view !is ViewGroup) return emptyList()
        return (0 until view.childCount).flatMap { composeHostViews(view.getChildAt(it)) }
    }

    private fun supportedRates(): List<Float> = compose.runOnIdle {
        val display = checkNotNull(compose.activity.display)
        val mode = display.mode
        display.supportedModes.filter {
            it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight
        }.flatMap { listOf(it.refreshRate) + it.alternativeRefreshRates.toList() }.distinct().sorted()
    }

    private fun assertRequestedRate(rate: Float) {
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(rate, compose.activity.window.attributes.preferredRefreshRate, 0.01f) }
    }

    private fun waitForSavedRate(rate: Float) {
        compose.waitUntil(5_000) {
            compose.activity.getSharedPreferences("refresh_rate", Context.MODE_PRIVATE).getFloat("rate_hz", -1f) == rate
        }
    }
}
