package com.abhishek.adblocker.ui.viewmodels

import com.abhishek.adblocker.domain.model.ObservedDomain

sealed class DomainMonitorUiState {
    data object Empty : DomainMonitorUiState()

    data class Monitoring(
        val observedDomains: List<ObservedDomain>,
        val searchQuery: String = "",
        val showBlockedOnly: Boolean = false,
        val totalObserved: Int = 0,
        val totalBlocked: Int = 0
    ) : DomainMonitorUiState() {

        val filteredDomains: List<ObservedDomain>
            get() {
                val filtered = if (showBlockedOnly) {
                    observedDomains.filter { it.isBlocked }
                } else {
                    observedDomains
                }
                return if (searchQuery.isBlank()) {
                    filtered
                } else {
                    filtered.filter { it.hostname.contains(searchQuery, ignoreCase = true) }
                }
            }
    }
}
