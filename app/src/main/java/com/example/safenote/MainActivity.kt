package com.example.safenote

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.findNavController
import com.example.safenote.utils.KeyStoreManager

class MainActivity : AppCompatActivity(), LifecycleObserver {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // maintaining lifecycle of activity
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
        // cryptographic and storage
        KeyStoreManager.init(applicationContext, packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE))
        setContentView(R.layout.activity_main)
    }

    // app is not allowed to work in background, immediately closing when minimized
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // working in background is only allowed when currently displayed fragment is VerificationFragment in order to verify access to device
        if(KeyStoreManager.getExitIfBackgrounded()) {
            finishAndRemoveTask()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if(KeyStoreManager.getExitOnResume()) {
            val intent = intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
        }
    }

}