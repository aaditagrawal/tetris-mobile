package com.maverick.tetris_mobile.ui.refresh

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshRateCapabilitiesTest {
    @Test
    fun `available rates preserve resolution and include seamless alternatives`() {
        val rates = supportedRefreshRates(
            listOf(
                RefreshRateMode(1200, 2000, 60f),
                RefreshRateMode(1200, 2000, 120f, listOf(90f)),
                RefreshRateMode(600, 1000, 144f)
            ),
            width = 1200,
            height = 2000
        )

        assertEquals(listOf(60f, 90f, 120f), rates)
    }

    @Test
    fun `invalid modes and duplicate rates never become choices`() {
        val rates = supportedRefreshRates(
            listOf(
                RefreshRateMode(1200, 2000, 120f, listOf(60f, 60.000004f, Float.NaN)),
                RefreshRateMode(1200, 2000, 60f),
                RefreshRateMode(1200, 2000, -1f),
                RefreshRateMode(1200, 2000, Float.POSITIVE_INFINITY),
                RefreshRateMode(1200, 2000, 0f)
            ),
            width = 1200,
            height = 2000
        )

        assertEquals(2, rates.size)
        assertEquals(60f, rates[0], 0.01f)
        assertEquals(120f, rates[1], 0f)
    }

    @Test
    fun `nominal requests use the actual fractional rate reported by Android`() {
        assertEquals(59.94f, resolveRefreshRate(60f, listOf(59.94f, 119.88f)), 0f)
        assertEquals(119.88f, resolveRefreshRate(120f, listOf(59.94f, 119.88f)), 0f)
    }

    @Test
    fun `unsupported or malformed selections return to automatic`() {
        val supported = listOf(60f, 90f)
        listOf(0f, -60f, 120f, 144f, Float.NaN, Float.POSITIVE_INFINITY).forEach { request ->
            assertEquals("Request $request must release the frame-rate vote", 0f, resolveRefreshRate(request, supported), 0f)
        }
        assertEquals(0f, resolveRefreshRate(60f, emptyList()), 0f)
    }

    @Test
    fun `nonstandard hardware rates can be selected without inventing arbitrary support`() {
        assertEquals(75f, resolveRefreshRate(75f, listOf(60f, 75f)), 0f)
        assertEquals(0f, resolveRefreshRate(72f, listOf(60f, 75f)), 0f)
    }

    @Test
    fun `distinct fractional and integer rates remain independently selectable`() {
        val rates = supportedRefreshRates(
            listOf(RefreshRateMode(1200, 2000, 60f, listOf(59.94f))),
            width = 1200,
            height = 2000
        )
        assertEquals(listOf(59.94f, 60f), rates)
        assertEquals(59.94f, resolveRefreshRate(59.94f, rates), 0f)
        assertEquals(60f, resolveRefreshRate(60f, rates), 0f)
    }
}
