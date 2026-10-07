package com.screentime.mobile

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.firestore.ktx.memoryCacheSettings
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The parent app is always online, and Firestore's default disk cache
        // would keep the family's data (the child's usage, requests, rules)
        // on this phone after sign-out or account deletion. Must run before
        // anything touches Firestore — Hilt creates it lazily, on first use.
        // Settings can only be applied before first use; if something ever
        // gets there first, keep running with the default cache rather than
        // crash on every launch.
        runCatching {
            Firebase.firestore.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings {})
            }
        }.onFailure { Log.w("MobileApp", "Could not switch Firestore to memory cache", it) }
    }
}
