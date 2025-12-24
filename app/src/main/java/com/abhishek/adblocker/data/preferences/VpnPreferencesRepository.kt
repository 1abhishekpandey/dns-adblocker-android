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
    private val userBlockedDomainsKey = stringSetPreferencesKey("user_blocked_domains")
    private val userUnblockedDefaultDomainsKey = stringSetPreferencesKey("user_unblocked_default_domains")

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[vpnEnabledKey] ?: false
        }

    val selectedAppPackages: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[selectedAppsKey] ?: emptySet()
        }

    val userBlockedDomains: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[userBlockedDomainsKey] ?: emptySet()
        }

    val userUnblockedDefaultDomains: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[userUnblockedDefaultDomainsKey] ?: emptySet()
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

    suspend fun addUserBlockedDomain(domain: String) {
        context.dataStore.edit { preferences ->
            val currentDomains = preferences[userBlockedDomainsKey] ?: emptySet()
            preferences[userBlockedDomainsKey] = currentDomains + domain
        }
    }

    suspend fun removeUserBlockedDomain(domain: String) {
        context.dataStore.edit { preferences ->
            val currentDomains = preferences[userBlockedDomainsKey] ?: emptySet()
            preferences[userBlockedDomainsKey] = currentDomains - domain
        }
    }

    suspend fun clearUserBlockedDomains() {
        context.dataStore.edit { preferences ->
            preferences[userBlockedDomainsKey] = emptySet()
        }
    }

    suspend fun addUserUnblockedDefaultDomain(domain: String) {
        context.dataStore.edit { preferences ->
            val currentDomains = preferences[userUnblockedDefaultDomainsKey] ?: emptySet()
            preferences[userUnblockedDefaultDomainsKey] = currentDomains + domain
        }
    }

    suspend fun removeUserUnblockedDefaultDomain(domain: String) {
        context.dataStore.edit { preferences ->
            val currentDomains = preferences[userUnblockedDefaultDomainsKey] ?: emptySet()
            preferences[userUnblockedDefaultDomainsKey] = currentDomains - domain
        }
    }

    suspend fun clearUserUnblockedDefaultDomains() {
        context.dataStore.edit { preferences ->
            preferences[userUnblockedDefaultDomainsKey] = emptySet()
        }
    }
}
