package com.qppd.ricedryer.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Dashboard : Screen("dashboard/{deviceId}") {
        fun createRoute(deviceId: String) = "dashboard/$deviceId"
    }
    object DeviceList : Screen("device_list")
    object PairDevice : Screen("pair_device")
    object Charts : Screen("charts/{deviceId}") {
        fun createRoute(deviceId: String) = "charts/$deviceId"
    }
    object DeviceSettings : Screen("device_settings/{deviceId}") {
        fun createRoute(deviceId: String) = "device_settings/$deviceId"
    }
    object Profile : Screen("profile")
}
