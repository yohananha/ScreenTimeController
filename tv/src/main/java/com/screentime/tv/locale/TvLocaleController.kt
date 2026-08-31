package com.screentime.tv.locale

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import com.screentime.shared.locale.LanguageProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the family's language (from [LanguageProvider]) to the TV process.
 * There's no Activity-based locale picker on the TV — the block overlay is a
 * WindowManager-hosted view (see BlockOverlayController), and
 * AppCompatDelegate has no effect there — so this controller is what makes
 * the locale actually take effect, on three surfaces:
 *
 *  1. [wrap] — a locale-applied Context for the overlay's ComposeView and for
 *     TV MainActivity's shadowed LocalContext.
 *  2. [Locale.setDefault] — covers non-Compose formatting (e.g. the
 *     pre-rendered "next window opens at" string built in
 *     EnforcementAccessibilityService before it ever reaches Compose).
 *  3. A SharedPreferences seed, read synchronously in the constructor — the
 *     AccessibilityService can call `overlay.show()` within milliseconds of
 *     connecting, before Firestore's first snapshot lands; without a
 *     synchronous seed the child would see an English block screen that
 *     flips to Hebrew a moment later.
 */
@Singleton
class TvLocaleController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    languageProvider: LanguageProvider,
) {
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _locale = MutableStateFlow(prefs.getString(KEY_LANGUAGE_TAG, null)?.let(Locale::forLanguageTag))
    val locale: StateFlow<Locale?> = _locale.asStateFlow()

    init {
        _locale.value?.let { Locale.setDefault(it) }
        scope.launch {
            languageProvider.languageTag.collect { tag ->
                val next = tag?.let(Locale::forLanguageTag)
                if (next == _locale.value) return@collect
                prefs.edit().putString(KEY_LANGUAGE_TAG, tag).apply()
                next?.let { Locale.setDefault(it) }
                _locale.value = next
            }
        }
    }

    /** A locale-applied Context whose Resources resolve strings in the family's language. */
    fun wrap(base: Context): Context {
        val l = _locale.value ?: return base
        val cfg = Configuration(base.resources.configuration).apply {
            setLocale(l)
            setLayoutDirection(l)
        }
        return base.createConfigurationContext(cfg)
    }

    fun layoutDirection(): LayoutDirection {
        val effective = _locale.value ?: Locale.getDefault()
        return if (TextUtils.getLayoutDirectionFromLocale(effective) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }

    private companion object {
        const val PREFS_NAME = "tv_locale"
        const val KEY_LANGUAGE_TAG = "language_tag"
    }
}
