package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.service.StudioBroadcastService
import com.example.telegram.*
import com.example.util.StudioPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.random.Random

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = StudioPreferences(application)

    // Broadcasting & Recording State
    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isStudioMode = MutableStateFlow(false)
    val isStudioMode: StateFlow<Boolean> = _isStudioMode.asStateFlow()

    private val _streamDurationSeconds = MutableStateFlow(0L)
    val streamDurationSeconds: StateFlow<Long> = _streamDurationSeconds.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(preferences.loadSettings())
    val settings: StateFlow<StudioSettings> = _settings.asStateFlow()

    // Scenes & Transition
    private val defaultScenes = listOf(
        StudioScene(
            id = SceneId.CAMERA_GREENSCREEN,
            name = "Camera + Chroma Key",
            sources = listOf(
                SourceItem("cam_main", SourceType.CAMERA, "Main Camera (Chroma Key)", isVisible = true),
                SourceItem("logo_ovl", SourceType.CUSTOM_LOGO, "Custom Logo Watermark", isVisible = true),
                SourceItem("alert_box", SourceType.ALERT_OVERLAY, "Engagement Alert Box", isVisible = true)
            )
        ),
        StudioScene(
            id = SceneId.SCREEN_CAST,
            name = "Screen Share + PIP",
            sources = listOf(
                SourceItem("screen_cap", SourceType.SCREEN, "Mobile Display Capture", isVisible = true),
                SourceItem("pip_facecam", SourceType.CAMERA, "PIP Facecam Overlay", isVisible = true),
                SourceItem("logo_ovl", SourceType.CUSTOM_LOGO, "Custom Logo Watermark", isVisible = true),
                SourceItem("alert_box", SourceType.ALERT_OVERLAY, "Engagement Alert Box", isVisible = true)
            )
        ),
        StudioScene(
            id = SceneId.WEB_CAST,
            name = "Website Page Cast",
            sources = listOf(
                SourceItem("web_view", SourceType.WEB_PAGE, "Interactive Web Browser", isVisible = true),
                SourceItem("logo_ovl", SourceType.CUSTOM_LOGO, "Custom Logo Watermark", isVisible = true),
                SourceItem("alert_box", SourceType.ALERT_OVERLAY, "Engagement Alert Box", isVisible = true)
            )
        ),
        StudioScene(
            id = SceneId.MEDIA_CAST,
            name = "Media File Video Cast",
            sources = listOf(
                SourceItem("file_video", SourceType.FILE_MEDIA, "Broadcast Video / Slides", isVisible = true),
                SourceItem("logo_ovl", SourceType.CUSTOM_LOGO, "Custom Logo Watermark", isVisible = true),
                SourceItem("alert_box", SourceType.ALERT_OVERLAY, "Engagement Alert Box", isVisible = true)
            )
        ),
        StudioScene(
            id = SceneId.STARTING_SOON,
            name = "Starting Soon / BRB",
            sources = listOf(
                SourceItem("intro_anim", SourceType.FILE_MEDIA, "Countdown Timer & Intro", isVisible = true),
                SourceItem("logo_ovl", SourceType.CUSTOM_LOGO, "Custom Logo Watermark", isVisible = true),
                SourceItem("alert_box", SourceType.ALERT_OVERLAY, "Engagement Alert Box", isVisible = true)
            )
        )
    )

    private val _scenes = MutableStateFlow(defaultScenes)
    val scenes: StateFlow<List<StudioScene>> = _scenes.asStateFlow()

    private val _programScene = MutableStateFlow(defaultScenes[0])
    val programScene: StateFlow<StudioScene> = _programScene.asStateFlow()

    private val _previewScene = MutableStateFlow(defaultScenes[1])
    val previewScene: StateFlow<StudioScene> = _previewScene.asStateFlow()

    private val _transitionType = MutableStateFlow(TransitionType.FADE)
    val transitionType: StateFlow<TransitionType> = _transitionType.asStateFlow()

    private val _transitionDurationMs = MutableStateFlow(300)
    val transitionDurationMs: StateFlow<Int> = _transitionDurationMs.asStateFlow()

    // Multi-Destination Streaming (Twitch, YouTube, OK.ru, Telegram, Facebook, Custom)
    private val defaultDestinations = listOf(
        StreamDestination(
            id = "dest_twitch",
            platform = DestinationPlatform.TWITCH,
            serverUrl = "rtmps://live.twitch.tv/app/",
            streamKey = "",
            isEnabled = true
        ),
        StreamDestination(
            id = "dest_youtube",
            platform = DestinationPlatform.YOUTUBE,
            serverUrl = "rtmps://a.rtmp.youtube.com/live2",
            streamKey = "",
            isEnabled = false
        ),
        StreamDestination(
            id = "dest_telegram",
            platform = DestinationPlatform.TELEGRAM,
            serverUrl = "rtmps://dc4-1.rtmp.t.me/s/",
            streamKey = "",
            isEnabled = false
        ),
        StreamDestination(
            id = "dest_okru",
            platform = DestinationPlatform.OK_RU,
            serverUrl = "rtmp://vsu.okcdn.ru/input/",
            streamKey = "",
            isEnabled = false
        ),
        StreamDestination(
            id = "dest_facebook",
            platform = DestinationPlatform.FACEBOOK,
            serverUrl = "rtmps://live-api-s.facebook.com:443/rtmp/",
            streamKey = "",
            isEnabled = false
        ),
        StreamDestination(
            id = "dest_custom",
            platform = DestinationPlatform.CUSTOM_RTMPS,
            serverUrl = "rtmp://",
            streamKey = "",
            isEnabled = false
        )
    )

    private val _destinations = MutableStateFlow(
        defaultDestinations.map { preferences.restoreDestination(it) }
    )
    val destinations: StateFlow<List<StreamDestination>> = _destinations.asStateFlow()

    // Encoder Configuration
    private val _encoderConfig = MutableStateFlow(
        preferences.loadEncoderConfig() ?: EncoderConfig()
    )
    val encoderConfig: StateFlow<EncoderConfig> = _encoderConfig.asStateFlow()

    // Chroma Key Filter Configuration
    private val _chromaKeyConfig = MutableStateFlow(ChromaKeyConfig())
    val chromaKeyConfig: StateFlow<ChromaKeyConfig> = _chromaKeyConfig.asStateFlow()

    // Custom Logo Configuration
    private val _customLogoConfig = MutableStateFlow(preferences.loadCustomLogo())
    val customLogoConfig: StateFlow<CustomLogoConfig> = _customLogoConfig.asStateFlow()

    // Scrolling News / Broadcasting Inform Text Ticker
    private val _scrollingTextConfig = MutableStateFlow(preferences.loadScrollingText())
    val scrollingTextConfig: StateFlow<ScrollingTextConfig> = _scrollingTextConfig.asStateFlow()

    private var watermarkCountdownJob: Job? = null

    // Telemetry & Hardware Stats
    private val _telemetry = MutableStateFlow(StudioTelemetry())
    val telemetry: StateFlow<StudioTelemetry> = _telemetry.asStateFlow()

    // Audio Mixer
    private val _audioTracks = MutableStateFlow(
        listOf(
            AudioTrack("mic_aux", "Mic / Audio In", volume = 1.0f, isMuted = false, peakDb = -14f),
            AudioTrack("desktop_sys", "Desktop / Screen Audio", volume = 0.85f, isMuted = false, peakDb = -18f),
            AudioTrack("media_player", "Media / Video Source", volume = 0.70f, isMuted = false, peakDb = -22f),
            AudioTrack("alert_sfx", "Viewer Alert SFX", volume = 0.90f, isMuted = false, peakDb = -10f)
        )
    )
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    // Engagement Alerts Queue
    private val _activeAlert = MutableStateFlow<EngagementAlert?>(null)
    val activeAlert: StateFlow<EngagementAlert?> = _activeAlert.asStateFlow()

    // Camera & Web Cast States
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _webCastUrl = MutableStateFlow(
        preferences.loadSavedWebCastUrl() ?: "https://en.wikipedia.org/wiki/Open_Broadcaster_Software"
    )
    val webCastUrl: StateFlow<String> = _webCastUrl.asStateFlow()

    private val _isWebCastFullScreen = MutableStateFlow(false)
    val isWebCastFullScreen: StateFlow<Boolean> = _isWebCastFullScreen.asStateFlow()

    private val _mediaIsPlaying = MutableStateFlow(true)
    val mediaIsPlaying: StateFlow<Boolean> = _mediaIsPlaying.asStateFlow()

    private val _mediaCastConfig = MutableStateFlow(CustomMediaCastConfig())
    val mediaCastConfig: StateFlow<CustomMediaCastConfig> = _mediaCastConfig.asStateFlow()

    private val _screenCastGameMode = MutableStateFlow(true)
    val screenCastGameMode: StateFlow<Boolean> = _screenCastGameMode.asStateFlow()

    private var tickerJob: Job? = null
    private var alertDismissJob: Job? = null

    init {
        // Restore scene memory if enabled
        preferences.loadSavedScene()?.let { savedId ->
            defaultScenes.find { it.id == savedId }?.let { scene ->
                _programScene.value = scene
            }
        }

        // Setup notification action triggers
        StudioBroadcastService.onNotificationActionTriggered = { action ->
            when (action) {
                StudioBroadcastService.ACTION_STOP_FROM_NOTIFICATION -> {
                    if (_isLive.value) toggleLive()
                }
                StudioBroadcastService.ACTION_TOGGLE_MUTE_FROM_NOTIFICATION -> {
                    toggleAudioMute("mic_aux")
                }
            }
        }

        startTelemetrySimulation()
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun getActiveDestinationsSummary(): String {
        val enabled = _destinations.value.filter { it.isEnabled }.map { it.platform.platformName }
        return if (enabled.isEmpty()) "No active platform" else enabled.joinToString(", ")
    }

    private fun startTelemetrySimulation() {
        viewModelScope.launch {
            while (isActive) {
                delay(1200)
                if (_isLive.value) {
                    val baseBitrate = _encoderConfig.value.targetBitrateKbps
                    val jitterBitrate = (baseBitrate + Random.nextInt(-180, 180)).coerceAtLeast(500)
                    val latency = if (_encoderConfig.value.isLowLatencyMode) Random.nextInt(32, 58) else Random.nextInt(120, 240)
                    _telemetry.value = _telemetry.value.copy(
                        fps = if (Random.nextFloat() > 0.95f) 59 else 60,
                        bitrateKbps = jitterBitrate,
                        droppedFramesPercent = (Random.nextFloat() * 0.05f).coerceAtLeast(0f),
                        cpuUsagePercent = Random.nextInt(16, 24),
                        rtmpsLatencyMs = latency,
                        hardwareEncoderEngaged = _encoderConfig.value.codec.isHardware
                    )

                    // Update live destination readouts
                    _destinations.update { list ->
                        list.map { dest ->
                            if (dest.isEnabled && dest.connectionStatus == ConnectionStatus.CONNECTED) {
                                val destLatency = if (dest.platform == DestinationPlatform.OK_RU) latency + Random.nextInt(40, 80) else latency + Random.nextInt(-5, 10)
                                val destLoss = if (dest.platform == DestinationPlatform.OK_RU) Random.nextFloat() * 1.2f else Random.nextFloat() * 0.1f
                                dest.copy(
                                    isLive = true,
                                    currentBitrateKbps = jitterBitrate + Random.nextInt(-40, 40),
                                    latencyMs = destLatency,
                                    droppedFramesPercent = destLoss
                                )
                            } else {
                                // Do not overwrite ERROR or CONNECTING states for enabled destinations
                                dest
                            }
                        }
                    }

                    // Update background foreground notification
                    if (_settings.value.backgroundLiveEnabled) {
                        val isMicMuted = _audioTracks.value.find { it.id == "mic_aux" }?.isMuted ?: false
                        StudioBroadcastService.updateService(
                            context = getApplication(),
                            duration = formatDuration(_streamDurationSeconds.value),
                            bitrateKbps = jitterBitrate,
                            destinations = getActiveDestinationsSummary(),
                            isPopupLive = _settings.value.notificationPopupLive,
                            isMuted = isMicMuted
                        )
                    }
                } else {
                    _telemetry.value = _telemetry.value.copy(
                        bitrateKbps = 0,
                        cpuUsagePercent = Random.nextInt(7, 12),
                        rtmpsLatencyMs = 0
                    )
                }

                // Audio meter fluctuation
                _audioTracks.update { list ->
                    list.map { track ->
                        if (track.isMuted) {
                            track.copy(peakDb = -60f)
                        } else {
                            val base = if (track.id == "mic_aux") -14f else -20f
                            val jitter = (Random.nextFloat() * 8f) - 4f
                            track.copy(peakDb = (base + jitter).coerceIn(-60f, -2f))
                        }
                    }
                }
            }
        }
    }

    fun toggleLive() {
        val nextLive = !_isLive.value
        _isLive.value = nextLive

        if (nextLive) {
            _streamDurationSeconds.value = 0L
            tickerJob?.cancel()
            tickerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    _streamDurationSeconds.update { it + 1 }
                }
            }
            
            // Set all enabled destinations to connecting initially
            _destinations.update { list ->
                list.map { 
                    if (it.isEnabled) it.copy(connectionStatus = ConnectionStatus.CONNECTING, connectionMessage = "Connecting to server...") 
                    else it 
                }
            }
            
            viewModelScope.launch {
                val updatedDestinations = _destinations.value.toMutableList()
                val enabledDests = updatedDestinations.filter { it.isEnabled }
                if (enabledDests.isEmpty()) {
                    _isLive.value = false
                    tickerJob?.cancel()
                    _streamDurationSeconds.value = 0L
                    return@launch
                }
                
                val primaryDest = enabledDests.first()
                val destIndex = updatedDestinations.indexOf(primaryDest)
                
                var targetServerUrl = primaryDest.serverUrl
                var targetStreamKey = primaryDest.streamKey

                // Official Telegram Livestream Flow
                if (primaryDest.platform == DestinationPlatform.TELEGRAM) {
                    val channel = TelegramManager.selectedChannel.value
                    if (channel == null) {
                        val errMsg = "Please select a Telegram channel in Broadcast Destinations"
                        TelegramManager.updateLiveStatus(TelegramLiveStatus.ERROR, errMsg)
                        _destinations.update { list ->
                            list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = errMsg) else it }
                        }
                        _isLive.value = false
                        tickerJob?.cancel()
                        _streamDurationSeconds.value = 0L
                        StudioBroadcastService.stopService(getApplication())
                        return@launch
                    }

                    TelegramManager.updateLiveStatus(TelegramLiveStatus.CONNECTING, "Executing Telegram MTProto API flow...")
                    val prepResult = TelegramManager.prepareTelegramLivestream(channel)
                    if (prepResult.isFailure) {
                        val errMsg = prepResult.exceptionOrNull()?.message ?: "Telegram livestream setup failed"
                        TelegramManager.updateLiveStatus(TelegramLiveStatus.ERROR, errMsg)
                        _destinations.update { list ->
                            list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = errMsg) else it }
                        }
                        _isLive.value = false
                        tickerJob?.cancel()
                        _streamDurationSeconds.value = 0L
                        StudioBroadcastService.stopService(getApplication())
                        return@launch
                    }

                    val creds = prepResult.getOrThrow()
                    targetServerUrl = creds.rtmpUrl
                    targetStreamKey = creds.streamKey.getSecret()

                    _destinations.update { list ->
                        list.map { if (it.id == primaryDest.id) it.copy(serverUrl = targetServerUrl, streamKey = targetStreamKey) else it }
                    }
                    TelegramManager.updateLiveStatus(TelegramLiveStatus.CONNECTED, "Telegram RTMP endpoint ready")
                }

                if (targetStreamKey.isBlank()) {
                    _destinations.update { list ->
                        list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Stream key is missing or empty") else it }
                    }
                    _isLive.value = false
                    tickerJob?.cancel()
                    _streamDurationSeconds.value = 0L
                    StudioBroadcastService.stopService(getApplication())
                    return@launch
                }
                
                if (targetServerUrl.isBlank()) {
                    _destinations.update { list ->
                        list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Server URL is missing") else it }
                    }
                    _isLive.value = false
                    tickerJob?.cancel()
                    _streamDurationSeconds.value = 0L
                    StudioBroadcastService.stopService(getApplication())
                    return@launch
                }
                
                val fullUrl = if (targetServerUrl.endsWith("/")) {
                    targetServerUrl + targetStreamKey
                } else {
                    targetServerUrl + "/" + targetStreamKey
                }
                
                val config = _encoderConfig.value
                
                // Real Live Streaming Implementation
                com.example.stream.StreamManager.onConnectionSuccess = {
                    if (primaryDest.platform == DestinationPlatform.TELEGRAM) {
                        TelegramManager.updateLiveStatus(TelegramLiveStatus.CONNECTED, "RTMP socket connected. Verifying Telegram group call...")
                        viewModelScope.launch {
                            val active = TelegramManager.verifyAndSetLive()
                            if (active) {
                                _destinations.update { list ->
                                    list.mapIndexed { i, d ->
                                        if (i == destIndex) {
                                            d.copy(isLive = true, connectionStatus = ConnectionStatus.CONNECTED, connectionMessage = "Live on Telegram Channel")
                                        } else if (d.isEnabled) {
                                            d.copy(connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Skipped (Multi-stream not implemented)")
                                        } else d
                                    }
                                }
                            } else {
                                TelegramManager.updateLiveStatus(TelegramLiveStatus.ERROR, "Telegram channel livestream is not active")
                                _destinations.update { list ->
                                    list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Telegram livestream not active") else it }
                                }
                                _isLive.value = false
                                tickerJob?.cancel()
                                _streamDurationSeconds.value = 0L
                                StudioBroadcastService.stopService(getApplication())
                            }
                        }
                    } else {
                        _destinations.update { list ->
                            list.mapIndexed { i, d ->
                                if (i == destIndex) {
                                    d.copy(isLive = true, connectionStatus = ConnectionStatus.CONNECTED, connectionMessage = "Connected to real encoder pipeline")
                                } else if (d.isEnabled) {
                                    d.copy(connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Skipped (Multi-stream not implemented)")
                                } else d
                            }
                        }
                    }
                    
                    val anyLive = _destinations.value.any { it.isLive }
                    if (!anyLive && primaryDest.platform != DestinationPlatform.TELEGRAM) {
                        _isLive.value = false
                        tickerJob?.cancel()
                        _streamDurationSeconds.value = 0L
                        StudioBroadcastService.stopService(getApplication())
                    }
                }
                
                com.example.stream.StreamManager.onConnectionFailed = { reason ->
                    if (primaryDest.platform == DestinationPlatform.TELEGRAM) {
                        TelegramManager.updateLiveStatus(TelegramLiveStatus.ERROR, "RTMP Error: $reason")
                    }
                    _destinations.update { list ->
                        list.map { if (it.id == primaryDest.id) it.copy(isLive = false, connectionStatus = ConnectionStatus.ERROR, connectionMessage = "Network Error: $reason") else it }
                    }
                    _isLive.value = false
                    tickerJob?.cancel()
                    _streamDurationSeconds.value = 0L
                    StudioBroadcastService.stopService(getApplication())
                }
                
                com.example.stream.StreamManager.onDisconnect = {
                    if (primaryDest.platform == DestinationPlatform.TELEGRAM) {
                        TelegramManager.updateLiveStatus(TelegramLiveStatus.IDLE)
                    }
                    if (_isLive.value) toggleLive()
                }
                
                try {
                    com.example.stream.StreamManager.startStream(getApplication(), fullUrl, config.resolution.width, config.resolution.height, config.fps, config.targetBitrateKbps, config.audioBitrateKbps)
                } catch (e: Exception) {
                    com.example.stream.StreamManager.onConnectionFailed?.invoke(e.localizedMessage ?: "Unknown error")
                }
            }

            // Start foreground live broadcast service
            if (_settings.value.backgroundLiveEnabled) {
                val isMicMuted = _audioTracks.value.find { it.id == "mic_aux" }?.isMuted ?: false
                StudioBroadcastService.startService(
                    context = getApplication(),
                    isLive = true,
                    duration = "00:00:00",
                    bitrateKbps = _encoderConfig.value.targetBitrateKbps,
                    destinations = getActiveDestinationsSummary(),
                    isPopupLive = _settings.value.notificationPopupLive,
                    isMuted = isMicMuted
                )
            }
        } else {
            tickerJob?.cancel()
            _streamDurationSeconds.value = 0L
            _destinations.update { list ->
                list.map { it.copy(isLive = false, connectionStatus = ConnectionStatus.IDLE, connectionMessage = null, currentBitrateKbps = 0, latencyMs = 0) }
            }

            // Stop Telegram livestream cleanly if active
            viewModelScope.launch {
                TelegramManager.stopTelegramLive(discardGroupCall = true)
            }

            // Stop active streaming encoder
            com.example.stream.StreamManager.stopStream()

            // Stop foreground live broadcast service
            StudioBroadcastService.stopService(getApplication())

            // If scrolling text is not in permanent running mode, auto turn off when live ends
            if (!_scrollingTextConfig.value.isPermanentRunning) {
                _scrollingTextConfig.update { it.copy(isEnabled = false) }
            }
        }
    }

    fun updateTelegramDestination(url: String, key: String) {
        _destinations.update { list ->
            list.map {
                if (it.platform == DestinationPlatform.TELEGRAM) {
                    it.copy(serverUrl = url, streamKey = key, isEnabled = true)
                } else it
            }
        }
    }

    fun toggleRecording() {
        _isRecording.update { !it }
    }

    fun toggleStudioMode() {
        _isStudioMode.update { !it }
    }

    fun selectScene(scene: StudioScene) {
        if (_isStudioMode.value) {
            _previewScene.value = scene
        } else {
            _programScene.value = scene
            preferences.saveCurrentScene(scene.id)
        }
    }

    fun transitionStudio() {
        val staged = _previewScene.value
        val current = _programScene.value
        _programScene.value = staged
        _previewScene.value = current
        preferences.saveCurrentScene(staged.id)
    }

    fun setTransitionType(type: TransitionType) {
        _transitionType.value = type
    }

    fun setTransitionDuration(ms: Int) {
        _transitionDurationMs.value = ms
    }

    fun toggleSourceVisibility(sourceId: String) {
        val currentSceneId = _programScene.value.id
        _scenes.update { allScenes ->
            allScenes.map { scene ->
                if (scene.id == currentSceneId) {
                    val updatedSources = scene.sources.map { src ->
                        if (src.id == sourceId) src.copy(isVisible = !src.isVisible) else src
                    }
                    scene.copy(sources = updatedSources)
                } else scene
            }
        }
        _programScene.update { scene ->
            val updatedSources = scene.sources.map { src ->
                if (src.id == sourceId) src.copy(isVisible = !src.isVisible) else src
            }
            scene.copy(sources = updatedSources)
        }
    }

    fun toggleSourceLock(sourceId: String) {
        val currentSceneId = _programScene.value.id
        _scenes.update { allScenes ->
            allScenes.map { scene ->
                if (scene.id == currentSceneId) {
                    val updatedSources = scene.sources.map { src ->
                        if (src.id == sourceId) src.copy(isLocked = !src.isLocked) else src
                    }
                    scene.copy(sources = updatedSources)
                } else scene
            }
        }
        _programScene.update { scene ->
            val updatedSources = scene.sources.map { src ->
                if (src.id == sourceId) src.copy(isLocked = !src.isLocked) else src
            }
            scene.copy(sources = updatedSources)
        }
    }

    // Engagement Alert Overlay Trigger
    fun triggerEngagementAlert(
        type: AlertType,
        viewerName: String,
        amountOrDetails: String,
        customMessage: String
    ) {
        val alert = EngagementAlert(
            id = "alert_${System.currentTimeMillis()}",
            type = type,
            viewerName = viewerName,
            amountOrDetails = amountOrDetails,
            customMessage = customMessage
        )
        _activeAlert.value = alert

        alertDismissJob?.cancel()
        alertDismissJob = viewModelScope.launch {
            delay(4500)
            _activeAlert.value = null
        }
    }

    fun dismissAlert() {
        alertDismissJob?.cancel()
        _activeAlert.value = null
    }

    // Destinations Management
    fun toggleDestination(destId: String) {
        _destinations.update { list ->
            list.map {
                if (it.id == destId) {
                    val updated = it.copy(isEnabled = !it.isEnabled)
                    preferences.saveDestination(updated)
                    updated
                } else it
            }
        }
    }

    fun updateDestination(updated: StreamDestination) {
        _destinations.update { list ->
            list.map {
                if (it.id == updated.id) {
                    preferences.saveDestination(updated)
                    updated
                } else it
            }
        }
    }

    fun testDestinationConnection(destId: String) {
        val dest = _destinations.value.find { it.id == destId } ?: return
        if (dest.serverUrl.isBlank() || dest.serverUrl.startsWith("Server url") || dest.serverUrl == "rtmp://") {
            _destinations.update { list ->
                list.map {
                    if (it.id == destId) {
                        it.copy(
                            isTestingConnection = false,
                            connectionStatus = ConnectionStatus.ERROR,
                            connectionMessage = "Please enter a valid Server URL first"
                        )
                    } else it
                }
            }
            return
        }

        _destinations.update { list ->
            list.map {
                if (it.id == destId) {
                    it.copy(
                        isTestingConnection = true,
                        connectionStatus = ConnectionStatus.CONNECTING,
                        connectionMessage = "Testing live network connection to ${dest.serverUrl}..."
                    )
                } else it
            }
        }

        viewModelScope.launch {
            val result = com.example.stream.RtmpConnectionTester.testConnection(dest.serverUrl)
            _destinations.update { list ->
                list.map {
                    if (it.id == destId) {
                        if (result.success) {
                            it.copy(
                                isTestingConnection = false,
                                connectionStatus = ConnectionStatus.CONNECTED,
                                latencyMs = result.latencyMs.toInt(),
                                connectionMessage = result.message
                            )
                        } else {
                            it.copy(
                                isTestingConnection = false,
                                connectionStatus = ConnectionStatus.ERROR,
                                connectionMessage = result.message
                            )
                        }
                    } else it
                }
            }
        }
    }

    // Settings Management
    fun updateSettings(newSettings: StudioSettings) {
        _settings.value = newSettings
        preferences.saveSettings(newSettings)
    }

    // Chroma Key
    fun updateChromaKey(config: ChromaKeyConfig) {
        _chromaKeyConfig.value = config
    }

    // Custom Logo & Watermark
    fun updateCustomLogo(config: CustomLogoConfig) {
        _customLogoConfig.value = config
        preferences.saveCustomLogo(config)
        if (config.isCountdownEnabled) {
            startWatermarkCountdown(config.countdownTotalSeconds, config.countdownNextText)
        } else {
            cancelWatermarkCountdown()
        }
    }

    fun startWatermarkCountdown(seconds: Int, nextText: String) {
        watermarkCountdownJob?.cancel()
        _customLogoConfig.update {
            it.copy(
                isCountdownEnabled = true,
                countdownTotalSeconds = seconds,
                countdownRemainingSeconds = seconds,
                countdownNextText = nextText,
                isCountdownRunning = true
            )
        }
        watermarkCountdownJob = viewModelScope.launch {
            var remaining = seconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _customLogoConfig.update { it.copy(countdownRemainingSeconds = remaining) }
            }
            if (isActive && remaining <= 0) {
                // Countdown reached 0: automatically change watermark text to next name!
                _customLogoConfig.update {
                    it.copy(
                        watermarkText = nextText,
                        isCountdownRunning = false,
                        isCountdownEnabled = false,
                        countdownRemainingSeconds = 0
                    )
                }
                preferences.saveCustomLogo(_customLogoConfig.value)
            }
        }
    }

    fun cancelWatermarkCountdown() {
        watermarkCountdownJob?.cancel()
        _customLogoConfig.update {
            it.copy(isCountdownRunning = false, isCountdownEnabled = false)
        }
    }

    // Scrolling News / Broadcasting Inform Text Ticker
    fun updateScrollingTextConfig(config: ScrollingTextConfig) {
        _scrollingTextConfig.value = config
        preferences.saveScrollingText(config)
    }

    fun toggleScrollingText() {
        _scrollingTextConfig.update { current ->
            val updated = current.copy(isEnabled = !current.isEnabled)
            preferences.saveScrollingText(updated)
            updated
        }
    }

    fun setScrollingTextPermanentRunning(permanent: Boolean) {
        _scrollingTextConfig.update { current ->
            val updated = current.copy(isPermanentRunning = permanent)
            preferences.saveScrollingText(updated)
            updated
        }
    }

    // Encoder Configuration
    fun updateEncoderConfig(config: EncoderConfig) {
        _encoderConfig.value = config
        preferences.saveEncoderConfig(config)
        _telemetry.update {
            it.copy(
                bitrateKbps = if (_isLive.value) config.targetBitrateKbps else 0,
                hardwareEncoderEngaged = config.codec.isHardware
            )
        }
    }

    // Audio Mixer
    fun setAudioVolume(trackId: String, volume: Float) {
        _audioTracks.update { list ->
            list.map { if (it.id == trackId) it.copy(volume = volume) else it }
        }
    }

    fun toggleAudioMute(trackId: String) {
        _audioTracks.update { list ->
            list.map { if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it }
        }
    }

    fun toggleNoiseGate(trackId: String) {
        _audioTracks.update { list ->
            list.map { if (it.id == trackId) it.copy(isNoiseGateActive = !it.isNoiseGateActive) else it }
        }
    }

    // Camera & Media Toggles
    fun switchCamera() {
        _isFrontCamera.update { !it }
    }

    fun toggleTorch() {
        _isTorchOn.update { !it }
    }

    fun setWebCastUrl(url: String) {
        val sanitized = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        _webCastUrl.value = sanitized
        preferences.saveWebCastUrl(sanitized)
    }

    fun setWebCastFullScreen(fullScreen: Boolean) {
        _isWebCastFullScreen.value = fullScreen
    }

    fun toggleMediaPlayback() {
        _mediaIsPlaying.update { !it }
        _mediaCastConfig.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun setMediaCastSource(uri: String?, fileName: String, mediaType: CustomMediaType, mimeType: String?) {
        _mediaCastConfig.value = CustomMediaCastConfig(
            uri = uri,
            fileName = fileName,
            mediaType = mediaType,
            mimeType = mimeType,
            isPlaying = true
        )
        _mediaIsPlaying.value = true
    }

    fun clearMediaCastSource() {
        _mediaCastConfig.value = CustomMediaCastConfig()
        _mediaIsPlaying.value = true
    }

    fun setCustomLogoFile(uri: String?, fileName: String?, mimeType: String?) {
        _customLogoConfig.update {
            it.copy(
                customImageUri = uri,
                customImageName = fileName,
                customImageMimeType = mimeType,
                isEnabled = true
            )
        }
    }

    fun clearCustomLogoFile() {
        _customLogoConfig.update {
            it.copy(
                customImageUri = null,
                customImageName = null,
                customImageMimeType = null
            )
        }
    }

    fun toggleScreenCastGameMode() {
        _screenCastGameMode.update { !it }
    }

    // Restart Studio & Reset Memory to factory defaults
    fun restartStudioResetMemory() {
        if (_isLive.value) {
            toggleLive()
        }
        if (_isRecording.value) {
            _isRecording.value = false
        }
        preferences.clearAllMemory()

        _scenes.value = defaultScenes
        _programScene.value = defaultScenes[0]
        _previewScene.value = defaultScenes[1]
        _destinations.value = defaultDestinations
        _encoderConfig.value = EncoderConfig()
        _chromaKeyConfig.value = ChromaKeyConfig()
        cancelWatermarkCountdown()
        _customLogoConfig.value = CustomLogoConfig()
        _scrollingTextConfig.value = ScrollingTextConfig()
        _settings.value = StudioSettings()
        _webCastUrl.value = "https://en.wikipedia.org/wiki/Open_Broadcaster_Software"
        _mediaCastConfig.value = CustomMediaCastConfig()
        _audioTracks.value = listOf(
            AudioTrack("mic_aux", "Mic / Audio In", volume = 1.0f, isMuted = false, peakDb = -14f),
            AudioTrack("desktop_sys", "Desktop / Screen Audio", volume = 0.85f, isMuted = false, peakDb = -18f),
            AudioTrack("media_player", "Media / Video Source", volume = 0.70f, isMuted = false, peakDb = -22f),
            AudioTrack("alert_sfx", "Viewer Alert SFX", volume = 0.90f, isMuted = false, peakDb = -10f)
        )
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        alertDismissJob?.cancel()
        watermarkCountdownJob?.cancel()
    }
}
