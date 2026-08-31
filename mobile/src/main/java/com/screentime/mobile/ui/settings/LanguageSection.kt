package com.screentime.mobile.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.Sprout

@Composable
fun LanguageSection(viewModel: LanguageViewModel = hiltViewModel()) {
    // null = "Use device language". Seeded from whatever AppCompat already
    // has applied — on a cold start that's either the locally-cached choice
    // (autoStoreLocales) or empty (device default). LanguageViewModel's own
    // init{} block reconciles this against the synced profile value shortly
    // after, which may trigger an Activity recreate on API < 33 and remount
    // this composable with the settled value.
    var selected by remember { mutableStateOf(currentAppLocaleTag()) }

    fun pick(tag: String?) {
        selected = tag
        viewModel.select(tag)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(vertical = 6.dp),
    ) {
        Text(
            stringResource(R.string.settings_language_section_title),
            style = Sprout.typography.headline,
            color = Sprout.colors.ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        LanguageRow(
            label = stringResource(R.string.settings_language_system),
            selected = selected == null,
            onClick = { pick(null) },
        )
        LanguageRow(
            label = stringResource(R.string.settings_language_en),
            selected = selected == "en",
            onClick = { pick("en") },
        )
        LanguageRow(
            label = stringResource(R.string.settings_language_he),
            selected = selected == "he",
            onClick = { pick("he") },
        )
        Text(
            stringResource(R.string.settings_language_hint),
            style = Sprout.typography.caption,
            color = Sprout.colors.inkMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun currentAppLocaleTag(): String? {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) null else locales[0]?.toLanguageTag()
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
        RadioButton(selected = selected, onClick = onClick)
    }
}
