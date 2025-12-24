package com.abhishek.adblocker.domain.model

data class ObservedDomain(
    val hostname: String,
    val isBlocked: Boolean,
    val isUserBlocked: Boolean,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
