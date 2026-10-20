package com.maverick.tetris_mobile.ui.refresh

import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberRefreshRateCapabilities(activity: ComponentActivity): RefreshRateCapabilities {
    var capabilities by remember(activity) { mutableStateOf(readCapabilities(activity)) }
    DisposableEffect(activity) {
        val manager = activity.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) { capabilities = readCapabilities(activity) }
            override fun onDisplayRemoved(displayId: Int) { capabilities = readCapabilities(activity) }
            override fun onDisplayChanged(displayId: Int) { capabilities = readCapabilities(activity) }
        }
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        // Refresh once the composition's host view has attached to its display.
        capabilities = readCapabilities(activity)
        onDispose { manager.unregisterDisplayListener(listener) }
    }
    return capabilities
}

private fun readCapabilities(activity: ComponentActivity): RefreshRateCapabilities {
    val display = activity.window.decorView.display ?: activity.display ?: return RefreshRateCapabilities()
    val currentMode = display.mode
    val modes = display.supportedModes.map {
        RefreshRateMode(it.physicalWidth, it.physicalHeight, it.refreshRate, it.alternativeRefreshRates.toList())
    } + if (Build.VERSION.SDK_INT >= 36) display.supportedRefreshRates.map {
        RefreshRateMode(currentMode.physicalWidth, currentMode.physicalHeight, it)
    } else emptyList()
    return RefreshRateCapabilities(
        ratesHz = supportedRefreshRates(modes, currentMode.physicalWidth, currentMode.physicalHeight),
        currentRateHz = display.refreshRate,
        arrSupported = Build.VERSION.SDK_INT >= 36 && display.hasArrSupport()
    )
}

@Composable
fun GameRefreshRateRequest(window: Window, requestedHz: Float, active: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(window, lifecycle, requestedHz, active) {
        val originalRate = window.attributes.preferredRefreshRate
        val originalMode = window.attributes.preferredDisplayModeId
        fun apply(rate: Float, mode: Int) {
            val attributes = window.attributes
            if (attributes.preferredRefreshRate == rate && attributes.preferredDisplayModeId == mode) return
            attributes.preferredDisplayModeId = mode
            attributes.preferredRefreshRate = rate
            window.attributes = attributes
        }
        fun update() {
            if (active && requestedHz > 0f && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                apply(requestedHz, 0)
            } else {
                apply(originalRate, originalMode)
            }
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer)
        update()
        onDispose {
            lifecycle.removeObserver(observer)
            apply(originalRate, originalMode)
        }
    }
}
