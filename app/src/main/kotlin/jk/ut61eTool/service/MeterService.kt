package jk.ut61eTool.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jake.ut61e.Ut61eFrameDecoder
import jk.ut61eTool.MainActivity
import jk.ut61eTool.R
import jk.ut61eTool.appContainer
import jk.ut61eTool.ble.FrameAssembler
import jk.ut61eTool.ble.MeterConnection
import jk.ut61eTool.ble.MeterConnectionEvent
import jk.ut61eTool.data.ConnectionState
import jk.ut61eTool.data.DeviceInfo
import jk.ut61eTool.data.MeterCommander
import jk.ut61eTool.data.Settings
import jk.ut61eTool.domain.DisplayReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Owns the BLE link to the meter and the whole sample pipeline for as long as
 * the user wants data, independent of which screen is visible.
 *
 * Notification channels: the legacy app had a "log" channel and a "connection"
 * style ongoing notification; they are merged into the single ongoing
 * "connection" notification (which shows the logged line count), leaving only
 * "connection" (LOW) and "alarm" (HIGH).
 */
class MeterService : LifecycleService() {

    // Resolved lazily so the container is fully wired before first access, and
    // so this file needs no compile-time knowledge of the container's types.
    private val repository by lazy { appContainer.meterRepository }
    private val settingsRepository by lazy { appContainer.settingsRepository }
    private val csvLogger by lazy { appContainer.csvLogger }
    private val converter by lazy { appContainer.converter }
    private val alarmEvaluator by lazy { appContainer.alarmEvaluator }

