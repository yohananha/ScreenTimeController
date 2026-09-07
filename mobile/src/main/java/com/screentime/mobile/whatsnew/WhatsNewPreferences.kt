package com.screentime.mobile.whatsnew

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Tracks the last app version the parent has already seen a "what's new" note for. */
@Singleton
class WhatsNewPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastSeenVersion: String?
        get() = prefs.getString(PREF_LAST_SEEN_VERSION, null)
        set(value) = prefs.edit().putString(PREF_LAST_SEEN_VERSION, value).apply()

    private companion object {
        const val PREFS_NAME = "whats_new"
        const val PREF_LAST_SEEN_VERSION = "last_seen_version"
    }
}
