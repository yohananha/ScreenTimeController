package com.screentime.shared.model

data class PairedDevice(
    val id: String,
    val name: String,
) {
    companion object {
        const val DEFAULT_NAME = "Android TV"
    }
}
