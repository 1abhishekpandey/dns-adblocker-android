package com.abhishek.adblocker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_preferences")

class VpnPreferencesRepository(private val context: Context) {
    private val vpnEnabledKey = booleanPreferencesKey("vpn_enabled")

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[vpnEnabledKey] ?: false
        }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[vpnEnabledKey] = enabled
        }
    }
}
