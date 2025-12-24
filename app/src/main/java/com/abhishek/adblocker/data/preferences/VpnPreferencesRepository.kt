package com.abhishek.adblocker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abhishek.adblocker.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_preferences")

class VpnPreferencesRepository(private val context: Context) {
    private val vpnEnabledKey = booleanPreferencesKey("vpn_enabled")
    private val selectedAppsKey = stringSetPreferencesKey("selected_app_packages")

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[vpnEnabledKey] ?: false
        }

    val selectedAppPackages: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[selectedAppsKey] ?: emptySet()
        }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[vpnEnabledKey] = enabled
        }
    }

    suspend fun setSelectedAppPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[selectedAppsKey] = packages
        }
    }
}
