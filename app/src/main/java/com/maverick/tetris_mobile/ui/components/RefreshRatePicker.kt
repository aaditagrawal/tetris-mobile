package com.maverick.tetris_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maverick.tetris_mobile.ui.refresh.RefreshRateCapabilities
import com.maverick.tetris_mobile.ui.refresh.refreshRateLabel
import com.maverick.tetris_mobile.ui.refresh.resolveRefreshRate
import com.maverick.tetris_mobile.ui.theme.GeistMono
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextPrimary
import com.maverick.tetris_mobile.ui.theme.TextSecondary

@Composable
fun RefreshRatePicker(selectedHz: Float, capabilities: RefreshRateCapabilities, onSelect: (Float) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val effectiveHz = resolveRefreshRate(selectedHz, capabilities.ratesHz)
    Text("refresh rate · ${refreshRateLabel(effectiveHz)}", fontFamily = GeistMono,
        fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Choose refresh rate") { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp))

    if (open) {
        val standardRates = listOf(60f, 90f, 120f, 144f)
        val extraRates = capabilities.ratesHz.filter { detected ->
            standardRates.none { resolveRefreshRate(it, capabilities.ratesHz) == detected }
        }
        val choices = listOf(0f) + (standardRates + extraRates).sorted()
        Dialog(onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.padding(16.dp).widthIn(max = 360.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(Surface)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)).padding(24.dp)) {
                Text("refresh rate", fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic,
                    fontSize = 28.sp, lineHeight = 32.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display now: ${refreshRateLabel(capabilities.currentRateHz)}",
                        fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
                    Text(if (capabilities.arrSupported) "Adaptive refresh is supported. Auto lets Android adjust to the game."
                        else "Auto lets Android choose. Adaptive refresh is not reported by this display.",
                        fontFamily = GeistMono, fontSize = 11.sp, lineHeight = 18.sp, color = TextSecondary)
                    Text("Higher rates may feel smoother and use more battery. Android may lower your requested rate to save power or manage heat.",
                        fontFamily = GeistMono, fontSize = 11.sp, lineHeight = 18.sp, color = TextSecondary)
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        choices.forEach { nominalHz ->
                            val actualHz = resolveRefreshRate(nominalHz, capabilities.ratesHz)
                            val available = nominalHz == 0f || actualHz > 0f
                            val selected = available && effectiveHz == actualHz
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .selectable(selected = selected, enabled = available, role = Role.RadioButton) {
                                    onSelect(actualHz)
                                }.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, enabled = available, onClick = null,
                                    modifier = Modifier.size(24.dp))
                                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text(refreshRateLabel(if (actualHz > 0f) actualHz else nominalHz),
                                        fontFamily = GeistMono, fontSize = 14.sp, lineHeight = 20.sp,
                                        color = if (available) TextPrimary else TextSecondary.copy(alpha = 0.5f))
                                    if (!available) Text("Unavailable on this display", fontFamily = GeistMono,
                                        fontSize = 10.sp, lineHeight = 16.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { open = false }.padding(12.dp),
                    contentAlignment = Alignment.Center) {
                    Text("done", fontFamily = GeistMono, fontSize = 14.sp,
                        lineHeight = 20.sp, color = TextPrimary)
                }
            }
        }
    }
}
