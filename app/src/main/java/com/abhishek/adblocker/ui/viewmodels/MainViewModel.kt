package com.abhishek.adblocker.ui.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.domain.model.VpnState
import com.abhishek.adblocker.vpn.AdBlockerVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModel() {

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _isVpnEnabled = MutableStateFlow(false)
    val isVpnEnabled: StateFlow<Boolean> = _isVpnEnabled.asStateFlow()

    private val _selectedAppCount = MutableStateFlow(0)
    val selectedAppCount: StateFlow<Int> = _selectedAppCount.asStateFlow()

    init {
        resetVpnStateOnStartup()
        observeVpnPreferences()
        observeSelectedApps()
    }

    private fun resetVpnStateOnStartup() {
        viewModelScope.launch {
            vpnPreferencesRepository.setVpnEnabled(false)
        }
    }

    private fun observeVpnPreferences() {
        viewModelScope.launch {
            vpnPreferencesRepository.isVpnEnabled.collect { enabled ->
                _isVpnEnabled.value = enabled
                updateVpnState(enabled)
            }
        }
    }

    private fun observeSelectedApps() {
        viewModelScope.launch {
            vpnPreferencesRepository.selectedAppPackages.collect { selectedApps ->
                _selectedAppCount.value = selectedApps.size
            }
        }
    }

    private fun updateVpnState(enabled: Boolean) {
        _vpnState.value = if (enabled) VpnState.Connected else VpnState.Disconnected
    }

    fun checkVpnPermission(activity: Activity): Boolean {
        val intent = VpnService.prepare(activity)
        return intent == null
    }

    fun requestVpnPermission(activity: Activity, requestCode: Int) {
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun toggleVpn(context: Context) {
        viewModelScope.launch {
            val currentlyEnabled = _isVpnEnabled.value

            if (currentlyEnabled) {
                stopVpnService(context)
            } else {
                startVpnService(context)
            }
        }
    }

    private suspend fun startVpnService(context: Context) {
        _vpnState.value = VpnState.Connecting

        val serviceIntent = Intent(context, AdBlockerVpnService::class.java).apply {
            action = AdBlockerVpnService.ACTION_START
        }
        context.startService(serviceIntent)

        vpnPreferencesRepository.setVpnEnabled(true)
    }

    private suspend fun stopVpnService(context: Context) {
        val serviceIntent = Intent(context, AdBlockerVpnService::class.java).apply {
            action = AdBlockerVpnService.ACTION_STOP
        }
        context.startService(serviceIntent)

        vpnPreferencesRepository.setVpnEnabled(false)
        _vpnState.value = VpnState.Disconnected
    }

    override fun onCleared() {
        super.onCleared()
    }
}
