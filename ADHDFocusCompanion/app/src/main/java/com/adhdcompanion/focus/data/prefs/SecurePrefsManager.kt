package com.adhdcompanion.focus.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the Gemini API key and small user settings locally, encrypted at
 * rest with a hardware-backed master key. Nothing in here ever leaves the
 * device except the one outbound HTTPS call the user's own key authorizes.
 */
class SecurePrefsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var useFlashLiteFallbackModel: Boolean
        get() = prefs.getBoolean(KEY_USE_FALLBACK_MODEL, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_FALLBACK_MODEL, value).apply()

    var userDisplayName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    /** 24h local time, e.g. "07:30", for the daily wake/transition briefing alarm. */
    var morningBriefingTime: String?
        get() = prefs.getString(KEY_MORNING_TIME, null)
        set(value) = prefs.edit().putString(KEY_MORNING_TIME, value).apply()

    var voicePreference: String
        get() = prefs.getString(KEY_VOICE_PREF, VOICE_GEMINI_WITH_FALLBACK) ?: VOICE_GEMINI_WITH_FALLBACK
        set(value) = prefs.edit().putString(KEY_VOICE_PREF, value).apply()

    fun hasApiKey(): Boolean = !geminiApiKey.isNullOrBlank()

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_USE_FALLBACK_MODEL = "use_fallback_model"
        private const val KEY_USER_NAME = "user_display_name"
        private const val KEY_MORNING_TIME = "morning_briefing_time"
        private const val KEY_VOICE_PREF = "voice_preference"

        const val VOICE_GEMINI_WITH_FALLBACK = "gemini_with_fallback"
        const val VOICE_ANDROID_ONLY = "android_only"
    }
}
