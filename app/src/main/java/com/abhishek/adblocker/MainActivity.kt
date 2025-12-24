package com.abhishek.adblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abhishek.adblocker.ui.navigation.NavRoutes
import com.abhishek.adblocker.ui.screens.AppSelectionScreen
import com.abhishek.adblocker.ui.screens.DomainMonitorScreen
import com.abhishek.adblocker.ui.screens.MainScreen
import com.abhishek.adblocker.ui.theme.AdBlockerTheme
import com.abhishek.adblocker.ui.viewmodels.MainViewModel
import com.abhishek.adblocker.ui.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdBlockerTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(LocalContext.current.applicationContext)
                )
                val isVpnActive by mainViewModel.isVpnEnabled.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.MAIN,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.MAIN) {
                            MainScreen(
                                onNavigateToAppSelection = {
                                    navController.navigate(NavRoutes.APP_SELECTION)
                                },
                                onNavigateToDomainMonitor = {
                                    navController.navigate(NavRoutes.DOMAIN_MONITOR)
                                }
                            )
                        }

                        composable(NavRoutes.APP_SELECTION) {
                            AppSelectionScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                isVpnActive = isVpnActive
                            )
                        }

                        composable(NavRoutes.DOMAIN_MONITOR) {
                            DomainMonitorScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
