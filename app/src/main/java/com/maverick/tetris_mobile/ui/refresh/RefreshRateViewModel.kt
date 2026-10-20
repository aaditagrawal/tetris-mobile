package com.maverick.tetris_mobile.ui.refresh

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

class RefreshRateViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences by lazy {
        application.getSharedPreferences("refresh_rate", Context.MODE_PRIVATE)
    }
    private val mutableRate = MutableStateFlow(0f)
    val selectedRateHz = mutableRate.asStateFlow()
    private var userSelected = false
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { preferences.getFloat("rate_hz", 0f) }
            if (!userSelected) mutableRate.value = saved.takeIf { it.isFinite() && it >= 0f } ?: 0f
        }
    }

    fun select(rateHz: Float) {
        if (!rateHz.isFinite() || rateHz < 0f) return
        userSelected = true
        mutableRate.value = rateHz
        viewModelScope.launch(Dispatchers.IO) {
            writes.withLock {
                preferences.edit { putFloat("rate_hz", mutableRate.value) }
            }
        }
    }
}
