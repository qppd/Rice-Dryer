@file:OptIn(ExperimentalMaterial3Api::class)

package com.qppd.ricedryer.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.ui.auth.*
import com.qppd.ricedryer.ui.dashboard.*
import com.qppd.ricedryer.ui.pairing.*
import com.qppd.ricedryer.ui.devices.*

@Composable
fun RiceDryerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Screens
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Device List (Main screen after login)
        composable(Screen.DeviceList.route) {
            val deviceListViewModel: DeviceListViewModel = viewModel(
                factory = DeviceListViewModel.factory(database)
            )
            DeviceListScreen(
                viewModel = deviceListViewModel,
                onNavigateToDashboard = { deviceId ->
                    navController.navigate(Screen.Dashboard.createRoute(deviceId))
                },
                onNavigateToPairing = {
                    navController.navigate(Screen.PairDevice.route)
                },
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard
        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.factory(database, deviceId)
            )
            
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCharts = {
                    navController.navigate(Screen.Charts.createRoute(deviceId))
                },
                onNavigateToDevices = {
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.DeviceList.route) { inclusive = false }
                    }
                }
            )
        }
        
        // Pair Device
        composable(Screen.PairDevice.route) {
            val pairDeviceViewModel: PairDeviceViewModel = viewModel(
                factory = PairDeviceViewModel.factory(database)
            )
            
            PairDeviceScreen(
                viewModel = pairDeviceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPairingSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        // Charts Screen
        composable(
            route = Screen.Charts.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val chartsViewModel: com.qppd.ricedryer.ui.charts.ChartsViewModel = viewModel(
                factory = com.qppd.ricedryer.ui.charts.ChartsViewModel.factory(database, deviceId)
            )
            
            com.qppd.ricedryer.ui.charts.ChartsScreen(
                viewModel = chartsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
