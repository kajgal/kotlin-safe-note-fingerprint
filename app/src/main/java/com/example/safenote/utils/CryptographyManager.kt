package com.example.safenote.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import java.security.KeyFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import kotlin.properties.Delegates

object CryptographyManager {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val ENCRYPTION_KEY_SIZE = 256
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val SECRET_KEY_ALIAS = "a\\\$2\$12\$wYn.4ca5zaexdj2cjz2jkOy3A16KWHKO5wIYxkrmFEqmVVO0fRgMu"

    private var isStrongBoxSupported by Delegates.notNull<Boolean>()

    fun isStrongBoxSupport(isSupported : Boolean) {
        isStrongBoxSupported = isSupported
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getOrCreateSecretKey() : SecretKey {

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
        keyStore.load(null)
        val key = keyStore.getKey(SECRET_KEY_ALIAS, null)

        if(key != null) {
            return key as SecretKey
        }

        val advKeySpec = KeyGenParameterSpec.Builder(
                SECRET_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setKeySize(ENCRYPTION_KEY_SIZE)
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
            setUserAuthenticationValidityDurationSeconds(-1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if(isStrongBoxSupported)
                    setIsStrongBoxBacked(true)

                setUnlockedDeviceRequired(true)
            }
        }.build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
        keyGenerator.init(advKeySpec)

        return keyGenerator.generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun initCipherForEncryption() : Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    fun encryptData(plainText : String, cipher: Cipher) : String {
        val cipherText = cipher.doFinal(plainText.toByteArray())
        val finalResult = cipher.iv + cipherText

        return Base64.encodeToString(finalResult, Base64.DEFAULT)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun initCipherForDecryption() : Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, getIV()))

        return cipher
    }

    fun decryptData(cipher: Cipher) : String {
        val cipherText = SharedPreferencesManager.getNote()
        val decodedText = Base64.decode(cipherText, Base64.NO_WRAP)
        val noteEncryptedBytes = decodedText.copyOfRange(12, decodedText.size)

        val decryptedText = cipher.doFinal(noteEncryptedBytes)

        return String(decryptedText)
    }

    private fun getCipher() : Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    private fun getIV() : ByteArray {
        val storedData = SharedPreferencesManager.getNote()
        val decodedData = Base64.decode(storedData, Base64.NO_WRAP)
        val ivBytes = decodedData.copyOfRange(0, 12)
        return ivBytes
    }

}