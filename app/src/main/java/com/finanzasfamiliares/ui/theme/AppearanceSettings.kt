package com.finanzasfamiliares.ui.theme

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

enum class AppAccentColor(val storageKey: String) {
    GREEN("green"),
    BLUE("blue"),
    TEAL("teal"),
    INDIGO("indigo"),
    VIOLET("violet"),
    SLATE("slate");

    companion object {
        fun fromStorageKey(value: String?): AppAccentColor =
            entries.firstOrNull { it.storageKey == value } ?: GREEN
    }
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromStorageKey(prefs.getString(PREF_THEME_MODE, null))
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(
        AppAccentColor.fromStorageKey(prefs.getString(PREF_ACCENT_COLOR, null))
    )
    val accentColor: StateFlow<AppAccentColor> = _accentColor.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(PREF_THEME_MODE, mode.storageKey).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(color: AppAccentColor) {
        prefs.edit().putString(PREF_ACCENT_COLOR, color.storageKey).apply()
        _accentColor.value = color
    }

    private companion object {
        const val PREFS_NAME = "finanzas_familiares_appearance"
        const val PREF_THEME_MODE = "theme_mode"
        const val PREF_ACCENT_COLOR = "accent_color"
    }
}
