package com.maverick.tetris_mobile.ui.theme

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PaletteViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences by lazy {
        application.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    }
    private val mutablePalette = MutableStateFlow(GamePalette.MUTED)
    val palette = mutablePalette.asStateFlow()
    private var userSelected = false
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                GamePalette.fromId(preferences.getString("palette", null))
            }
            // A tap during loading takes precedence over the previous preference.
            if (!userSelected) mutablePalette.value = saved
        }
    }

    fun select(palette: GamePalette) {
        userSelected = true
        mutablePalette.value = palette
        viewModelScope.launch(Dispatchers.IO) {
            writes.withLock {
                // Concurrent rapid selections always persist the most recent choice.
                preferences.edit { putString("palette", mutablePalette.value.id) }
            }
        }
    }
}
