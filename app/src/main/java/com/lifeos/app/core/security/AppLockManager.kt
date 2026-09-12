package com.lifeos.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin adapter around Android's real biometric/device credential UI.
 * LifeOS never reads or stores biometric data; Android owns enrollment and
 * verification. Device credential is allowed as a platform fallback on
 * devices where a strong biometric is unavailable.
 */
class AppLockManager(private val context: Context) {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(): Int = BiometricManager.from(context).canAuthenticate(authenticators)

    fun isBiometricAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        when (canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("Set up a fingerprint/face or device screen lock in Android Settings first.")
                return
            }
            else -> {
                onError("This device does not provide a compatible secure unlock method.")
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LifeOS")
            .setSubtitle("Use your fingerprint, face, or device screen lock")
            .setDescription("Android verifies the credential. LifeOS only receives the verification result.")
            .setAllowedAuthenticators(authenticators)
            .build()

        runCatching { prompt.authenticate(promptInfo) }
            .onFailure { onError("Secure unlock could not start. Please try again.") }
    }
}