    private val connection by lazy { MeterConnection(this) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    /** Outlives [onDestroy] so the log file can still be closed cleanly. */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val assembler = FrameAssembler()

    private var connectionJob: Job? = null
    private var device: DeviceInfo? = null
    private var settings = Settings()
    private var connectionState = ConnectionState.DISCONNECTED
    private var alarmActive = false
    private var foregroundStarted = false
    private var linkLostNotified = false
    private var lastNotificationUpdate = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        lifecycleScope.launch {
            settingsRepository.settings.collect { settings = it }
        }
        lifecycleScope.launch {
            csvLogger.state.collect { state ->
                repository.publishLoggingState(state)
                updateNotification(throttled = true)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Every start arrives via startForegroundService, so the notification has
        // to go up before anything else can fail.
        ensureForeground()

        when (intent?.action) {
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS)
                if (address.isNullOrBlank()) {
                    stopIfIdle()
                } else {
                    startConnection(DeviceInfo(address, intent.getStringExtra(EXTRA_NAME)))
                }
            }

            ACTION_DISCONNECT -> stopConnection()

            ACTION_START_LOG -> startLogging(intent.getStringExtra(EXTRA_FILE_NAME).orEmpty())

            ACTION_STOP_LOG -> lifecycleScope.launch { csvLogger.stop() }

            else -> stopIfIdle()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        connectionJob?.cancel()
        connectionJob = null
        cleanupScope.launch { csvLogger.stop() }
        notificationManager?.cancel(ALARM_NOTIFICATION_ID)
        super.onDestroy()
    }

    // ---- connection -------------------------------------------------------

    private fun startConnection(target: DeviceInfo) {
        connectionJob?.cancel()
        device = target
        assembler.reset()
        alarmEvaluator.reset()
        alarmActive = false
        clearLinkLostNotification()
        repository.clearSession()
        publishState(ConnectionState.CONNECTING)

        connectionJob = lifecycleScope.launch {
            val current = settingsRepository.settings.first()
            settings = current
            val uuid = runCatching { UUID.fromString(current.characteristicUuid) }
                .getOrElse { UUID.fromString(Settings().characteristicUuid) }

            keepConnected(target, uuid)
        }
    }

    /**
     * Holds the link up until the user disconnects or the meter stays away.
     *
     * The meter drops the link by itself — switching range is enough to do it — and
     * without this the session was over: the user had to disconnect, scan and connect
     * again to get their reading back. Each attempt is a fresh [MeterConnection] flow,
     * since a closed GATT client cannot be reused.
     */
    private suspend fun keepConnected(target: DeviceInfo, uuid: UUID) {
        var failures = 0

        while (currentCoroutineContext().isActive) {
            assembler.reset()
            connection.connect(target.address, uuid)
                // A failed attempt is just an attempt that ends early.
                .catch { }
                .collect { event ->
                    // Any traffic means the meter is there; a later drop starts its
                    // own backoff instead of inheriting an old one.
                    if (event is MeterConnectionEvent.Data) {
                        failures = 0
                        clearLinkLostNotification()
                    }
                    handleEvent(event)
                }

            // The flow only completes once the link is gone, one way or another.
            if (++failures > MAX_RECONNECT_ATTEMPTS) break
            publishState(ConnectionState.CONNECTING)
            if (failures >= LINK_LOST_WARNING_ATTEMPTS) notifyLinkLost(retrying = true)
            delay(reconnectDelayMs(failures))
        }

        notifyLinkLost(retrying = false)
        onDisconnected()
    }

    /**
     * Says out loud that the meter is gone.
     *
     * Silent reconnection is right for the range switch it was built for, but a meter
     * that stays away is a measurement the user is no longer taking — and they may
     * well have left it logging and walked off. This is the same high-importance
     * channel as the measurement alarm, so it reaches a pocketed phone.
     */
    private fun notifyLinkLost(retrying: Boolean) {
        if (retrying && linkLostNotified) return
        linkLostNotified = true

        val what = getString(
            if (retrying) R.string.ble_link_lost_retrying else R.string.ble_link_lost_gave_up,
        )
        val logging = csvLogger.state.value
        val text = if (logging.running) {
            getString(R.string.ble_link_lost_logging, what, logging.lineCount)
        } else {
            what
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_meter_notification)
            .setContentTitle(getString(R.string.ble_link_lost_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent())
            .setOngoing(retrying)
            .setAutoCancel(!retrying)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager?.notify(LINK_LOST_NOTIFICATION_ID, notification)

        // Only on the way in: a phone buzzing again when the retries run out would
        // say nothing the first buzz did not.
        if (retrying && settings.vibration) vibrate()
    }

    private fun clearLinkLostNotification() {
        if (!linkLostNotified) return
        linkLostNotified = false
        notificationManager?.cancel(LINK_LOST_NOTIFICATION_ID)
    }

    private suspend fun handleEvent(event: MeterConnectionEvent) {
        when (event) {
            MeterConnectionEvent.Connecting -> publishState(ConnectionState.CONNECTING)
            MeterConnectionEvent.Connected -> publishState(ConnectionState.CONNECTED)
            MeterConnectionEvent.ServicesReady -> updateNotification(throttled = false)
            is MeterConnectionEvent.Data -> onData(event.bytes)
            // The link is down; keepConnected decides whether to try again.
            is MeterConnectionEvent.Disconnected -> Unit
        }
    }

    /** Explicit user disconnect: the service has nothing left to do. */
    private fun stopConnection() {
        connectionJob?.cancel()
        clearLinkLostNotification()
        connectionJob = null
        publishState(ConnectionState.DISCONNECTED)
        stopSelf()
    }

    /** The meter is gone for good: either it never came back, or the flow failed. */
    private fun onDisconnected() {
        connectionJob = null
        publishState(ConnectionState.DISCONNECTED)
        if (alarmActive) {
            alarmActive = false
            repository.publishAlarm(false)
        }
        // A dropped link while logging keeps the service (and the open log file)
        // alive so the user can reconnect and continue the same file.
        if (!csvLogger.state.value.running) {
            stopSelf()
        } else {
            updateNotification(throttled = false)
        }
    }

    // ---- sample pipeline --------------------------------------------------

    /**
     * Pipeline order is the legacy one: decode -> convert -> alarm (on the
     * converted value, evaluated even for ignored samples) -> graph/log.
     */
    private suspend fun onData(bytes: ByteArray) {
        for (frame in assembler.feed(bytes)) {
            val reading = Ut61eFrameDecoder.decode(frame).getOrNull() ?: continue
            val current = settings
            val display = converter.convert(reading, current)

            evaluateAlarm(display, current)

            if (converter.isIgnored(reading, current)) {
                repository.publishReading(display, current.viewportSize, includeInWindow = false)
                continue
            }

            repository.publishReading(display, current.viewportSize)
            if (csvLogger.state.value.running) {
                csvLogger.log(display.toCsvLine())
            }
        }
    }

    private fun evaluateAlarm(reading: DisplayReading, current: Settings) {
        val triggered = alarmEvaluator.evaluate(reading.value, current)
        if (triggered && !alarmActive) {
            alarmActive = true
            repository.publishAlarm(true)
            showAlarmNotification(reading, current)
        } else if (!triggered && alarmActive) {
            alarmActive = false
            repository.publishAlarm(false)
        }
    }

    // ---- logging ----------------------------------------------------------

    private fun startLogging(fileName: String) {
        lifecycleScope.launch {
            val current = settingsRepository.settings.first()
            settings = current
            csvLogger.start(current.logFolderUri, fileName, !current.noLogfileReuse)
            updateNotification(throttled = false)
        }
    }

    // ---- notifications ----------------------------------------------------

    private fun createNotificationChannels() {
        val manager = notificationManager ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTION,
                getString(R.string.ble_channel_connection),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.ble_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // Vibration is a user setting, so it is triggered explicitly
                // instead of being baked into the channel.
                enableVibration(false)
            },
        )
    }

