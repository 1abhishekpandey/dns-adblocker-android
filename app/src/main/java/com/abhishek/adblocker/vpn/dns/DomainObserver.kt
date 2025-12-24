package com.abhishek.adblocker.vpn.dns

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton that bridges VPN service domain observations to the UI layer.
 *
 * Uses SharedFlow to emit domain observations that can be collected
 * by ViewModels even across Activity recreations.
 */
object DomainObserver {
    private val _observedDomains = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val observedDomainsFlow: SharedFlow<String> = _observedDomains.asSharedFlow()

    suspend fun emitDomain(hostname: String) {
        _observedDomains.emit(hostname)
    }
}
