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
        val hasChanges: Boolean = false
    ) : AppSelectionUiState() {

        /**
         * Computed property that filters apps based on search query.
         *
         * Searches across app name and package name (case-insensitive).
         * Returns all apps if search query is empty.
         */
        val filteredApps: List<InstalledApp>
            get() = if (searchQuery.isBlank()) {
                apps
            } else {
                val query = searchQuery.lowercase()
                apps.filter { app ->
                    app.appName.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query)
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
