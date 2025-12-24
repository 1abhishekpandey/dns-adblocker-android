package com.abhishek.adblocker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.domain.model.ObservedDomain
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.dns.DomainObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DomainMonitorViewModel(
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DomainMonitorUiState>(DomainMonitorUiState.Empty)
    val uiState: StateFlow<DomainMonitorUiState> = _uiState.asStateFlow()

    private val observedDomainsMap: MutableMap<String, ObservedDomain> = mutableMapOf()

    init {
        observeDomains()
        observeUserBlockedDomainsChanges()
    }

    private fun observeDomains() {
        viewModelScope.launch {
            DomainObserver.observedDomainsFlow.collect { hostname ->
                addObservedDomain(hostname)
            }
        }
    }

    private fun observeUserBlockedDomainsChanges() {
        viewModelScope.launch {
            vpnPreferencesRepository.userBlockedDomains.collect { userBlockedDomains ->
                BlockedDomains.updateUserDomains(userBlockedDomains)
                refreshBlockedStates()
            }
        }
        viewModelScope.launch {
            vpnPreferencesRepository.userUnblockedDefaultDomains.collect {
                refreshBlockedStates()
            }
        }
    }

    private fun addObservedDomain(hostname: String) {
        val normalized = hostname.lowercase().trimEnd('.')

        val isBlocked = BlockedDomains.isBlocked(normalized)
        val isUserBlocked = BlockedDomains.isBlockedByUser(normalized)

        val existingDomain = observedDomainsMap[normalized]
        val updatedDomain = if (existingDomain != null) {
            existingDomain.copy(
                isBlocked = isBlocked,
                isUserBlocked = isUserBlocked,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        } else {
            ObservedDomain(
                hostname = normalized,
                isBlocked = isBlocked,
                isUserBlocked = isUserBlocked,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        }

        observedDomainsMap[normalized] = updatedDomain
        updateUiState()
    }

    private fun refreshBlockedStates() {
        observedDomainsMap.replaceAll { hostname, domain ->
            domain.copy(
                isBlocked = BlockedDomains.isBlocked(hostname),
                isUserBlocked = BlockedDomains.isBlockedByUser(hostname)
            )
        }
        updateUiState()
    }

    private fun updateUiState() {
        if (observedDomainsMap.isEmpty()) {
            _uiState.value = DomainMonitorUiState.Empty
            return
        }

        val sortedDomains = observedDomainsMap.values
            .sortedByDescending { it.lastSeenTimestamp }

        val currentState = _uiState.value
        val searchQuery = if (currentState is DomainMonitorUiState.Monitoring) {
            currentState.searchQuery
        } else {
            ""
        }

        val showBlockedOnly = if (currentState is DomainMonitorUiState.Monitoring) {
            currentState.showBlockedOnly
        } else {
            false
        }

        _uiState.value = DomainMonitorUiState.Monitoring(
            observedDomains = sortedDomains,
            searchQuery = searchQuery,
            showBlockedOnly = showBlockedOnly,
            totalObserved = sortedDomains.size,
            totalBlocked = sortedDomains.count { it.isBlocked }
        )
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is DomainMonitorUiState.Monitoring) {
            _uiState.value = currentState.copy(searchQuery = query)
        }
    }

    fun onShowBlockedOnlyToggled() {
        val currentState = _uiState.value
        if (currentState is DomainMonitorUiState.Monitoring) {
            _uiState.value = currentState.copy(showBlockedOnly = !currentState.showBlockedOnly)
        }
    }

    fun toggleDomainBlocked(hostname: String) {
        viewModelScope.launch {
            val isBlockedByDefault = BlockedDomains.isBlockedByDefault(hostname)
            val isBlockedByUser = BlockedDomains.isBlockedByUser(hostname)
            val isUnblockedByUser = BlockedDomains.isUnblockedByUser(hostname)

            when {
                // Case 1: Default domain that's currently blocked (not overridden)
                isBlockedByDefault && !isUnblockedByUser -> {
                    // Unblock by adding to override list
                    vpnPreferencesRepository.addUserUnblockedDefaultDomain(hostname)
                    Logger.i("Unblocked default domain: $hostname")
                }
                // Case 2: Default domain that was unblocked, now re-blocking
                isBlockedByDefault && isUnblockedByUser -> {
                    // Re-block by removing from override list
                    vpnPreferencesRepository.removeUserUnblockedDefaultDomain(hostname)
                    Logger.i("Re-blocked default domain: $hostname")
                }
                // Case 3: User-added domain, now unblocking
                isBlockedByUser -> {
                    // Unblock by removing from user list
                    vpnPreferencesRepository.removeUserBlockedDomain(hostname)
                    Logger.i("Unblocked user domain: $hostname")
                }
                // Case 4: Not blocked at all, now blocking
                else -> {
                    // Block by adding to user list
                    vpnPreferencesRepository.addUserBlockedDomain(hostname)
                    Logger.i("Blocked domain: $hostname")
                }
            }
        }
    }

    fun clearObservedDomains() {
        observedDomainsMap.clear()
        _uiState.value = DomainMonitorUiState.Empty
    }

    fun resetUserBlockedDomains() {
        viewModelScope.launch {
            vpnPreferencesRepository.clearUserBlockedDomains()
            vpnPreferencesRepository.clearUserUnblockedDefaultDomains()
            Logger.i("Reset all user blocked/unblocked domains to default")
        }
    }
}
