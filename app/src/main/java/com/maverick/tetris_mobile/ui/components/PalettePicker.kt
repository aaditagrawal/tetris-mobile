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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
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
import com.maverick.tetris_mobile.game.PieceType
import com.maverick.tetris_mobile.ui.theme.GamePalette
import com.maverick.tetris_mobile.ui.theme.GeistMono
import com.maverick.tetris_mobile.ui.theme.InstrumentSerif
import com.maverick.tetris_mobile.ui.theme.Surface
import com.maverick.tetris_mobile.ui.theme.SurfaceBorder
import com.maverick.tetris_mobile.ui.theme.TextPrimary
import com.maverick.tetris_mobile.ui.theme.TextSecondary

@Composable
fun PalettePicker(palette: GamePalette, onSelect: (GamePalette) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Choose color palette") { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("palette · ${palette.title}", fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
        PaletteSwatches(palette)
    }

    if (open) {
        Dialog(onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text("color palettes", fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic,
                    fontSize = 28.sp, lineHeight = 32.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Choose your colors", fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier.weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GamePalette.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(option.background)
                                .border(1.dp, if (option == palette) TextPrimary else option.border, RoundedCornerShape(12.dp))
                                .selectable(selected = option == palette, role = Role.RadioButton) { onSelect(option) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option == palette, onClick = null, modifier = Modifier.size(20.dp))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(option.title, fontFamily = GeistMono, fontSize = 14.sp, lineHeight = 20.sp, color = option.textPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(option.description, fontFamily = GeistMono, fontSize = 11.sp, lineHeight = 16.sp, color = option.textSecondary)
                                Spacer(Modifier.height(12.dp))
                                PaletteSwatches(option)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { open = false }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("done", fontFamily = GeistMono, fontSize = 14.sp,
                        lineHeight = 20.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatches(palette: GamePalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PieceType.entries.forEach { type ->
            Box(Modifier.size(16.dp).background(palette.colorFor(type), RoundedCornerShape(4.dp)))
        }
    }
}
