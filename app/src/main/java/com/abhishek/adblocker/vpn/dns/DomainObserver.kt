package com.abhishek.adblocker.vpn.dns

import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.domain.model.ObservedDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds observed domains state, bridging VPN service to UI layer.
 *
 * Uses StateFlow to maintain accumulated domain state that survives UI lifecycle.
 * When the UI is backgrounded, domains continue to accumulate. When the UI returns,
 * it immediately receives the current state.
 */
object DomainObserver {
    private val _observedDomains = MutableStateFlow<Map<String, ObservedDomain>>(emptyMap())
    val observedDomainsFlow: StateFlow<Map<String, ObservedDomain>> = _observedDomains.asStateFlow()

    fun addDomain(hostname: String, isBlocked: Boolean, isUserBlocked: Boolean) {
        val normalized = hostname.lowercase().trimEnd('.')
        val current = _observedDomains.value.toMutableMap()

        current[normalized] = ObservedDomain(
            hostname = normalized,
            isBlocked = isBlocked,
            isUserBlocked = isUserBlocked,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        _observedDomains.value = current
    }

    fun updateBlockedStates(userBlocked: Set<String>, userUnblocked: Set<String>) {
        val current = _observedDomains.value
        if (current.isEmpty()) return

        val updated = current.mapValues { (hostname, domain) ->
            domain.copy(
                isBlocked = BlockedDomains.isBlocked(hostname),
                isUserBlocked = BlockedDomains.isBlockedByUser(hostname)
            )
        }

        _observedDomains.value = updated
    }

    fun reset() {
        _observedDomains.value = emptyMap()
    }
}
