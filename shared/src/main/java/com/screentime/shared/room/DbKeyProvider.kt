package com.screentime.shared.room

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and stores the SQLCipher passphrase used to encrypt the local Room database.
 *
 * The passphrase itself is 32 random bytes generated on first run. It is wrapped with an
 * AES-256/GCM key resident in the Android Keystore (not exportable) and the resulting
 * ciphertext is persisted to a private SharedPreferences file. On every subsequent open
 * the wrapped blob is decrypted in-process by the Keystore.
 *
 * Threat model: an attacker with raw disk access (rooted device, locked-down extraction)
 * cannot decrypt the database without invoking the Keystore key, which is bound to this
 * app's UID. The passphrase never leaves the device.
 */
internal object DbKeyProvider {

    private const val PREFS_NAME = "screen_time_db_key"
    private const val PREF_WRAPPED = "wrapped_passphrase_v1"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "screen_time_db_key_v1"
    private const val PASSPHRASE_LENGTH_BYTES = 32
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

    /** Returns the raw passphrase bytes — generating and persisting them on first call. */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(PREF_WRAPPED, null)
        if (existing != null) {
            runCatching { return unwrap(existing) }
                // Keystore key was cleared (e.g. all biometrics removed on some OEMs).
                // Fall through and regenerate; the database will be recreated from
                // Firestore on next sync.
                .onFailure { prefs.edit().remove(PREF_WRAPPED).apply() }
        }

        val fresh = ByteArray(PASSPHRASE_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(PREF_WRAPPED, wrap(fresh)).apply()
        return fresh
    }

    private fun wrap(plaintext: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        val out = ByteArray(iv.size + ciphertext.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(ciphertext, 0, this, iv.size, ciphertext.size)
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrap(encoded: String): ByteArray {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            loadKey() ?: error("Keystore alias missing"),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey = loadKey() ?: generateKey()

    private fun loadKey(): SecretKey? {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val entry = ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun generateKey(): SecretKey {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return gen.generateKey()
    }
}
