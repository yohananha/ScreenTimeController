package com.screentime.mobile.ui.requests

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits once immediately, then once per [periodMillis] — used to re-evaluate time-based state (e.g. grant expiry). */
fun tickerFlow(periodMillis: Long = 1_000L): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMillis)
    }
}
