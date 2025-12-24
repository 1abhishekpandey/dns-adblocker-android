package com.abhishek.adblocker.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isSelected: Boolean = false
) {
    companion object {
        fun comparator(): Comparator<AppInfo> = compareBy<AppInfo> { !it.isSelected }
            .thenBy { it.isSystemApp }
            .thenBy { it.appName.lowercase() }
    }
}
