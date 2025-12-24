package com.abhishek.adblocker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository

class DomainMonitorViewModelFactory(
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DomainMonitorViewModel::class.java)) {
            return DomainMonitorViewModel(vpnPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
