package com.lifeos.app.core.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifeos.app.core.security.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_settings")

enum class AppLockType { NONE, BIOMETRIC, PIN }

data class DailyAlarm(val hour: Int, val minute: Int) {
    val minutesSinceMidnight: Int get() = hour * 60 + minute
}

/**
 * Central app settings — Section 59 (Settings screen). No settings are
 * ever synced off-device except through the explicit Backup/Export flow.
 *
 * SECURITY NOTE: the App PIN and recovery answer are NEVER stored in
 * plaintext — only a random salt + SHA-256 hash of each (see
 * core/security/PinHasher.kt). This class never exposes the raw PIN or
 * recovery answer back out; only hash verification is possible.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AI_FEATURES_ENABLED = booleanPreferencesKey("ai_features_enabled")

        val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val RECOVERY_QUESTION = stringPreferencesKey("recovery_question")
        val RECOVERY_ANSWER_SALT = stringPreferencesKey("recovery_answer_salt")
        val RECOVERY_ANSWER_HASH = stringPreferencesKey("recovery_answer_hash")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val ALARM_HOUR = androidx.datastore.preferences.core.intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = androidx.datastore.preferences.core.intPreferencesKey("alarm_minute")
        val ALARM_TIMES = stringSetPreferencesKey("alarm_times")
    }

    val darkThemeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME_ENABLED] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val aiFeaturesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_FEATURES_ENABLED] ?: false }

    val appLockType: Flow<AppLockType> = context.dataStore.data.map {
        it[Keys.APP_LOCK_TYPE]?.let { name -> runCatching { AppLockType.valueOf(name) }.getOrNull() } ?: AppLockType.NONE
    }
    val recoveryQuestion: Flow<String?> = context.dataStore.data.map { it[Keys.RECOVERY_QUESTION] }
    val alarmEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALARM_ENABLED] ?: false }
    val alarmHour: Flow<Int> = context.dataStore.data.map { it[Keys.ALARM_HOUR] ?: 6 }
    val alarmMinute: Flow<Int> = context.dataStore.data.map { it[Keys.ALARM_MINUTE] ?: 0 }
    /** Multiple daily alarms, persisted in DataStore. The legacy single-alarm keys are
     * retained so existing installs migrate without losing the user's current alarm. */
    val alarmTimes: Flow<List<DailyAlarm>> = context.dataStore.data.map { prefs ->
        val stored = prefs[Keys.ALARM_TIMES].orEmpty()
            .mapNotNull { value ->
                val parts = value.split(":")
                if (parts.size != 2) return@mapNotNull null
                val hour = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: return@mapNotNull null
                val minute = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: return@mapNotNull null
                DailyAlarm(hour, minute)
            }
            .distinctBy { it.minutesSinceMidnight }
            .sortedBy { it.minutesSinceMidnight }
        if (stored.isNotEmpty()) stored
        else if (prefs[Keys.ALARM_ENABLED] == true) listOf(DailyAlarm(prefs[Keys.ALARM_HOUR] ?: 6, prefs[Keys.ALARM_MINUTE] ?: 0))
        else emptyList()
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME_ENABLED] = enabled }
    suspend fun setOnboardingComplete(complete: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    suspend fun setAiFeaturesEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.AI_FEATURES_ENABLED] = enabled }

    suspend fun setAlarm(enabled: Boolean, hour: Int, minute: Int) = setAlarmTimes(
        if (enabled) listOf(DailyAlarm(hour.coerceIn(0, 23), minute.coerceIn(0, 59))) else emptyList()
    )

    suspend fun setAlarmTimes(times: List<DailyAlarm>) {
        val normalized = times
            .map { DailyAlarm(it.hour.coerceIn(0, 23), it.minute.coerceIn(0, 59)) }
            .distinctBy { it.minutesSinceMidnight }
            .sortedBy { it.minutesSinceMidnight }
        context.dataStore.edit { prefs ->
            if (normalized.isEmpty()) {
                prefs[Keys.ALARM_ENABLED] = false
                prefs.remove(Keys.ALARM_TIMES)
            } else {
                prefs[Keys.ALARM_ENABLED] = true
                prefs[Keys.ALARM_HOUR] = normalized.first().hour
                prefs[Keys.ALARM_MINUTE] = normalized.first().minute
                prefs[Keys.ALARM_TIMES] = normalized.map { "%02d:%02d".format(it.hour, it.minute) }.toSet()
            }
        }
    }

    /** Enables biometric-only App Lock. Caller must have already verified a successful BiometricPrompt auth before calling this. */
    suspend fun enableBiometricLock() = context.dataStore.edit {
        it[Keys.APP_LOCK_TYPE] = AppLockType.BIOMETRIC.name
        it.remove(Keys.PIN_SALT); it.remove(Keys.PIN_HASH)
    }

    /** Enables PIN App Lock with a mandatory recovery question, so a forgotten PIN doesn't lock the user out permanently. */
    suspend fun enablePinLock(pin: String, recoveryQuestion: String, recoveryAnswer: String) {
        val pinHash = PinHasher.hash(pin)
        val answerHash = PinHasher.hash(recoveryAnswer.trim().lowercase())
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = AppLockType.PIN.name
            it[Keys.PIN_SALT] = pinHash.saltBase64
            it[Keys.PIN_HASH] = pinHash.hashBase64
            it[Keys.RECOVERY_QUESTION] = recoveryQuestion
            it[Keys.RECOVERY_ANSWER_SALT] = answerHash.saltBase64
            it[Keys.RECOVERY_ANSWER_HASH] = answerHash.hashBase64
        }
    }

    /** Disables App Lock entirely, wiping any stored PIN/recovery data. */
    suspend fun disableAppLock() = context.dataStore.edit {
        it[Keys.APP_LOCK_TYPE] = AppLockType.NONE.name
        it.remove(Keys.PIN_SALT); it.remove(Keys.PIN_HASH)
        it.remove(Keys.RECOVERY_QUESTION); it.remove(Keys.RECOVERY_ANSWER_SALT); it.remove(Keys.RECOVERY_ANSWER_HASH)
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val hash = prefs[Keys.PIN_HASH] ?: return false
        return PinHasher.verify(enteredPin, salt, hash)
    }

    suspend fun verifyRecoveryAnswer(enteredAnswer: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.RECOVERY_ANSWER_SALT] ?: return false
        val hash = prefs[Keys.RECOVERY_ANSWER_HASH] ?: return false
        return PinHasher.verify(enteredAnswer.trim().lowercase(), salt, hash)
    }
}
