package com.screentime.mobile.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Fans a language choice out to three places (see the Hebrew-localization
 * plan's locale-plumbing stage for the full rationale):
 *  - [AppCompatDelegate.setApplicationLocales] — applied to this device
 *    immediately; this is what the user actually sees change.
 *  - users/{uid}.language — the person's own preference, portable across
 *    every device they sign into.
 *  - families/{id}/settings/language — what a paired TV shows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val authRepository: AuthRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    init {
        // Applies the synced profile value whenever it differs from what's
        // already active on this device — covers first sign-in on a new
        // device, a reinstall, and a live change made on the person's other
        // phone.
        //
        // Loop-guard rules (the whole correctness argument for this
        // collector): it must ONLY call setApplicationLocales, never write
        // back to Firestore — writes happen exclusively in select(). And it
        // must compare before applying, because setApplicationLocales
        // triggers an Activity recreate on API < 33, which would otherwise
        // re-enter this same collector on the new instance and could cycle.
        viewModelScope.launch {
            authRepository.currentSession.flatMapLatest { session ->
                if (session == null) flowOf(null) else firestore.userLanguageFlow(session.uid)
            }.collectLatest { tag ->
                if (tag != currentAppLocaleTag()) {
                    AppCompatDelegate.setApplicationLocales(tag.toLocaleList())
                }
            }
        }
    }

    /** [tag] is a BCP-47 language tag ("en", "he"), or null for "use device language". */
    fun select(tag: String?) {
        AppCompatDelegate.setApplicationLocales(tag.toLocaleList())
        viewModelScope.launch {
            val uid = authRepository.currentSession.first()?.uid ?: return@launch
            runCatching { firestore.setUserLanguage(uid, tag) }

            val familyId = familyIdProvider.familyId.value ?: return@launch
            // The TV can't honour "the *phone's* device language" — resolve
            // "use device language" to a concrete tag before pushing it to
            // the family doc (the picker's helper text says as much).
            val effective = tag ?: Locale.getDefault().toLanguageTag().substringBefore('-')
            runCatching { firestore.setLanguage(familyId, effective) }
        }
    }

    private fun String?.toLocaleList(): LocaleListCompat =
        if (this == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(this)

    private fun currentAppLocaleTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales[0]?.toLanguageTag()
    }
}
