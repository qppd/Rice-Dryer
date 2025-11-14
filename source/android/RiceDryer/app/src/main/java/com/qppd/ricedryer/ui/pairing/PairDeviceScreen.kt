package com.qppd.ricedryer.ui.pairing

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairDeviceScreen(
    viewModel: PairDeviceViewModel,
    onNavigateBack: () -> Unit,
    onPairingSuccess: () -> Unit
) {
    var deviceId by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState) {
        if (uiState is PairDeviceUiState.Success) {
            onPairingSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair New Device") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Rice Dryer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter the device ID and 6-digit pairing code shown on your Rice Dryer LCD display",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Device ID Input
            OutlinedTextField(
                value = deviceId,
                onValueChange = { deviceId = it.uppercase() },
                label = { Text("Device ID (MAC Address)") },
                placeholder = { Text("AABBCCDDEEFF") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState is PairDeviceUiState.Error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pairing Code Input
            Text(
                text = "Pairing Code",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 6-digit code input with focus support
            val focusRequester = remember { FocusRequester() }
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Visual representation of 6-digit code
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { focusRequester.requestFocus() }
                ) {
                    repeat(6) { index ->
                        val digit = if (index < pairingCode.length) pairingCode[index].toString() else ""
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = 2.dp,
                                    color = if (uiState is PairDeviceUiState.Error) 
                                        MaterialTheme.colorScheme.error 
                                    else if (index == pairingCode.length)
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Actual input field (visible but styled to blend)
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pairingCode = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    label = { Text("Tap boxes above or type here") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = uiState is PairDeviceUiState.Error
                )
            }
            
            LaunchedEffect(Unit) {
                // Auto-focus on the text field when screen loads
                focusRequester.requestFocus()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pair Button
            Button(
                onClick = {
                    viewModel.pairDevice(deviceId, pairingCode)
                },
                enabled = deviceId.isNotBlank() && 
                         pairingCode.length == 6 && 
                         uiState !is PairDeviceUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is PairDeviceUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Pair Device")
                }
            }
            
            // Error Message
            if (uiState is PairDeviceUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as PairDeviceUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How to pair:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Power on your Rice Dryer\n" +
                               "2. Hold BUTTON 1 for 3 seconds to enter pairing mode\n" +
                               "3. The LCD will display the Device ID and Pairing Code\n" +
                               "4. Enter the code here within 10 minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
