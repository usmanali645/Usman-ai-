package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("usman_ai_settings", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(prefs.getString("theme", "Dark") ?: "Dark")
    val themeState: StateFlow<String> = _themeState.asStateFlow()

    private val _voiceResponse = MutableStateFlow(prefs.getBoolean("voice", false))
    val voiceResponse: StateFlow<Boolean> = _voiceResponse.asStateFlow()

    private val _saveHistory = MutableStateFlow(prefs.getBoolean("history", true))
    val saveHistory: StateFlow<Boolean> = _saveHistory.asStateFlow()

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _themeState.value = theme
    }

    fun setVoiceResponse(enabled: Boolean) {
        prefs.edit().putBoolean("voice", enabled).apply()
        _voiceResponse.value = enabled
    }

    fun setSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean("history", enabled).apply()
        _saveHistory.value = enabled
    }
}
