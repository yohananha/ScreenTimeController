package com.screentime.mobile.ui.theme

import androidx.compose.ui.graphics.Color

private val appAccents = listOf(
    Color(0xFFE5483A), Color(0xFF5B6B7B), Color(0xFF2A2730), Color(0xFF4FA98C),
    Color(0xFF8E86D9), Color(0xFFF2A93B), Color(0xFFB9A8F0),
)

fun appAccentFor(packageName: String): Color =
    appAccents[(packageName.hashCode().let { if (it < 0) -it else it }) % appAccents.size]
