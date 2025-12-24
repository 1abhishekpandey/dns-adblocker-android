package com.abhishek.adblocker.util

import android.util.Log

object Logger {
    private const val TAG = "AdBlockerApp"

    enum class LogLevel(val priority: Int) {
        VERBOSE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        NONE(5)
    }

    var currentLevel: LogLevel = LogLevel.VERBOSE

    fun v(message: String, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.VERBOSE.priority) {
            Log.v(tag, message)
        }
    }

    fun d(message: String, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.DEBUG.priority) {
            Log.d(tag, message)
        }
    }

    fun i(message: String, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.INFO.priority) {
            Log.i(tag, message)
        }
    }

    fun w(message: String, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.WARN.priority) {
            Log.w(tag, message)
        }
    }

    fun e(message: String, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.ERROR.priority) {
            Log.e(tag, message)
        }
    }

    fun e(message: String, throwable: Throwable, tag: String = TAG) {
        if (currentLevel.priority <= LogLevel.ERROR.priority) {
            Log.e(tag, message, throwable)
        }
    }

    fun domainBlocked(hostname: String) {
        i("🚫 BLOCKED: $hostname")
    }

    fun domainAllowed(hostname: String) {
        v("✅ ALLOWED: $hostname")
    }

    fun vpnStarted() {
        i("🔒 VPN Service Started")
    }

    fun vpnStopped() {
        i("🔓 VPN Service Stopped")
    }

    fun packetProcessed(packetType: String, size: Int) {
        v("📦 Packet: type=$packetType, size=$size bytes")
    }

    fun dnsQueryReceived(hostname: String, type: Int) {
        v("🔍 DNS Query: $hostname (type=$type)")
    }

    fun dnsForwarded(hostname: String, upstreamDns: String) {
        v("➡️ Forwarded to $upstreamDns: $hostname")
    }

    fun vpnInterfaceEstablished(address: String) {
        i("🌐 VPN Interface: $address")
    }

    fun packetProcessingStarted() {
        i("▶️ Packet processing started")
    }

    fun packetProcessingStopped() {
        i("⏹️ Packet processing stopped")
    }
}
