package com.example.safenote.viewmodels

import androidx.lifecycle.ViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.safenote.utils.KeyStoreManager
import java.nio.charset.StandardCharsets
import java.security.KeyStore

class SetPasswordViewModel : ViewModel() {

    private var isPasswordStrong = false

    private val uppercaseLetters : Regex = Regex(".*[A-Z].*")
    private val lowercaseLetters : Regex = Regex(".*[a-z].*")
    private val numbers : Regex = Regex(".*\\d.*")
    private val specialCharacters : Regex = Regex(".*[!\"#\$%&'()*+,-./:;<=>?@^_`{|}~].*")

    fun getPasswordStrengthVerificationFlags(password : String) : Array<Boolean> {

        val verificationFlags = arrayOf(false, false, false, false)

        if(password.length >= 12) {
            verificationFlags[0] = true
        }

        if(password.matches(uppercaseLetters) && password.matches(lowercaseLetters)) {
            verificationFlags[1] = true
        }

        if(password.matches(numbers) && (password.matches(uppercaseLetters) || password.matches(lowercaseLetters))) {
            verificationFlags[2] = true
        }

        if(password.matches(specialCharacters)) {
            verificationFlags[3] = true
        }

        if(!verificationFlags.contains(false))
            isPasswordStrong = true

        return verificationFlags
    }

    fun isPasswordStrong() : Boolean {
        return isPasswordStrong
    }

    fun setPassword(password : String) {
        KeyStoreManager.savePassword(password)
    }
}