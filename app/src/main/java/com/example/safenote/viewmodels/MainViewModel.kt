package com.example.safenote.viewmodels

import androidx.lifecycle.ViewModel
import com.example.safenote.utils.KeyStoreManager

class MainViewModel : ViewModel() {

    fun isFirstTimeUsage() : Boolean {
        if (KeyStoreManager.getPasswordHash() == "")
            return true
        return false
    }
}