package com.maverick.tetris_mobile.ui.refresh

import kotlin.math.abs
import java.util.Locale

data class RefreshRateMode(
    val width: Int,
    val height: Int,
    val rateHz: Float,
    val alternativeRatesHz: List<Float> = emptyList()
)

data class RefreshRateCapabilities(
    val ratesHz: List<Float> = emptyList(),
    val currentRateHz: Float = 0f,
    val arrSupported: Boolean = false
)

fun supportedRefreshRates(modes: List<RefreshRateMode>, width: Int, height: Int): List<Float> {
    val rates = modes.filter { it.width == width && it.height == height }
        .flatMap { listOf(it.rateHz) + it.alternativeRatesHz }
        .filter { it.isFinite() && it > 0f }
        .sorted()
    return rates.fold(mutableListOf()) { result, rate ->
        if (result.none { abs(it - rate) < 0.01f }) result.add(rate)
        result
    }
}

fun resolveRefreshRate(requestedHz: Float, supportedHz: List<Float>): Float {
    if (!requestedHz.isFinite() || requestedHz <= 0f) return 0f
    return supportedHz.filter { it.isFinite() && it > 0f }
        .minByOrNull { abs(it - requestedHz) }
        ?.takeIf { abs(it - requestedHz) < 0.5f } ?: 0f
}

fun refreshRateLabel(rateHz: Float): String =
    if (rateHz == 0f) "auto" else String.format(Locale.ROOT, "%.2f", rateHz)
        .trimEnd('0').trimEnd('.') + " Hz"
