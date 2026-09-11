package com.lifeos.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

/**
 * App Lock biometric authentication.
 *
 * The biometric lock is now bound to an Android Keystore AES key configured
 * for per-use user authentication and invalidated when the enrolled
 * biometrics change. The key material never leaves Android Keystore and is
 * never exposed to LifeOS.
 *
 * A CryptoObject is used with BIOMETRIC_STRONG. Device credential fallback is
 * intentionally not combined with this cryptographic binding because the
 * Android biometric API does not provide the same key-enrollment semantics
 * for that fallback path.
 */
class AppLockManager(private val context: Context) {

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lifeos_biometric_lock_v2"
    }

    fun isBiometricAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (!isBiometricAvailable()) {
            onError("A strong biometric (fingerprint or face) must be enrolled to use biometric App Lock.")
            return
        }

        val cipher = runCatching { createAuthenticatedCipher() }.getOrElse {
            onError("LifeOS could not prepare secure biometric storage: ${it.message ?: "unknown error"}")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // Touch the CryptoObject so a successful prompt is also a
                // successful use of the Keystore-bound credential.
                runCatching {
                    result.cryptoObject?.cipher?.update(byteArrayOf(0))
                }.onFailure {
                    onError("Secure biometric verification failed. Please try again.")
                    return
                }
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
            .setSubtitle("Secure biometric verification")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun createAuthenticatedCipher(): Cipher {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

        fun newCipher(key: java.security.Key): Cipher =
            Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, key)
            }

        val existingKey = if (keyStore.containsAlias(KEY_ALIAS)) {
            runCatching { keyStore.getKey(KEY_ALIAS, null) }.getOrNull()
        } else null

        existingKey?.let { key ->
            runCatching { newCipher(key) }.getOrNull()?.let { return it }
        }

        // A biometric enrollment change can invalidate the old key. Replace
        // only that local Keystore entry; no user data is touched.
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
        return newCipher(generateKey())
    }

    private fun generateKey(): java.security.Key {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()

        generator.init(spec)
        return generator.generateKey()
    }
}
