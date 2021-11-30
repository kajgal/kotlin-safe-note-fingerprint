package com.example.safenote.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {

    private const val ESP_NOTE = "\$2a\$12\$KHFOM6eIhOD8imiY2qkqCu3/WDn9Fg60WOOZVyIbQKpH1REXfvORO"
    private const val ESP_SHARED = "\$2a\$12\$Kq3Z84YER1nIGC7RjEtna.69JGfdwKxR2syGPSaECGTFgcs5zxe6G"

    private lateinit var sharedPreferences : SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(ESP_SHARED, Context.MODE_PRIVATE)
    }

    // saving already encrypted and encoded content
    fun saveNote(note : String) {
        sharedPreferences.edit().putString(ESP_NOTE, note).apply()
    }

    // getting encrypted and encoded content
    fun getNote() : String {
        return sharedPreferences.getString(ESP_NOTE, "")!!
    }

    fun removeNote() {
        sharedPreferences.edit().remove(ESP_NOTE).apply()
    }
}