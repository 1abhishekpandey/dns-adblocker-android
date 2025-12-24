package com.abhishek.adblocker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abhishek.adblocker.data.apps.InstalledAppsRepository
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository

/**
 * Factory for creating AppSelectionViewModel with required dependencies.
 *
 * Injects InstalledAppsRepository and VpnPreferencesRepository into the ViewModel.
 *
 * @property installedAppsRepository Repository for fetching installed apps
 * @property vpnPreferencesRepository Repository for VPN preferences
 */
class AppSelectionViewModelFactory(
    private val installedAppsRepository: InstalledAppsRepository,
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppSelectionViewModel::class.java)) {
            return AppSelectionViewModel(
                installedAppsRepository = installedAppsRepository,
                vpnPreferencesRepository = vpnPreferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
