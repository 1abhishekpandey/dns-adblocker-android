package com.abhishek.adblocker.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.AdBlockerVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DnsServer(
    val name: String,
    val address: String,
    val description: String
)

class SettingsViewModel(
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModel() {

    private val _selectedDnsServer = MutableStateFlow("8.8.8.8")
    val selectedDnsServer: StateFlow<String> = _selectedDnsServer.asStateFlow()

    private val _customDnsServer = MutableStateFlow("")
    val customDnsServer: StateFlow<String> = _customDnsServer.asStateFlow()

    private val _isCustomDnsMode = MutableStateFlow(false)
    val isCustomDnsMode: StateFlow<Boolean> = _isCustomDnsMode.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val presetDnsServers = listOf(
        DnsServer("Google", "8.8.8.8", "Fast and reliable"),
        DnsServer("Cloudflare", "1.1.1.1", "Privacy-focused, fastest response time"),
        DnsServer("Quad9", "9.9.9.9", "Security and privacy focused"),
        DnsServer("OpenDNS", "208.67.222.222", "Content filtering, good performance")
    )

    init {
        loadCurrentDnsServer()
    }

    private fun loadCurrentDnsServer() {
        viewModelScope.launch {
            vpnPreferencesRepository.selectedDnsServer.collect { dnsServer ->
                _selectedDnsServer.value = dnsServer

                // Check if it's a custom DNS (not in preset list)
                val isPreset = presetDnsServers.any { it.address == dnsServer }
                _isCustomDnsMode.value = !isPreset
                if (!isPreset) {
                    _customDnsServer.value = dnsServer
                }
            }
        }
    }

    fun selectDnsServer(address: String) {
        _selectedDnsServer.value = address
        _isCustomDnsMode.value = false
        _errorMessage.value = null
    }

    fun enableCustomDnsMode() {
        _isCustomDnsMode.value = true
        _errorMessage.value = null
    }

    fun setCustomDnsServer(address: String) {
        _customDnsServer.value = address
        _errorMessage.value = null
    }

    fun saveDnsSettings(context: Context, isVpnActive: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                val dnsToSave = if (_isCustomDnsMode.value) {
                    _customDnsServer.value.trim()
                } else {
                    _selectedDnsServer.value
                }

                // Save to preferences (will validate IP address)
                vpnPreferencesRepository.setDnsServer(dnsToSave)

                Logger.i("DNS server saved: $dnsToSave")

                // If VPN is active, restart it to apply changes
                if (isVpnActive) {
                    Logger.i("Restarting VPN to apply DNS changes...")
                    restartVpnService(context)
                }

                onSuccess()
            } catch (e: IllegalArgumentException) {
                Logger.e("Failed to save DNS server: ${e.message}")
                _errorMessage.value = e.message ?: "Invalid DNS server address"
            } catch (e: Exception) {
                Logger.e("Unexpected error saving DNS server", e)
                _errorMessage.value = "Failed to save settings: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun restartVpnService(context: Context) {
        // Stop VPN
        val stopIntent = Intent(context, AdBlockerVpnService::class.java).apply {
            action = AdBlockerVpnService.ACTION_STOP
        }
        context.startService(stopIntent)

        delay(500) // Wait for cleanup

        // Start VPN
        val startIntent = Intent(context, AdBlockerVpnService::class.java).apply {
            action = AdBlockerVpnService.ACTION_START
        }
        context.startService(startIntent)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
