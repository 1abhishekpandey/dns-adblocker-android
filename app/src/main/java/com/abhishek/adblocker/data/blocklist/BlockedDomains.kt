package com.abhishek.adblocker.data.blocklist

object BlockedDomains {
    private val domains = setOf(
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

    fun isBlocked(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        return domains.any { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }

    fun getBlockedDomains(): Set<String> = domains
}
