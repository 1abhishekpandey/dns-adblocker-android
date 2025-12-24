package com.abhishek.adblocker.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.adblocker.data.apps.InstalledAppsRepository
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.domain.InstalledApp
import com.abhishek.adblocker.domain.model.AppInfo
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.AdBlockerVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the App Selection screen.
 *
 * Manages the list of installed apps, search functionality, selection state,
 * and persisting selections to DataStore. Restarts VPN service when selections
 * change to apply new routing rules.
 *
 * @property installedAppsRepository Repository for fetching installed apps
 * @property vpnPreferencesRepository Repository for VPN preferences (selected apps)
 */
class AppSelectionViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val vpnPreferencesRepository: VpnPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppSelectionUiState>(AppSelectionUiState.Loading)
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    private var initialSelectedPackages: Set<String> = emptySet()

    init {
        loadInstalledApps()
    }

    /**
     * Loads installed apps from repository and marks previously selected apps.
     *
     * Fetches all installed apps, filters out any uninstalled packages from preferences,
     * and initializes the success state with apps marked as selected based on saved preferences.
     */
    fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                _uiState.value = AppSelectionUiState.Loading

                val savedPackages = vpnPreferencesRepository.selectedAppPackages.first()
                initialSelectedPackages = savedPackages

                val allApps = installedAppsRepository.getInstalledApps()

                val validPackages = allApps.map { it.packageName }.toSet()
                val filteredSavedPackages = savedPackages.filter { it in validPackages }.toSet()

                if (filteredSavedPackages.size != savedPackages.size) {
                    vpnPreferencesRepository.setSelectedAppPackages(filteredSavedPackages)
                    initialSelectedPackages = filteredSavedPackages
                }

                val appsWithSelection = allApps.map { app ->
                    app.copy(isSelected = app.packageName in filteredSavedPackages)
                }.sortedWith(AppInfo.comparator())

                _uiState.value = AppSelectionUiState.Success(
                    apps = appsWithSelection,
                    searchQuery = "",
                    selectedCount = filteredSavedPackages.size,
                    hasChanges = false
                )

                Logger.d("Loaded ${allApps.size} apps, ${filteredSavedPackages.size} selected")
            } catch (e: Exception) {
                Logger.e("Failed to load installed apps: ${e.message}", e)
                _uiState.value = AppSelectionUiState.Error("Failed to load apps: ${e.message}")
            }
        }
    }

    /**
     * Updates the search query and filters the app list.
     *
     * @param query New search query (searches app name and package name)
     */
    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            _uiState.value = currentState.copy(searchQuery = query)
            Logger.d("Search query updated: '$query'")
        }
    }

    /**
     * Toggles the "show user apps" filter.
     */
    fun onShowUserAppsToggled() {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            _uiState.value = currentState.copy(showUserApps = !currentState.showUserApps)
            Logger.d("Show user apps toggled: ${!currentState.showUserApps}")
        }
    }

    /**
     * Toggles the "show system apps" filter.
     */
    fun onShowSystemAppsToggled() {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            _uiState.value = currentState.copy(showSystemApps = !currentState.showSystemApps)
            Logger.d("Show system apps toggled: ${!currentState.showSystemApps}")
        }
    }

    /**
     * Toggles selection state for a specific app.
     *
     * Updates the app's selection status and recalculates selected count and hasChanges flag.
     *
     * @param packageName Package name of the app to toggle
     */
    fun onAppSelectionToggled(packageName: String) {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            val updatedApps = currentState.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelected = !app.isSelected)
                } else {
                    app
                }
            }.sortedWith(AppInfo.comparator())

            val selectedCount = updatedApps.count { it.isSelected }
            val currentSelectedPackages = updatedApps.filter { it.isSelected }
                .map { it.packageName }
                .toSet()
            val hasChanges = currentSelectedPackages != initialSelectedPackages

            _uiState.value = currentState.copy(
                apps = updatedApps,
                selectedCount = selectedCount,
                hasChanges = hasChanges
            )

            Logger.d("Toggled $packageName, selected count: $selectedCount, hasChanges: $hasChanges")
        }
    }

    /**
     * Clears all app selections.
     *
     * Deselects all apps and updates state accordingly.
     */
    fun onClearAllClicked() {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            val clearedApps = currentState.apps.map { it.copy(isSelected = false) }
                .sortedWith(AppInfo.comparator())
            val hasChanges = initialSelectedPackages.isNotEmpty()

            _uiState.value = currentState.copy(
                apps = clearedApps,
                selectedCount = 0,
                hasChanges = hasChanges
            )

            Logger.d("Cleared all selections, hasChanges: $hasChanges")
        }
    }

    /**
     * Saves current selections to DataStore and restarts VPN service if active.
     *
     * Persists the selected package names to preferences. If VPN is currently active,
     * restarts the service to apply new routing rules.
     *
     * @param context Android context for service management
     * @param isVpnActive Whether VPN service is currently running
     */
    fun onSaveClicked(context: Context, isVpnActive: Boolean) {
        val currentState = _uiState.value
        if (currentState is AppSelectionUiState.Success) {
            viewModelScope.launch {
                try {
                    val selectedPackages = currentState.apps
                        .filter { it.isSelected }
                        .map { it.packageName }
                        .toSet()

                    vpnPreferencesRepository.setSelectedAppPackages(selectedPackages)
                    initialSelectedPackages = selectedPackages

                    _uiState.value = currentState.copy(hasChanges = false)

                    Logger.i("Saved ${selectedPackages.size} selected apps to preferences")

                    if (isVpnActive) {
                        restartVpnService(context)
                    }
                } catch (e: Exception) {
                    Logger.e("Failed to save app selections: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Restarts the VPN service to apply new routing rules.
     *
     * Stops the current VPN service, waits 500ms for cleanup, then starts it again.
     * This ensures new app selections are applied to the VPN tunnel configuration.
     *
     * @param context Android context for service management
     */
    private fun restartVpnService(context: Context) {
        viewModelScope.launch {
            try {
                Logger.i("Restarting VPN service to apply new app selections")

                val stopIntent = Intent(context, AdBlockerVpnService::class.java).apply {
                    action = AdBlockerVpnService.ACTION_STOP
                }
                context.startService(stopIntent)

                delay(500)

                val startIntent = Intent(context, AdBlockerVpnService::class.java).apply {
                    action = AdBlockerVpnService.ACTION_START
                }
                context.startService(startIntent)

                Logger.i("VPN service restarted successfully")
            } catch (e: Exception) {
                Logger.e("Failed to restart VPN service: ${e.message}", e)
            }
        }
    }
}
