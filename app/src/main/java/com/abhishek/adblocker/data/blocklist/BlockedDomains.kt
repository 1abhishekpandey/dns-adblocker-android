package com.abhishek.adblocker.data.blocklist

object BlockedDomains {
    private val defaultDomains = setOf(
        // Google Ads
        "pagead2.googlesyndication.com",
        "googleads.g.doubleclick.net",
        "ad.doubleclick.net",
        "pubads.g.doubleclick.net",
        "googleadservices.com",
        "tpc.googlesyndication.com",
        "partner.googleadservices.com",
        "adservices.google.com",
        "googlesyndication.com",
        "doubleclick.net",

        // Hotstar Ads
        "hesads.akamaized.net",

        // Ad Verification & Tracking
        "doubleverify.com",
        "appsflyersdk.com",
        "appsflyer.com",
        "clevertap-prod.com",
        "clevertap.com",

        // Video Ad SDKs
        "imasdk.googleapis.com",

        // Common Ad Networks
        "ads.yahoo.com",
        "advertising.com",
        "adnxs.com",
        "adsrvr.org",
        "criteo.com",
        "criteo.net",
        "moatads.com",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com"
    )

    private var userDomains: Set<String> = emptySet()
    private var userUnblockedDefaultDomains: Set<String> = emptySet()

    fun updateUserDomains(domains: Set<String>) {
        userDomains = domains
    }

    fun updateUserUnblockedDefaultDomains(domains: Set<String>) {
        userUnblockedDefaultDomains = domains
    }

    fun isBlocked(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        val isInDefault = isBlockedBySet(normalized, defaultDomains)
        val isUnblocked = isBlockedBySet(normalized, userUnblockedDefaultDomains)
        val isInUser = isBlockedBySet(normalized, userDomains)

        return (isInDefault && !isUnblocked) || isInUser
    }

    fun isBlockedByDefault(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        return isBlockedBySet(normalized, defaultDomains)
    }

    fun isBlockedByUser(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        return isBlockedBySet(normalized, userDomains)
    }

    fun isUnblockedByUser(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        return isBlockedBySet(normalized, userUnblockedDefaultDomains)
    }

    private fun isBlockedBySet(normalized: String, domains: Set<String>): Boolean {
        return domains.any { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }

    fun getDefaultDomains(): Set<String> = defaultDomains

    fun getUserDomains(): Set<String> = userDomains

    fun getBlockedDomains(): Set<String> = (defaultDomains - userUnblockedDefaultDomains) + userDomains
}
