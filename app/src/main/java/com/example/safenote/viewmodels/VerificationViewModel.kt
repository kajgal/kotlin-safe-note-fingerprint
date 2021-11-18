package com.example.safenote.viewmodels

import androidx.lifecycle.ViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.safenote.utils.KeyStoreManager
import kotlinx.coroutines.Job

class VerificationViewModel : ViewModel() {

    private var isDeviceNotRooted = false;
    private var isDeviceAccessVerified = false;
    private var isCaptchaVerified = false;
    private var isUserPasswordVerified = false;

    fun isUserPasswordCorrect(password : String) : Boolean {
        val passwordHash = KeyStoreManager.getPasswordHash().toString()
        return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified
    }

    fun setIsDeviceNotRooted() {
        isDeviceNotRooted = true
    }

    fun setIsCaptchaVerified() {
        isCaptchaVerified = true
    }

    fun setIsDeviceAccessVerified() {
        isDeviceAccessVerified = true
    }

    fun setIsUserPasswordVerified() {
        isUserPasswordVerified = true
    }

    fun isDeviceAccessVerified() : Boolean {
        return isDeviceAccessVerified
    }

    fun isCaptchaVerified() : Boolean {
        return isCaptchaVerified
    }

    fun isUserPasswordVerified() : Boolean {
        return isUserPasswordVerified
    }

    fun isSuccessfullyVerified() : Boolean {
        return (isDeviceNotRooted and isDeviceAccessVerified and isCaptchaVerified and isUserPasswordVerified)
    }
}