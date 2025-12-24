package com.abhishek.adblocker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.domain.model.ObservedDomain
import com.abhishek.adblocker.ui.viewmodels.DomainMonitorUiState
import com.abhishek.adblocker.ui.viewmodels.DomainMonitorViewModel
import com.abhishek.adblocker.ui.viewmodels.DomainMonitorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainMonitorScreen(
    onNavigateBack: () -> Unit,
    isVpnActive: Boolean = false,
    onRestartVpn: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DomainMonitorViewModel = viewModel(
        factory = DomainMonitorViewModelFactory(
            vpnPreferencesRepository = VpnPreferencesRepository(LocalContext.current.applicationContext)
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var domainToToggle by remember { mutableStateOf<ObservedDomain?>(null) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Domain Monitor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearObservedDomains() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear list"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DomainMonitorUiState.Empty -> EmptyState()
                is DomainMonitorUiState.Monitoring -> MonitoringState(
                    state = state,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onShowBlockedOnlyToggled = viewModel::onShowBlockedOnlyToggled,
                    onDomainClicked = { domain -> domainToToggle = domain },
                    onResetClicked = { showResetDialog = true }
                )
            }
        }
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                viewModel.resetUserBlockedDomains()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    domainToToggle?.let { domain ->
        BlockUnblockDialog(
            domain = domain,
            isVpnActive = isVpnActive,
            onConfirm = {
                viewModel.toggleDomainBlocked(domain.hostname)
                if (isVpnActive) {
                    onRestartVpn()
                }
                domainToToggle = null
            },
            onDismiss = { domainToToggle = null }
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No domains observed yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonitoringState(
    state: DomainMonitorUiState.Monitoring,
    onSearchQueryChanged: (String) -> Unit,
    onShowBlockedOnlyToggled: () -> Unit,
    onDomainClicked: (ObservedDomain) -> Unit,
    onResetClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            query = state.searchQuery,
            onQueryChanged = onSearchQueryChanged
        )

        StatsAndFilterRow(
            totalObserved = state.totalObserved,
            totalBlocked = state.totalBlocked,
            showBlockedOnly = state.showBlockedOnly,
            onShowBlockedOnlyToggled = onShowBlockedOnlyToggled
        )

        OutlinedButton(
            onClick = onResetClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Reset to Default Blocklist")
        }

        DomainList(
            domains = state.filteredDomains,
            onDomainClicked = onDomainClicked
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search domains...") },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun StatsAndFilterRow(
    totalObserved: Int,
    totalBlocked: Int,
    showBlockedOnly: Boolean,
    onShowBlockedOnlyToggled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$totalObserved domains observed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$totalBlocked blocked",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onShowBlockedOnlyToggled)
            ) {
                Checkbox(
                    checked = showBlockedOnly,
                    onCheckedChange = { onShowBlockedOnlyToggled() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Blocked only",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DomainList(
    domains: List<ObservedDomain>,
    onDomainClicked: (ObservedDomain) -> Unit
) {
    if (domains.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No domains found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = domains,
                key = { it.hostname }
            ) { domain ->
                DomainListItem(
                    domain = domain,
                    onClick = { onDomainClicked(domain) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DomainListItem(
    domain: ObservedDomain,
    onClick: () -> Unit
) {
    val isInDefault = BlockedDomains.isBlockedByDefault(domain.hostname)
    val isUnblockedDefault = isInDefault && !domain.isBlocked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = domain.hostname,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (domain.isBlocked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (domain.isBlocked) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Default list badge
                if (isInDefault) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Default",
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Status text
                if (domain.isBlocked) {
                    Text(
                        text = if (domain.isUserBlocked) "User blocked" else "Blocked",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isUnblockedDefault) {
                    Text(
                        text = "Overridden",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (domain.isBlocked) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Blocked",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset to Default Blocklist") },
        text = { Text("This will remove all user-blocked domains. Only the default blocklist will remain active.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BlockUnblockDialog(
    domain: ObservedDomain,
    isVpnActive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (domain.isBlocked) "Unblock Domain?" else "Block Domain?"
    val baseMessage = when {
        domain.isBlocked && domain.isUserBlocked ->
            "Unblock ${domain.hostname}? (User blocked)"
        domain.isBlocked && !domain.isUserBlocked ->
            "Unblock ${domain.hostname}? (Default blocklist - will override)"
        else ->
            "Block ${domain.hostname}?"
    }
    val message = if (isVpnActive) {
        "$baseMessage\n\nVPN will restart to apply changes immediately."
    } else {
        baseMessage
    }
    val confirmText = if (domain.isBlocked) "Unblock" else "Block"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
