package com.qppd.ricedryer.data.model

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val devices: List<String> = emptyList(),
    val notificationEnabled: Boolean = true,
    val tempUnit: String = "C", // C or F
    val createdAt: Long = 0,
    val lastLogin: Long = 0
)

data class UserDevice(
    val deviceId: String = "",
    val deviceName: String = "",
    val addedAt: Long = 0,
    val favorite: Boolean = false
)
