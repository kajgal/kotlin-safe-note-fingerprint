package com.example.safenote.viewmodels

import androidx.lifecycle.ViewModel
import com.example.safenote.utils.CryptographyManager
import com.example.safenote.utils.SharedPreferencesManager
import javax.crypto.Cipher

class VerificationViewModel : ViewModel() {

    val captchaSiteKey = "6LcEVDsdAAAAAJbRg7iUKgPwMdxYZt-pCWqjH4wD"
    val TIMEOUT_AFTER = 60000L

    private var isDeviceNotRooted = false
    private var isCaptchaVerified = false

    private var noteContent = ""

    fun resetVerification() {
        isDeviceNotRooted = false
        isCaptchaVerified = false
    }

    fun setIsDeviceNotRooted() {
        isDeviceNotRooted = true
    }

    fun isDeviceNotRooted() : Boolean {
        return isDeviceNotRooted
    }

    fun setIsCaptchaVerified() {
        isCaptchaVerified = true
    }

    fun isCaptchaVerified() : Boolean {
        return isCaptchaVerified
    }

    fun setNoteContent(newContent : String) {
        noteContent = newContent
    }

    fun getNoteContent() : String {
        return noteContent
    }

    fun onAuthSuccess(cipher: Cipher) {
        val decryptedNote = CryptographyManager.decryptData(cipher)
        setNoteContent(decryptedNote)
    }

    fun saveNoteContent(note : String, cipher : Cipher) {
        val encryptedNote = CryptographyManager.encryptData(note, cipher)
        SharedPreferencesManager.saveNote(encryptedNote)
    }

    fun isFirstTimeUsage(): Boolean {
        if(SharedPreferencesManager.getNote() == "")
            return true

        return false
    }
}