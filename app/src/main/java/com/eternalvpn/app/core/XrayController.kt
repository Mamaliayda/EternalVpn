package com.eternalvpn.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.concurrent.atomic.AtomicReference

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * Wraps AndroidLibXrayLite (libv2ray) CoreController.
 * API verified against 2dust/AndroidLibXrayLite v26.11.9 source.
 */
object XrayController {

    private const val TAG = "XrayController"
    private const val MAX_LOG_LINES = 200

    private val _state = MutableStateFlow(VpnState.DISCONNECTED)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val controllerRef = AtomicReference<CoreController?>(null)
    @Volatile private var initialized = false

    private val callback = object : CoreCallbackHandler {
        override fun startup(): Long {
            appendLog("Core callback: startup")
            _state.value = VpnState.CONNECTED
            _lastError.value = null
            return 0L
        }

        override fun shutdown(): Long {
            appendLog("Core callback: shutdown")
            if (_state.value != VpnState.ERROR) {
                _state.value = VpnState.DISCONNECTED
            }
            return 0L
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            val msg = s ?: ""
            appendLog("Core status[$l]: $msg")
            if (l < 0) {
                _lastError.value = msg
                _state.value = VpnState.ERROR
            }
            return 0L
        }
    }

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        val baseDir = File(context.filesDir, "xray").apply { mkdirs() }
        // libv2ray expects asset/cert dir; copy geo assets if present for Xray to read.
        try {
            Libv2ray.initCoreEnv(baseDir.absolutePath, "")
            appendLog("initCoreEnv: ${baseDir.absolutePath}")
        } catch (t: Throwable) {
            appendLog("initCoreEnv failed: ${t.message}")
            Log.e(TAG, "initCoreEnv failed", t)
        }
        controllerRef.compareAndSet(null, Libv2ray.newCoreController(callback))
        initialized = true
    }

    @Synchronized
    fun start(configJson: String, tunFd: Int): Result<Unit> {
        val controller = controllerRef.get() ?: return Result.failure(IllegalStateException("Controller not initialized"))
        _state.value = VpnState.CONNECTING
        _lastError.value = null
        appendLog("Starting core (tunFd=$tunFd)...")
        return runCatching {
            controller.startLoop(configJson, tunFd)
            // startLoop may return before callback in some builds; reflect actual state if available.
            if (controller.isRunning) {
                _state.value = VpnState.CONNECTED
            }
            appendLog("startLoop returned, isRunning=${controller.isRunning}")
        }.onFailure { t ->
            _lastError.value = t.message ?: t.toString()
            _state.value = VpnState.ERROR
            appendLog("startLoop failed: ${t.message}")
            Log.e(TAG, "startLoop failed", t)
        }
    }

    @Synchronized
    fun stop() {
        val controller = controllerRef.get() ?: return
        appendLog("Stopping core...")
        runCatching { controller.stopLoop() }
            .onSuccess { appendLog("stopLoop returned") }
            .onFailure { t ->
                appendLog("stopLoop error: ${t.message}")
                Log.w(TAG, "stopLoop error", t)
            }
        _state.value = VpnState.DISCONNECTED
    }

    fun isRunning(): Boolean = runCatching { controllerRef.get()?.isRunning == true }.getOrDefault(false)

    fun queryStats(): String = runCatching { controllerRef.get()?.queryAllOutboundTrafficStats().orEmpty() }
        .getOrDefault("")

    fun measureDelay(url: String = "https://www.gstatic.com/generate_204"): Long =
        runCatching { controllerRef.get()?.measureDelay(url) ?: -1L }.getOrDefault(-1L)

    fun appendLog(line: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$ts] $line"
        Log.i(TAG, line)
        _logs.update { (it + entry).takeLast(MAX_LOG_LINES) }
    }

    fun clearError() { _lastError.value = null }
}
