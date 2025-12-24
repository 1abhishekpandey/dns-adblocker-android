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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.adblocker.data.apps.InstalledAppsRepository
import com.abhishek.adblocker.data.preferences.VpnPreferencesRepository
import com.abhishek.adblocker.domain.model.AppInfo
import com.abhishek.adblocker.ui.viewmodels.AppSelectionUiState
import com.abhishek.adblocker.ui.viewmodels.AppSelectionViewModel
import com.abhishek.adblocker.ui.viewmodels.AppSelectionViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onNavigateBack: () -> Unit,
    isVpnActive: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AppSelectionViewModel = viewModel(
        factory = AppSelectionViewModelFactory(
            installedAppsRepository = InstalledAppsRepository(LocalContext.current.applicationContext),
            vpnPreferencesRepository = VpnPreferencesRepository(LocalContext.current.applicationContext)
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        handleBackNavigation(uiState, onNavigateBack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Select Apps") },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation(uiState, onNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is AppSelectionUiState.Success && (uiState as AppSelectionUiState.Success).hasChanges) {
                        TextButton(
                            onClick = {
                                viewModel.onSaveClicked(context, isVpnActive)
                                onNavigateBack()
                            }
                        ) {
                            Text(
                                text = "SAVE",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
                is AppSelectionUiState.Loading -> LoadingState()
                is AppSelectionUiState.Success -> SuccessState(
                    state = state,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onAppSelectionToggled = viewModel::onAppSelectionToggled,
                    onClearAllClicked = viewModel::onClearAllClicked,
                    onShowUserAppsToggled = viewModel::onShowUserAppsToggled,
                    onShowSystemAppsToggled = viewModel::onShowSystemAppsToggled
                )
                is AppSelectionUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = viewModel::loadInstalledApps
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading apps...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Error",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SuccessState(
    state: AppSelectionUiState.Success,
    onSearchQueryChanged: (String) -> Unit,
    onAppSelectionToggled: (String) -> Unit,
    onClearAllClicked: () -> Unit,
    onShowUserAppsToggled: () -> Unit,
    onShowSystemAppsToggled: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            query = state.searchQuery,
            onQueryChanged = onSearchQueryChanged
        )

        AppTypeFilterRow(
            showUserApps = state.showUserApps,
            showSystemApps = state.showSystemApps,
            onShowUserAppsToggled = onShowUserAppsToggled,
            onShowSystemAppsToggled = onShowSystemAppsToggled
        )

        InfoSection(
            selectedCount = state.selectedCount,
            totalCount = state.apps.size
        )

        if (state.selectedCount > 0) {
            ClearAllButton(onClick = onClearAllClicked)
        }

        AppList(
            apps = state.filteredApps,
            onAppSelectionToggled = onAppSelectionToggled
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
        placeholder = { Text("Search apps...") },
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
private fun AppTypeFilterRow(
    showUserApps: Boolean,
    showSystemApps: Boolean,
    onShowUserAppsToggled: () -> Unit,
    onShowSystemAppsToggled: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onShowUserAppsToggled)
        ) {
            Checkbox(
                checked = showUserApps,
                onCheckedChange = { onShowUserAppsToggled() }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Show user apps",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onShowSystemAppsToggled)
        ) {
            Checkbox(
                checked = showSystemApps,
                onCheckedChange = { onShowSystemAppsToggled() }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Show system apps",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoSection(
    selectedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$selectedCount apps selected",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (selectedCount == 0) {
                    "All apps will be routed through VPN"
                } else {
                    "Only selected apps will be routed through VPN"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ClearAllButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Clear All")
    }
}

@Composable
private fun AppList(
    apps: List<AppInfo>,
    onAppSelectionToggled: (String) -> Unit
) {
    if (apps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No apps found",
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
                items = apps,
                key = { it.packageName }
            ) { app ->
                AppListItem(
                    app = app,
                    onToggle = { onAppSelectionToggled(app.packageName) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = app.isSelected,
            onCheckedChange = { onToggle() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (app.isSystemApp) {
                    SystemAppBadge()
                }
            }

            Text(
                text = app.packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SystemAppBadge() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = "SYSTEM",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun handleBackNavigation(
    uiState: AppSelectionUiState,
    onNavigateBack: () -> Unit
) {
    onNavigateBack()
}
