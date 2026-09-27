package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.window.DialogProperties
import com.example.telegram.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val TelegramBlue = Color(0xFF24A1DE)

@Composable
fun TelegramLiveDialog(
    onDismiss: () -> Unit,
    onSaveAndApply: (String, ProtectedStreamKey) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val authState by TelegramManager.authState.collectAsState()
    val adminChannels by TelegramManager.adminChannels.collectAsState()
    val selectedChannel by TelegramManager.selectedChannel.collectAsState()
    val session by TelegramManager.session.collectAsState()
    val liveStatus by TelegramManager.liveStatus.collectAsState()

    var apiIdInput by remember { mutableStateOf("2040") }
    var apiHashInput by remember { mutableStateOf("b18441a1ff607e10a989891a5462e627") }
    var phoneInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var botTokenInput by remember { mutableStateOf("") }
    var useBotAuth by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var streamTitle by remember { mutableStateOf("OBS Studio Mobile Broadcast") }
    var autoDiscardOnStop by remember { mutableStateOf(true) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (authState.isAuthenticated && adminChannels.isEmpty()) {
            TelegramManager.refreshAdminChannels()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, TelegramBlue.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .testTag("telegram_live_dialog"),
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TELEGRAM CHANNEL LIVE",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Official MTProto RTMP Livestream Engine",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Live Status Pill
                    val statusColor = when (liveStatus) {
                        TelegramLiveStatus.IDLE -> TextMuted
                        TelegramLiveStatus.CONNECTING -> StudioAmber
                        TelegramLiveStatus.CONNECTED -> TelegramBlue
                        TelegramLiveStatus.LIVE -> StudioNeonGreen
                        TelegramLiveStatus.ERROR -> StudioLiveRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("telegram_status_badge")
                    ) {
                        Text(
                            text = liveStatus.label,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_tg_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(
                    color = StudioCardBorder,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Body content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Action Error Banner
                    if (actionError != null || authState.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioLiveRed.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioLiveRed)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = StudioLiveRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = actionError ?: authState.error ?: "Error occurred",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Step 1: Telegram Authentication
                    Text(
                        text = "1. TELEGRAM MTPROTO AUTHENTICATION",
                        color = TelegramBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (!authState.isAuthenticated) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { useBotAuth = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!useBotAuth) TelegramBlue else StudioDarkSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("User Phone Login", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { useBotAuth = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (useBotAuth) TelegramBlue else StudioDarkSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Bot / Token Login", fontSize = 11.sp)
                                    }
                                }

                                if (!useBotAuth) {
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it },
                                        label = { Text("Phone Number (+1234567890)") },
                                        modifier = Modifier.fillMaxWidth().testTag("tg_phone_input"),
                                        singleLine = true
                                    )

                                    if (authState.phoneCodeHash != null) {
                                        OutlinedTextField(
                                            value = codeInput,
                                            onValueChange = { codeInput = it },
                                            label = { Text("Telegram Verification Code") },
                                            modifier = Modifier.fillMaxWidth().testTag("tg_code_input"),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                isSubmitting = true
                                                coroutineScope.launch {
                                                    val res = TelegramManager.signInWithCode(codeInput)
                                                    isSubmitting = false
                                                    if (res.isFailure) actionError = res.exceptionOrNull()?.message
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("tg_signin_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                            enabled = !isSubmitting
                                        ) {
                                            Text("Confirm Code & Authenticate")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                isSubmitting = true
                                                coroutineScope.launch {
                                                    val id = apiIdInput.toIntOrNull() ?: 2040
                                                    val res = TelegramManager.sendVerificationCode(phoneInput, id, apiHashInput)
                                                    isSubmitting = false
                                                    if (res.isFailure) actionError = res.exceptionOrNull()?.message
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("tg_send_code_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                            enabled = !isSubmitting && phoneInput.isNotBlank()
                                        ) {
                                            Text("Send Verification Code")
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = botTokenInput,
                                        onValueChange = { botTokenInput = it },
                                        label = { Text("Bot Token (e.g. 123456:ABC-DEF...)") },
                                        modifier = Modifier.fillMaxWidth().testTag("tg_bot_token_input"),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            isSubmitting = true
                                            coroutineScope.launch {
                                                val id = apiIdInput.toIntOrNull() ?: 2040
                                                val res = TelegramManager.signInWithBotToken(botTokenInput, id, apiHashInput)
                                                isSubmitting = false
                                                if (res.isFailure) actionError = res.exceptionOrNull()?.message
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("tg_bot_auth_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                        enabled = !isSubmitting && botTokenInput.isNotBlank()
                                    ) {
                                        Text("Authorize Telegram Session")
                                    }
                                }
                            }
                        }
                    } else {
                        // Authenticated Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioNeonGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = StudioNeonGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Authenticated: @${authState.userName ?: "TelegramUser"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "MTProto Client ID: ${authState.userId ?: 777000L}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                TextButton(
                                    onClick = { TelegramManager.logout() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = StudioLiveRed)
                                ) {
                                    Text("Log Out")
                                }
                            }
                        }
                    }

                    // Step 2: Channel Selection
                    Text(
                        text = "2. TARGET TELEGRAM CHANNEL",
                        color = TelegramBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Channels with Manage Video Chats Right:",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                IconButton(
                                    onClick = { coroutineScope.launch { TelegramManager.refreshAdminChannels() } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TelegramBlue)
                                }
                            }

                            if (adminChannels.isEmpty()) {
                                Text(
                                    text = "No administrator channels detected yet. Click refresh or ensure your Telegram account is administrator with 'Manage Video Chats' permission.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            } else {
                                adminChannels.forEach { channel ->
                                    val isSelected = selectedChannel?.id == channel.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { TelegramManager.selectChannel(channel) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) TelegramBlue.copy(alpha = 0.2f) else StudioDarkSurface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) TelegramBlue else StudioCardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { TelegramManager.selectChannel(channel) },
                                                colors = RadioButtonDefaults.colors(selectedColor = TelegramBlue)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = channel.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "@${channel.username ?: "channel_${channel.id}"} • ID: ${channel.id}",
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(StudioNeonGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ADMIN: OK",
                                                    color = StudioNeonGreen,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: Stream Configuration & Official RTMP Lifecycle
                    Text(
                        text = "3. RTMP LIVESTREAM CREDENTIALS & LIFECYCLE",
                        color = TelegramBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = streamTitle,
                                onValueChange = { streamTitle = it },
                                label = { Text("Stream Title (shown in Telegram Channel)") },
                                modifier = Modifier.fillMaxWidth().testTag("tg_stream_title_input"),
                                singleLine = true
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = autoDiscardOnStop,
                                    onCheckedChange = { autoDiscardOnStop = it },
                                    colors = CheckboxDefaults.colors(checkedColor = TelegramBlue)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Terminate/Discard channel video chat on Stop Live",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }

                            // Dynamic Credentials Display
                            val currentCredentials = session.credentials
                            if (currentCredentials != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioNeonGreen.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "DYNAMIC RTMP SERVER URL:",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = currentCredentials.rtmpUrl,
                                            color = StudioCyan,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "SECRET STREAM KEY:",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "••••••••••••••••••••••••••••••••",
                                            color = StudioNeonGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "✓ Stream key safely secured in memory. Never exposed in logs.",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            // Action button: Obtain dynamic credentials
                            Button(
                                onClick = {
                                    val chan = selectedChannel
                                    if (chan == null) {
                                        actionError = "Please select a Telegram channel first"
                                        return@Button
                                    }
                                    isSubmitting = true
                                    actionError = null
                                    coroutineScope.launch {
                                        val res = TelegramManager.prepareTelegramLivestream(chan, streamTitle)
                                        isSubmitting = false
                                        if (res.isFailure) {
                                            actionError = res.exceptionOrNull()?.message
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("tg_prepare_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                enabled = !isSubmitting && selectedChannel != null
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isSubmitting) "Connecting to Telegram MTProto..." else "Fetch Live RTMP Endpoint (phone.getGroupCallStreamRtmpUrl)"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = StudioCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val creds = session.credentials
                            if (creds != null) {
                                onSaveAndApply(creds.rtmpUrl, creds.streamKey)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        enabled = session.credentials != null,
                        modifier = Modifier.testTag("tg_apply_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to OBS Studio")
                    }
                }
            }
        }
    }
}
