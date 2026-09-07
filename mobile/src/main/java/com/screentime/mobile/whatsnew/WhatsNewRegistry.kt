package com.screentime.mobile.whatsnew

import androidx.annotation.StringRes
import com.screentime.mobile.R

/** One release's short "what's new" note, shown once after that version installs. */
data class WhatsNewEntry(
    val versionName: String,
    @StringRes val titleRes: Int,
    @StringRes val bulletRes: List<Int>,
)

/**
 * To ship a "what's new" note with a release: add a [WhatsNewEntry] here
 * (with its string resources, including values-iw), keyed to the exact
 * `versionName` in mobile/build.gradle.kts for that release. Nothing else
 * needs to change — [WhatsNewViewModel] looks up the entry matching the
 * installed version automatically the first time the app opens on it.
 *
 * Only the entry matching the currently installed version is ever shown
 * (not a backlog of every version skipped over), so an entry for a version
 * nobody installs is simply never seen.
 */
object WhatsNewRegistry {
    val entries: List<WhatsNewEntry> = listOf(
        WhatsNewEntry(
            versionName = "0.1.13",
            titleRes = R.string.whats_new_v0_1_13_title,
            bulletRes = listOf(
                R.string.whats_new_v0_1_13_bullet_1,
                R.string.whats_new_v0_1_13_bullet_2,
            ),
        ),
    )

    fun forVersion(versionName: String): WhatsNewEntry? = entries.find { it.versionName == versionName }
}
