package com.offipe.app.data

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

/** Creates and retrieves the SQLCipher key protected by the Android Keystore. */
class DbKeyProvider(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreateKey(): ByteArray {
        val hasCiphertext = preferences.contains(CIPHERTEXT_KEY)
        val hasIv = preferences.contains(IV_KEY)
        if (hasCiphertext || hasIv) {
            check(hasCiphertext && hasIv) { "Database key storage is incomplete" }
            return decryptKey(
                ciphertext = Base64.decode(requireNotNull(preferences.getString(CIPHERTEXT_KEY, null)), Base64.NO_WRAP),
                iv = Base64.decode(requireNotNull(preferences.getString(IV_KEY, null)), Base64.NO_WRAP)
            )
        }

        val databaseKey = ByteArray(KEY_LENGTH_BYTES)
        SecureRandom().nextBytes(databaseKey)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
            val ciphertext = cipher.doFinal(databaseKey)
            val persisted = preferences.edit()
                .putString(CIPHERTEXT_KEY, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
            check(persisted) { "Unable to persist database key" }
            return databaseKey
        } catch (exception: Exception) {
            databaseKey.fill(0)
            throw IllegalStateException("Unable to create database key", exception)
        }
    }

    private fun decryptKey(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        check(iv.size == GCM_IV_LENGTH_BYTES) { "Invalid database key IV" }
        val wrappingKey = loadWrappingKey()
            ?: throw IllegalStateException("Database wrapping key is unavailable")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        loadWrappingKey()?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            WRAPPING_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return generator.apply { init(spec) }.generateKey()
    }

    private fun loadWrappingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(WRAPPING_KEY_ALIAS, null) as? SecretKey
    }

    private companion object {
        const val PREFERENCES_NAME = "offipe_database_key"
        const val CIPHERTEXT_KEY = "wrapped_key"
        const val IV_KEY = "wrapped_key_iv"
        const val WRAPPING_KEY_ALIAS = "offipe_db_wrap"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_LENGTH_BYTES = 32
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}