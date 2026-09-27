package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SceneId
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import com.example.util.FileUtils
import com.example.viewmodel.StudioViewModel

@Composable
fun StudioDashboardScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isLive by viewModel.isLive.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isStudioMode by viewModel.isStudioMode.collectAsStateWithLifecycle()
    val streamDurationSeconds by viewModel.streamDurationSeconds.collectAsStateWithLifecycle()

    val programScene by viewModel.programScene.collectAsStateWithLifecycle()
    val previewScene by viewModel.previewScene.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()

    val transitionType by viewModel.transitionType.collectAsStateWithLifecycle()
    val transitionDurationMs by viewModel.transitionDurationMs.collectAsStateWithLifecycle()

    val destinations by viewModel.destinations.collectAsStateWithLifecycle()
    val encoderConfig by viewModel.encoderConfig.collectAsStateWithLifecycle()
    val chromaKeyConfig by viewModel.chromaKeyConfig.collectAsStateWithLifecycle()
    val customLogoConfig by viewModel.customLogoConfig.collectAsStateWithLifecycle()
    val scrollingTextConfig by viewModel.scrollingTextConfig.collectAsStateWithLifecycle()
    val mediaCastConfig by viewModel.mediaCastConfig.collectAsStateWithLifecycle()

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()

    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val webCastUrl by viewModel.webCastUrl.collectAsStateWithLifecycle()
    val mediaIsPlaying by viewModel.mediaIsPlaying.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Dialog Visibility States
    var showChromaKeyDialog by remember { mutableStateOf(false) }
    var showDestinationsDialog by remember { mutableStateOf(false) }
    var showTelegramLiveDialog by remember { mutableStateOf(false) }
    var showAlertSimulator by remember { mutableStateOf(false) }
    var showLogoDialog by remember { mutableStateOf(false) }
    var showScrollingTextDialog by remember { mutableStateOf(false) }
    var showWebCastDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Media Picker launcher from device storage (video or image)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = FileUtils.queryFileName(context, uri)
            val mime = context.contentResolver.getType(uri)
            val mediaType = FileUtils.detectMediaType(fileName, mime)
            viewModel.setMediaCastSource(
                uri = uri.toString(),
                fileName = fileName,
                mediaType = mediaType,
                mimeType = mime
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .testTag("studio_dashboard_screen"),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian)
            ) {
                // Top Studio Header & Telemetry Bar
                StudioHeaderBar(
                    isLive = isLive,
                    isRecording = isRecording,
                    isStudioMode = isStudioMode,
                    durationSeconds = streamDurationSeconds,
                    telemetry = telemetry,
                    onToggleLive = { viewModel.toggleLive() },
                    onToggleRecording = { viewModel.toggleRecording() },
                    onToggleStudioMode = { viewModel.toggleStudioMode() },
                    onOpenDestinations = { showDestinationsDialog = true },
                    onOpenAlertSimulator = { showAlertSimulator = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenTelegramLive = { showTelegramLiveDialog = true }
                )

                // Scrollable Broadcast Canvas & Control Deck
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isStudioMode) {
                    // Studio Mode: Stacked Preview (Staged) and Program (Live)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Staged Preview Monitor
                        ProgramMonitorView(
                            scene = previewScene,
                            isLive = false,
                            isRecording = false,
                            isPreview = true,
                            chromaKeyConfig = chromaKeyConfig,
                            customLogoConfig = customLogoConfig,
                            activeAlert = null,
                            onDismissAlert = {},
                            isFrontCamera = isFrontCamera,
                            isTorchOn = isTorchOn,
                            webCastUrl = webCastUrl,
                            onWebUrlChange = { viewModel.setWebCastUrl(it) },
                            mediaIsPlaying = mediaIsPlaying,
                            onToggleMediaPlay = { viewModel.toggleMediaPlayback() },
                            mediaCastConfig = mediaCastConfig,
                            onUploadMediaClick = { mediaPickerLauncher.launch("*/*") },
                            onClearMediaClick = { viewModel.clearMediaCastSource() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live Program Monitor
                        ProgramMonitorView(
                            scene = programScene,
                            isLive = isLive,
                            isRecording = isRecording,
                            isPreview = false,
                            chromaKeyConfig = chromaKeyConfig,
                            customLogoConfig = customLogoConfig,
                            activeAlert = activeAlert,
                            onDismissAlert = { viewModel.dismissAlert() },
                            isFrontCamera = isFrontCamera,
                            isTorchOn = isTorchOn,
                            webCastUrl = webCastUrl,
                            onWebUrlChange = { viewModel.setWebCastUrl(it) },
                            mediaIsPlaying = mediaIsPlaying,
                            onToggleMediaPlay = { viewModel.toggleMediaPlayback() },
                            mediaCastConfig = mediaCastConfig,
                            onUploadMediaClick = { mediaPickerLauncher.launch("*/*") },
                            onClearMediaClick = { viewModel.clearMediaCastSource() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Normal Program Monitor View (Full 16:9 Canvas)
                    ProgramMonitorView(
                        scene = programScene,
                        isLive = isLive,
                        isRecording = isRecording,
                        isPreview = false,
                        chromaKeyConfig = chromaKeyConfig,
                        customLogoConfig = customLogoConfig,
                        activeAlert = activeAlert,
                        onDismissAlert = { viewModel.dismissAlert() },
                        isFrontCamera = isFrontCamera,
                        isTorchOn = isTorchOn,
                        webCastUrl = webCastUrl,
                        onWebUrlChange = { viewModel.setWebCastUrl(it) },
                        mediaIsPlaying = mediaIsPlaying,
                        onToggleMediaPlay = { viewModel.toggleMediaPlayback() },
                        mediaCastConfig = mediaCastConfig,
                        onUploadMediaClick = { mediaPickerLauncher.launch("*/*") },
                        onClearMediaClick = { viewModel.clearMediaCastSource() },
                        scrollingTextConfig = scrollingTextConfig,
                        onToggleScrollingText = { viewModel.toggleScrollingText() },
                        onOpenScrollingTextDialog = { showScrollingTextDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Scene Switcher Carousel
                SceneSwitcherBar(
                    scenes = scenes,
                    activeScene = programScene,
                    previewScene = previewScene,
                    isStudioMode = isStudioMode,
                    selectedTransition = transitionType,
                    transitionDurationMs = transitionDurationMs,
                    onSelectScene = { viewModel.selectScene(it) },
                    onTransitionStudio = { viewModel.transitionStudio() },
                    onSelectTransition = { viewModel.setTransitionType(it) }
                )

                // Sources & Filter Manager (with quick filter buttons for Chroma, Custom Logo, Media Upload, and Ticker)
                SourcesLayerPanel(
                    sources = programScene.sources,
                    onToggleVisibility = { viewModel.toggleSourceVisibility(it) },
                    onToggleLock = { viewModel.toggleSourceLock(it) },
                    onOpenChromaKey = { showChromaKeyDialog = true },
                    onOpenLogoEditor = { showLogoDialog = true },
                    onOpenTickerEditor = { showScrollingTextDialog = true },
                    onSwitchCamera = { viewModel.switchCamera() },
                    onToggleTorch = { viewModel.toggleTorch() },
                    isTorchOn = isTorchOn,
                    onOpenMediaPicker = { mediaPickerLauncher.launch("*/*") }
                )

                // Dedicated Media Cast Control Deck (When MEDIA_CAST scene is active)
                if (programScene.id == SceneId.MEDIA_CAST) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                        border = BorderStroke(1.dp, StudioCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_cast_quick_panel")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
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
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = StudioCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MOBILE STORAGE VIDEO / IMAGE CASTING",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "CURRENT: ${mediaCastConfig.fileName} (${mediaCastConfig.mediaType.label})",
                                        color = StudioNeonGreen,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { mediaPickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("upload_storage_media_dashboard_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = StudioObsidian,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upload Video / Image",
                                        color = StudioObsidian,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!mediaCastConfig.uri.isNullOrEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearMediaCastSource() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRecRed),
                                        border = BorderStroke(1.dp, StudioRecRed),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("clear_media_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = "Reset",
                                            tint = StudioRecRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset Reel",
                                            color = StudioRecRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // If currently on Web Cast scene, show quick Web URL bar & Fullscreen launch
                if (programScene.id == SceneId.WEB_CAST) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showWebCastDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioSurfaceVariant
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = StudioCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CURRENT WEB CAST: $webCastUrl (TAP TO CHANGE)",
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Quick Studio Bar: Settings & Restart Studio Memory Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSettingsDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyan),
                        border = BorderStroke(1.dp, StudioCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("dashboard_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Studio Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.restartStudioResetMemory() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRecRed),
                        border = BorderStroke(1.dp, StudioRecRed.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("dashboard_restart_studio_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = StudioRecRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset Memory",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Audio Mixer Deck
                AudioMixerBar(
                    tracks = audioTracks,
                    onVolumeChange = { id, vol -> viewModel.setAudioVolume(id, vol) },
                    onToggleMute = { id -> viewModel.toggleAudioMute(id) }
                )
            }
        }
    }
}

    // Modal Dialogs
    if (showChromaKeyDialog) {
        ChromaKeyDialog(
            config = chromaKeyConfig,
            onSave = { viewModel.updateChromaKey(it) },
            onDismiss = { showChromaKeyDialog = false }
        )
    }

    if (showDestinationsDialog) {
        StreamDestinationsDialog(
            destinations = destinations,
            encoderConfig = encoderConfig,
            isLive = isLive,
            onToggleDestination = { viewModel.toggleDestination(it) },
            onUpdateDestination = { viewModel.updateDestination(it) },
            onUpdateEncoder = { viewModel.updateEncoderConfig(it) },
            onTestConnection = { viewModel.testDestinationConnection(it) },
            onDismiss = { showDestinationsDialog = false }
        )
    }

    if (showAlertSimulator) {
        AlertSimulatorSheet(
            onTriggerAlert = { type, name, amount, msg ->
                viewModel.triggerEngagementAlert(type, name, amount, msg)
            },
            onDismiss = { showAlertSimulator = false }
        )
    }

    if (showLogoDialog) {
        CustomLogoDialog(
            config = customLogoConfig,
            onSave = { viewModel.updateCustomLogo(it) },
            onStartCountdown = { sec, nextText ->
                viewModel.startWatermarkCountdown(sec, nextText)
            },
            onDismiss = { showLogoDialog = false }
        )
    }

    if (showScrollingTextDialog) {
        ScrollingTextDialog(
            config = scrollingTextConfig,
            isLive = isLive,
            onSave = { viewModel.updateScrollingTextConfig(it) },
            onDismiss = { showScrollingTextDialog = false }
        )
    }

    if (showWebCastDialog) {
        WebCastUrlDialog(
            currentUrl = webCastUrl,
            onSaveUrl = { viewModel.setWebCastUrl(it) },
            onDismiss = { showWebCastDialog = false }
        )
    }

    if (showSettingsDialog) {
        StudioSettingsDialog(
            settings = settings,
            encoderConfig = encoderConfig,
            destinations = destinations,
            isLive = isLive,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onUpdateEncoder = { viewModel.updateEncoderConfig(it) },
            onToggleDestination = { viewModel.toggleDestination(it) },
            onOpenDestinationsFull = {
                showSettingsDialog = false
                showDestinationsDialog = true
            },
            onOpenWebCastFullScreen = {
                showSettingsDialog = false
                val webScene = scenes.find { it.id == SceneId.WEB_CAST }
                if (webScene != null) viewModel.selectScene(webScene)
            },
            onRestartStudioResetMemory = {
                viewModel.restartStudioResetMemory()
            },
            onOpenLogoSettings = {
                showSettingsDialog = false
                showLogoDialog = true
            },
            onOpenTickerSettings = {
                showSettingsDialog = false
                showScrollingTextDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showTelegramLiveDialog) {
        TelegramLiveDialog(
            onDismiss = { showTelegramLiveDialog = false },
            onSaveAndApply = { url, key ->
                viewModel.updateTelegramDestination(url, key.getSecret())
            }
        )
    }
}
