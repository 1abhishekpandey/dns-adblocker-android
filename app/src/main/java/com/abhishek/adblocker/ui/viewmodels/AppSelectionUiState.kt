package com.abhishek.adblocker.ui.viewmodels

import com.abhishek.adblocker.domain.InstalledApp

/**
 * UI state for the App Selection screen.
 *
 * Manages the list of installed apps, search functionality, and selection tracking.
 */
sealed class AppSelectionUiState {

    /**
     * Initial loading state while fetching installed apps.
     */
    data object Loading : AppSelectionUiState()

    /**
     * Success state with loaded apps and search/selection state.
     *
     * @property apps Complete list of installed apps (filtered to exclude uninstalled)
     * @property searchQuery Current search query for filtering apps
     * @property selectedCount Number of currently selected apps
     * @property hasChanges Whether selections differ from saved preferences
     */
    data class Success(
        val apps: List<InstalledApp>,
        val searchQuery: String = "",
        val selectedCount: Int = 0,
        val hasChanges: Boolean = false,
        val showUserApps: Boolean = true,
        val showSystemApps: Boolean = false
    ) : AppSelectionUiState() {

        /**
         * Computed property that filters apps based on app type and search query.
         *
         * First applies app type filtering based on showUserApps and showSystemApps flags.
         * Then searches across app name and package name (case-insensitive).
         * Returns all apps matching the filters if search query is empty.
         */
        val filteredApps: List<InstalledApp>
            get() {
                // First, apply app type filtering
                val typeFiltered = apps.filter { app ->
                    when {
                        app.isSystemApp -> showSystemApps
                        else -> showUserApps
                    }
                }

                // Then, apply search query filtering
                return if (searchQuery.isBlank()) {
                    typeFiltered
                } else {
                    val query = searchQuery.lowercase()
                    typeFiltered.filter { app ->
                        app.appName.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
                    }
                }
            }
    }

    /**
     * Error state when app loading fails.
     *
     * @property message User-friendly error message
     */
    data class Error(val message: String) : AppSelectionUiState()
}