    private fun ensureForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildConnectionNotification(),
            FOREGROUND_TYPES,
        )
        foregroundStarted = true
        lastNotificationUpdate = SystemClock.elapsedRealtime()
    }

    private fun updateNotification(throttled: Boolean) {
        if (!foregroundStarted) return
        val now = SystemClock.elapsedRealtime()
        if (throttled && now - lastNotificationUpdate < NOTIFICATION_THROTTLE_MS) return
        lastNotificationUpdate = now
        notificationManager?.notify(NOTIFICATION_ID, buildConnectionNotification())
    }

    private fun buildConnectionNotification(): Notification {
        val logging = csvLogger.state.value
        val stateText = when (connectionState) {
            ConnectionState.CONNECTING -> getString(R.string.ble_state_connecting)
            ConnectionState.CONNECTED -> getString(R.string.ble_state_connected)
            ConnectionState.DISCONNECTED -> getString(R.string.ble_state_disconnected)
        }
        val text = if (logging.running) {
            getString(R.string.ble_notification_logging, stateText, logging.lineCount)
        } else {
            stateText
        }
        return NotificationCompat.Builder(this, CHANNEL_CONNECTION)
            .setSmallIcon(R.drawable.ic_meter_notification)
            .setContentTitle(
                device?.name?.takeIf { it.isNotBlank() }
                    ?: device?.address
                    ?: getString(R.string.ble_notification_title),
            )
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showAlarmNotification(reading: DisplayReading, current: Settings) {
        val value = "%.4f %s".format(Locale.US, reading.value, reading.unit)
        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_meter_notification)
            .setContentTitle(getString(R.string.ble_alarm_title))
            .setContentText(getString(R.string.ble_alarm_text, value))
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager?.notify(ALARM_NOTIFICATION_ID, notification)
        if (current.vibration) vibrate()
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
        } else {
            getSystemService(Vibrator::class.java)?.vibrate(effect)
        }
    }

    // ---- misc -------------------------------------------------------------

    private fun publishState(state: ConnectionState) {
        connectionState = state
        repository.publishConnectionState(state, device)
        updateNotification(throttled = false)
    }

    private fun stopIfIdle() {
        if (connectionJob?.isActive != true && !csvLogger.state.value.running) stopSelf()
    }

    companion object {
        const val ACTION_CONNECT = "jk.ut61eTool.action.CONNECT"
        const val ACTION_DISCONNECT = "jk.ut61eTool.action.DISCONNECT"
        const val ACTION_START_LOG = "jk.ut61eTool.action.START_LOG"
        const val ACTION_STOP_LOG = "jk.ut61eTool.action.STOP_LOG"

        const val EXTRA_ADDRESS = "jk.ut61eTool.extra.ADDRESS"
        const val EXTRA_NAME = "jk.ut61eTool.extra.NAME"
        const val EXTRA_FILE_NAME = "jk.ut61eTool.extra.FILE_NAME"

        const val CHANNEL_CONNECTION = "connection"
        const val CHANNEL_ALARM = "alarm"

        private const val NOTIFICATION_ID = 42
        private const val ALARM_NOTIFICATION_ID = 43
        private const val LINK_LOST_NOTIFICATION_ID = 44
        private const val NOTIFICATION_THROTTLE_MS = 1_000L

        private val VIBRATION_PATTERN = longArrayOf(0, 500, 500)

        /**
         * The whole job is keeping the meter's BLE link open; logging what it sends to
         * a local file is part of that session, not a sync. See the manifest.
         */
        private const val FOREGROUND_TYPES =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    }
}

/**
 * [MeterCommander] implementation the UI talks to; every command is a start
 * intent, so the service keeps running while no screen is bound.
 *
 * Install once in `AppContainer`:
 * `meterRepository.commander = MeterServiceCommander(context)`.
 */
class MeterServiceCommander(context: Context) : MeterCommander {

    private val appContext = context.applicationContext

    override fun connect(device: DeviceInfo) = send(MeterService.ACTION_CONNECT) {
        putExtra(MeterService.EXTRA_ADDRESS, device.address)
        putExtra(MeterService.EXTRA_NAME, device.name)
    }

    override fun disconnect() = send(MeterService.ACTION_DISCONNECT)

    override fun startLogging(fileName: String) = send(MeterService.ACTION_START_LOG) {
        putExtra(MeterService.EXTRA_FILE_NAME, fileName)
    }

    override fun stopLogging() = send(MeterService.ACTION_STOP_LOG)

    private fun send(action: String, extras: Intent.() -> Unit = {}) {
        val intent = Intent(appContext, MeterService::class.java)
            .setAction(action)
            .apply(extras)
        ContextCompat.startForegroundService(appContext, intent)
    }
}

/**
 * Consecutive failed attempts before a session is given up. The count resets on every
 * sample received, so this is "the meter has been away for about a minute", not "the
 * link has dropped ten times".
 */
internal const val MAX_RECONNECT_ATTEMPTS = 10

/**
 * Consecutive failed attempts before the user is told the meter is gone. Low enough
 * to reach them while it could still be nudged back, high enough that the range
 * switch this all exists for never raises it.
 */
internal const val LINK_LOST_WARNING_ATTEMPTS = 4

/**
 * Backoff before the [failures]-th consecutive retry: fast enough that a range switch
 * is barely visible, slow enough not to hammer the radio while the meter is off.
 */
internal fun reconnectDelayMs(failures: Int): Long =
    (FIRST_RECONNECT_DELAY_MS shl (failures - 1).coerceIn(0, RECONNECT_BACKOFF_STEPS))
        .coerceAtMost(MAX_RECONNECT_DELAY_MS)

private const val FIRST_RECONNECT_DELAY_MS = 500L
private const val MAX_RECONNECT_DELAY_MS = 8_000L
private const val RECONNECT_BACKOFF_STEPS = 4
