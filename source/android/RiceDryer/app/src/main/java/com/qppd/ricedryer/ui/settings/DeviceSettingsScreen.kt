package com.qppd.ricedryer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsScreen(
    deviceId: String,
    deviceName: String,
    onNavigateBack: () -> Unit,
    onRenameDevice: (String) -> Unit,
    onRemoveDevice: () -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue(deviceName)) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Device ID: $deviceId", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Device Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onRenameDevice(name.text) },
                enabled = name.text.isNotBlank() && name.text != deviceName,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rename Device")
            }
            Divider()
            Button(
                onClick = { showRemoveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Remove Device")
            }
        }
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text("Remove Device") },
                text = { Text("Are you sure you want to remove this device? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRemoveDialog = false
                        onRemoveDevice()
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
