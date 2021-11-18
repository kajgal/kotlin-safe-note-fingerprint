package com.example.safenote.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import at.favre.lib.crypto.bcrypt.BCrypt
import java.lang.Exception
import java.security.Key
import kotlin.properties.Delegates

object KeyStoreManager {

    const val captchaSiteKey = "6LcEVDsdAAAAAJbRg7iUKgPwMdxYZt-pCWqjH4wD"
    const val TIMEOUT_AFTER = 60000L
    const val DELETE_AFTER = 5
    const val DEVICE_ACCESS_CODE = 101

    private const val MASTER_KEY = "\$2a\$08\$AlKj64lkJDMGW"
    private const val ESP_SHARED = "\$2a\$08\$d1ZYLJwvyKu3qSQUbyU99uuh4J2jGi5sDB6q5d3NY.X1IMTe3msi."
    private const val ESP_PASSWORD = "\$2a\$08\$78LX1DgBmK7JZYhO.1byKOCRiFyk1.CT4Jwk16x1leh0uFjpLVC9e"
    private const val ESP_NOTE = "SAFE_NOTE_ESP_NOTE"
    private const val ESP_FAILED = "SAFE_NOTE_ESP_FAILED"

    private var isStrongBox by Delegates.notNull<Boolean>()
    private lateinit var advKeySpec : KeyGenParameterSpec
    private lateinit var advKeyAlias : String

    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var encryptedSharedPreferences: EncryptedSharedPreferences

    private var requireAuth = true
    private var exitIfBackgrounded = true
    private var exitOnResume = false

    @RequiresApi(Build.VERSION_CODES.M)
    fun init(context : Context, isStrongBoxSupported : Boolean) {
        sharedPreferences = context.getSharedPreferences(ESP_SHARED, Context.MODE_PRIVATE)
        isStrongBox = isStrongBoxSupported
    }

    fun allowAccess(context : Context) {
        advKeySpec = KeyGenParameterSpec.Builder(
                MASTER_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(requireAuth)
            setUserAuthenticationValidityDurationSeconds(60)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if(isStrongBox)
                    setIsStrongBoxBacked(true)
                setUnlockedDeviceRequired(true)
            }

        }.build()

        advKeyAlias = MasterKeys.getOrCreate(advKeySpec)

        encryptedSharedPreferences = EncryptedSharedPreferences.create(
                ESP_SHARED,
                advKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    fun getPasswordHash() : String? {
        return sharedPreferences.getString(ESP_PASSWORD, "")
    }

    fun savePassword(password : String) {
        sharedPreferences.edit().putString(ESP_PASSWORD, BCrypt.withDefaults().hashToString(14, password.toCharArray())).apply()
        requireAuth = false
    }

    fun saveNote(note : String) {
        encryptedSharedPreferences.edit().putString(ESP_NOTE, note).apply()
    }

    fun getNote() : String {
        return encryptedSharedPreferences.getString(ESP_NOTE, "").toString()
    }

    fun getExitIfBackgrounded() : Boolean {
        return exitIfBackgrounded
    }

    fun setExitIfBackgrounded(value : Boolean) {
        exitIfBackgrounded = value
    }

    fun getExitOnResume() : Boolean {
        return exitOnResume
    }

    fun setExitOnResume(value : Boolean) {
        exitOnResume = value
    }
}