package com.example.safenote

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.safenote.utils.CryptographyManager
import com.example.safenote.utils.SharedPreferencesManager


class MainActivity : AppCompatActivity(), LifecycleObserver {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // maintaining lifecycle of activity
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
        // init storage
        SharedPreferencesManager.init(applicationContext)
        // setup StrongBox if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            CryptographyManager.isStrongBoxSupport(packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE))
        }
        setContentView(R.layout.activity_main)
    }

    // app is not allowed to work in background, immediately closing when minimized
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        //finishAndRemoveTask()
    }
}