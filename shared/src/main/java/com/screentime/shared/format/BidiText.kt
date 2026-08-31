package com.screentime.shared.format

import android.text.BidiFormatter

/**
 * Wraps a piece of formatted text (a duration, a clock time, a countdown, a
 * six-digit code, an app label…) in Unicode bidi isolation marks so it reads
 * correctly when it ends up embedded inside a right-to-left sentence (e.g.
 * Hebrew). Apply this **inside** a formatter, never at the call site that
 * concatenates strings together — that way it can't be forgotten.
 *
 * `android.text.BidiFormatter.unicodeWrap` emits FSI/PDI isolates around the
 * text; it's a light wrapper over ICU and has been available since API 23,
 * well below this project's minSdk 26. It's always safe to call regardless
 * of locale — it only inserts marks when the text's own direction disagrees
 * with the surrounding context.
 */
private val bidi: BidiFormatter = BidiFormatter.getInstance()

fun String.bidiWrap(): String = bidi.unicodeWrap(this)
