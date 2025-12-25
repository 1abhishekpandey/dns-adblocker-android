package com.abhishek.adblocker.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.domain.model.VpnState
import com.abhishek.adblocker.ui.viewmodels.MainViewModel
import com.abhishek.adblocker.ui.viewmodels.MainViewModelFactory

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onNavigateToAppSelection: () -> Unit = {},
    onNavigateToDomainMonitor: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    val vpnEnabled by viewModel.isVpnEnabled.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val selectedAppCount by viewModel.selectedAppCount.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        AppTitle()

        StatusSection(vpnState = vpnState)

        VpnToggleSection(
            enabled = vpnEnabled,
            onToggle = { enabled ->
                if (enabled) {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        viewModel.toggleVpn(context)
                    }
                } else {
                    viewModel.toggleVpn(context)
                }
            }
        )

        BlockedDomainsSection()

        SelectedAppsSection(
            selectedCount = selectedAppCount,
            onSelectAppsClick = onNavigateToAppSelection
        )

        if (vpnEnabled && selectedAppCount > 0) {
            DomainMonitorSection(onMonitorDomainsClick = onNavigateToDomainMonitor)
        }

        SettingsSection(onSettingsClick = onNavigateToSettings)
    }
}

@Composable
private fun AppTitle() {
    Text(
        text = "AdBlocker",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StatusSection(vpnState: VpnState) {
    val statusText = when (vpnState) {
        is VpnState.Connected -> "Connected"
        is VpnState.Connecting -> "Connecting..."
        is VpnState.Disconnected -> "Disconnected"
    }

    val statusColor = when (vpnState) {
        is VpnState.Connected -> MaterialTheme.colorScheme.primary
        is VpnState.Connecting -> MaterialTheme.colorScheme.tertiary
        is VpnState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Status",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = statusText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )
    }
}

@Composable
private fun VpnToggleSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Enable VPN",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun BlockedDomainsSection() {
    val blockedCount = BlockedDomains.getBlockedDomains().size

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Blocked Domains",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = blockedCount.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SelectedAppsSection(
    selectedCount: Int,
    onSelectAppsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Selected Apps",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (selectedCount == 0) "All Apps" else selectedCount.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = onSelectAppsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Apps")
        }
    }
}

@Composable
private fun DomainMonitorSection(
    onMonitorDomainsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Domain Activity",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onMonitorDomainsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Monitor Domains")
        }
    }
}

@Composable
private fun SettingsSection(
    onSettingsClick: () -> Unit
) {
    Button(
        onClick = onSettingsClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Settings")
    }
}
