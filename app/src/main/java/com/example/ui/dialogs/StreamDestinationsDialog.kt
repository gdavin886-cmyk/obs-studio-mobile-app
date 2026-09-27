package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.telegram.*
import com.example.ui.theme.*

@Composable
fun StreamDestinationsDialog(
    destinations: List<StreamDestination>,
    encoderConfig: EncoderConfig,
    isLive: Boolean,
    onToggleDestination: (String) -> Unit,
    onUpdateDestination: (StreamDestination) -> Unit,
    onUpdateEncoder: (EncoderConfig) -> Unit,
    onTestConnection: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Output Platforms, 1: Hardware Encoder
    var currentEncoder by remember { mutableStateOf(encoderConfig) }
    var showTelegramDialog by remember { mutableStateOf(false) }

    if (showTelegramDialog) {
        TelegramLiveDialog(
            onDismiss = { showTelegramDialog = false },
            onSaveAndApply = { url, key ->
                val tg = destinations.find { it.platform == DestinationPlatform.TELEGRAM }
                if (tg != null) {
                    onUpdateDestination(tg.copy(serverUrl = url, streamKey = key.getSecret(), isEnabled = true))
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StudioCardBg)
                            .border(1.dp, StudioCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BROADCAST DESTINATIONS & ENCODER",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Real Live RTMP Ingest & Hardware Acceleration",
                            color = StudioCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCardBg)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selectedTab == 0) StudioCyan else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DESTINATIONS",
                            color = if (selectedTab == 0) StudioObsidian else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selectedTab == 1) StudioPurple else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HARDWARE ENCODER",
                            color = if (selectedTab == 1) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Destinations Tab
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        destinations.forEach { dest ->
                            DestinationCard(
                                destination = dest,
                                isLive = isLive,
                                onToggle = { onToggleDestination(dest.id) },
                                onUpdateKey = { newKey ->
                                    onUpdateDestination(dest.copy(streamKey = newKey))
                                },
                                onUpdateUrl = { newUrl ->
                                    onUpdateDestination(dest.copy(serverUrl = newUrl))
                                },
                                onTestConnection = { onTestConnection(dest.id) },
                                onOpenTelegramSetup = { showTelegramDialog = true }
                            )
                        }
                    }
                } else {
                    // Hardware Encoder Tab
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Codec Selector
                        Text(
                            text = "VIDEO ENCODER CODEC (CURRENT: ${currentEncoder.codec.codecName})",
                            color = StudioCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        HardwareCodec.values().forEach { codec ->
                            val isSelected = currentEncoder.codec == codec
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) StudioSurfaceVariant else StudioCardBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) StudioCyan else StudioCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        currentEncoder = currentEncoder.copy(codec = codec)
                                        onUpdateEncoder(currentEncoder)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        currentEncoder = currentEncoder.copy(codec = codec)
                                        onUpdateEncoder(currentEncoder)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = StudioCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = codec.codecName,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${codec.chipVendor} • ${if (isSelected) "CURRENTLY ACTIVE" else "AVAILABLE"}",
                                        color = if (codec.isHardware) StudioNeonGreen else TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Resolution Selector
                        Text(
                            text = "STREAM RESOLUTION (CURRENT: ${currentEncoder.resolution.label})",
                            color = StudioCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        StreamResolution.values().forEach { res ->
                            val isSelected = currentEncoder.resolution == res
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) StudioSurfaceVariant else StudioCardBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) StudioPurple else StudioCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        currentEncoder = currentEncoder.copy(
                                            resolution = res,
                                            targetBitrateKbps = res.defaultBitrate,
                                            fps = if (res.label.contains("60")) 60 else 30
                                        )
                                        onUpdateEncoder(currentEncoder)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        currentEncoder = currentEncoder.copy(
                                            resolution = res,
                                            targetBitrateKbps = res.defaultBitrate,
                                            fps = if (res.label.contains("60")) 60 else 30
                                        )
                                        onUpdateEncoder(currentEncoder)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = StudioPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = res.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${res.width}x${res.height} • Suggested: ${res.defaultBitrate} kbps",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Bitrate Slider
                        Text(
                            text = "CURRENT TARGET BITRATE: ${currentEncoder.targetBitrateKbps} KBPS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = currentEncoder.targetBitrateKbps.toFloat(),
                            onValueChange = {
                                currentEncoder = currentEncoder.copy(targetBitrateKbps = it.toInt())
                                onUpdateEncoder(currentEncoder)
                            },
                            valueRange = 1000f..12000f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioCyan,
                                activeTrackColor = StudioCyan,
                                inactiveTrackColor = StudioCardBorder
                            )
                        )

                        // Low-Latency Streaming Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioCardBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Low Latency Streaming (LL-HLS / RTMPS)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentEncoder.isLowLatencyMode) "CURRENT: ULTRA-LOW DELAY (< 40ms)" else "CURRENT: STANDARD BUFFER DELAY (~ 2-3s)",
                                    color = if (currentEncoder.isLowLatencyMode) StudioNeonGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = currentEncoder.isLowLatencyMode,
                                onCheckedChange = {
                                    currentEncoder = currentEncoder.copy(isLowLatencyMode = it)
                                    onUpdateEncoder(currentEncoder)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StudioNeonGreen,
                                    checkedTrackColor = Color(0xFF0F3A22)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("done_destinations_btn")
                ) {
                    Text(
                        text = "Save & Close",
                        color = StudioObsidian,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: StreamDestination,
    isLive: Boolean,
    onToggle: () -> Unit,
    onUpdateKey: (String) -> Unit,
    onUpdateUrl: (String) -> Unit,
    onTestConnection: () -> Unit,
    onOpenTelegramSetup: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var streamKeyInput by remember { mutableStateOf(destination.streamKey) }
    var serverUrlInput by remember { mutableStateOf(destination.serverUrl) }

    val isTelegram = destination.platform == DestinationPlatform.TELEGRAM
    val tgChannel by TelegramManager.selectedChannel.collectAsState()
    val tgLiveStatus by TelegramManager.liveStatus.collectAsState()

    val platformColor = when (destination.platform) {
        DestinationPlatform.TWITCH -> TwitchPurple
        DestinationPlatform.YOUTUBE -> YouTubeRed
        DestinationPlatform.OK_RU -> Color(0xFFEE8208) // OK.ru Orange
        DestinationPlatform.TELEGRAM -> Color(0xFF24A1DE) // Telegram Blue
        DestinationPlatform.FACEBOOK -> FacebookBlue
        DestinationPlatform.CUSTOM_RTMPS -> RtmpsGold
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (destination.isEnabled) platformColor else StudioCardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Platform Color Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(platformColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = destination.platform.platformName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTelegram) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TelegramBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = tgLiveStatus.label,
                                    color = TelegramBlue,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Text(
                        text = if (destination.serverUrl.isNotBlank()) destination.serverUrl else destination.platform.defaultUrl,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Live Status Badge
                if (isLive && destination.isEnabled && destination.connectionStatus == ConnectionStatus.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioLiveRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE (${destination.latencyMs}ms)",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else if (destination.isTestingConnection) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioCyan.copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TESTING...",
                            color = StudioCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else if (destination.connectionStatus == ConnectionStatus.CONNECTED && !isLive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F3A22))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ONLINE (${destination.latencyMs}ms)",
                            color = StudioNeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Switch(
                    checked = destination.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = platformColor,
                        checkedTrackColor = platformColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("toggle_dest_${destination.id}")
                )
            }

            // Real Connection Status Message Display (Always visible if message exists)
            if (!destination.connectionMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val isSuccess = destination.connectionStatus == ConnectionStatus.CONNECTED
                val isError = destination.connectionStatus == ConnectionStatus.ERROR
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSuccess) Color(0xFF0C2A18) else if (isError) Color(0xFF330000) else StudioDarkSurface)
                        .border(
                            1.dp,
                            if (isSuccess) StudioNeonGreen.copy(alpha = 0.5f) else if (isError) Color.Red.copy(alpha = 0.5f) else StudioCyan.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else if (isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isSuccess) StudioNeonGreen else if (isError) Color.Red else StudioCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = destination.connectionMessage,
                        color = if (isSuccess) Color(0xFF86EFAC) else if (isError) Color(0xFFFF9999) else StudioCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }

            // Real Connection Test Button
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onTestConnection,
                enabled = !destination.isTestingConnection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF162032),
                    contentColor = StudioCyan
                ),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("test_conn_${destination.id}")
            ) {
                if (destination.isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = StudioCyan, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connecting live socket...", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                } else {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = StudioCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡ Test Real RTMP Connection", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Telegram specific setup button
            if (isTelegram) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { onOpenTelegramSetup?.invoke() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TelegramBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TelegramBlue),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("open_telegram_setup_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (tgChannel != null) "Telegram MTProto: ${tgChannel?.title}" else "Telegram MTProto API Setup",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stream Key and URL input toggle
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = StudioCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isExpanded) "Hide Configuration" else "Edit Server URL & Stream Key",
                    color = StudioCyan,
                    fontSize = 10.sp
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = {
                        serverUrlInput = it
                        onUpdateUrl(it)
                    },
                    label = { Text("Server URL", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_${destination.id}")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = streamKeyInput,
                    onValueChange = {
                        streamKeyInput = it
                        onUpdateKey(it)
                    },
                    label = { Text("Stream Key", fontSize = 10.sp) },
                    placeholder = { Text("Enter your real stream key", fontSize = 10.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("key_input_${destination.id}")
                )
                if (streamKeyInput.isBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ℹ Enter your real Stream Key from ${destination.platform.platformName} before clicking GO LIVE.",
                        color = Color(0xFFFBBF24),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
