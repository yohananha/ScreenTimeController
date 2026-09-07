package com.screentime.mobile.whatsnew

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: WhatsNewPreferences,
) : ViewModel() {

    private val installedVersionName: String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    private val _entryToShow = MutableStateFlow(computeEntryToShow())
    val entryToShow: StateFlow<WhatsNewEntry?> = _entryToShow.asStateFlow()

    private fun computeEntryToShow(): WhatsNewEntry? {
        if (prefs.lastSeenVersion == installedVersionName) return null
        return WhatsNewRegistry.forVersion(installedVersionName)
    }

    /** Called once the dialog has been shown to the user, whether or not an entry existed. */
    fun dismiss() {
        prefs.lastSeenVersion = installedVersionName
        _entryToShow.value = null
    }
}
