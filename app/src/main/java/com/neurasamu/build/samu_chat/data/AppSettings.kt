package com.neurasamu.build.samu_chat.data

import android.content.Context
import androidx.core.content.edit

data class AppSettings(
    val theme: String = "dark", // "light", "dark", "system"
    val fontSizeMultiplier: Float = 1.0f, // 0.8 to 1.4
    val customInstructions: String = "",
    val customInstructionsEnabled: Boolean = true
) {
    companion object {
        private const val PREFS = "app_settings"
        private const val K_THEME = "theme"
        private const val K_FONT = "font_size"
        private const val K_CI = "custom_instructions"
        private const val K_CI_ENABLED = "ci_enabled"

        fun load(ctx: Context): AppSettings {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return AppSettings(
                theme = p.getString(K_THEME, "dark") ?: "dark",
                fontSizeMultiplier = p.getFloat(K_FONT, 1.0f),
                customInstructions = p.getString(K_CI, "") ?: "",
                customInstructionsEnabled = p.getBoolean(K_CI_ENABLED, true)
            )
        }

        fun save(ctx: Context, s: AppSettings) {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            p.edit {
                putString(K_THEME, s.theme)
                putFloat(K_FONT, s.fontSizeMultiplier)
                putString(K_CI, s.customInstructions)
                putBoolean(K_CI_ENABLED, s.customInstructionsEnabled)
            }
        }
    }
}
