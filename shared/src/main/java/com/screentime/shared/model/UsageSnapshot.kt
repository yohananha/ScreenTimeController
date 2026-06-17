package com.screentime.shared.model

data class UsageSnapshot(
    val date: String,
    val perAppMillis: Map<String, Long>,
) {
    fun millisFor(packageName: String): Long = perAppMillis[packageName] ?: 0L

    fun totalMillis(): Long = perAppMillis.values.sum()
}
