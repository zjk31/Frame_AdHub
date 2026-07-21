package cn.manxinghai.zhuimange.core.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 通用 SSE (Server-Sent Events) 客户端。
 *
 * 无业务依赖——接收 URL + Header Provider + Event Callback 即可工作。
 * 子 App 在 [onEvent] 中根据 [SseEvent.eventType] 路由业务逻辑。
 *
 * # 生命周期
 * ```
 * val client = SseClient(url, headerProvider, onEvent)
 * client.connect()      // 启动
 * client.disconnect()   // 停止（退出登录/不再需要时）
 * ```
 *
 * # 特性
 * - StateFlow<SseConnectionState> 暴露连接状态（UI 可观察）
 * - 指数退避自动重连（5s → 10s → … → 60s 封顶）
 * - 内部使用 SupervisorJob，单事件异常不影响连接
 */
class SseClient(
    private val sseUrl: String,
    private val headerProvider: () -> Map<String, String>,
    private val onEvent: suspend (SseEvent) -> Unit,
) {
    companion object {
        private const val TAG = "SseClient"
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
    }

    // ── 连接状态 ──

    private val _connectionState = MutableStateFlow(SseConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SseConnectionState> = _connectionState.asStateFlow()

    // ── 内部状态 ──

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectJob: Job? = null
    private var reconnectAttempt = 0

    /** SSE 专用 OkHttp —— readTimeout=0（无限等待） */
    private val sseHttpClient: OkHttpClient by lazy {
        AppNetworkClient.buildClient(readTimeoutSec = 0)
    }

    // ── 公开 API ──

    /** 启动连接（幂等：已连接/连接中则跳过） */
    fun connect() {
        if (connectJob?.isActive == true) return
        connectJob = scope.launch { doConnect() }
    }

    /** 断开连接并停止重连 */
    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        reconnectAttempt = 0
        _connectionState.value = SseConnectionState.DISCONNECTED
    }

    // ═══════════════════════════════════════════════════════
    //  连接实现
    // ═══════════════════════════════════════════════════════

    private suspend fun doConnect() {
        _connectionState.value = SseConnectionState.CONNECTING
        try {
            val headers = headerProvider()
            val request = Request.Builder()
                .url(sseUrl)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()

            val response = sseHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "SSE connect failed: HTTP ${response.code}")
                _connectionState.value = SseConnectionState.FAILED
                scheduleReconnect()
                return
            }

            _connectionState.value = SseConnectionState.CONNECTED
            reconnectAttempt = 0
            Log.i(TAG, "SSE connected: $sseUrl")

            val source = response.body?.source()
            if (source != null) {
                readEventStream(source)
            }

            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "SSE connection error", e)
            _connectionState.value = SseConnectionState.FAILED
            scheduleReconnect()
        }
    }

    // ═══════════════════════════════════════════════════════
    //  事件流解析
    // ═══════════════════════════════════════════════════════

    private var currentEventType: String? = null

    private suspend fun readEventStream(source: okio.BufferedSource) {
        try {
            val input = source.inputStream()
            val buf = StringBuilder()

            while (scope.isActive) {
                val b = input.read()
                if (b == -1) break

                val c = b.toChar()
                if (c == '\n') {
                    val line = buf.toString().trimEnd()
                    buf.clear()

                    when {
                        line.startsWith("event:") ->
                            currentEventType = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            val data = line.removePrefix("data:").trim()
                            if (data.isNotBlank() && data != "[DONE]") {
                                onEvent(SseEvent(eventType = currentEventType, data = data))
                            }
                        }
                        line.isNotEmpty() ->
                            Log.d(TAG, "SSE comment: $line")
                    }
                } else if (c != '\r') {
                    buf.append(c)
                }
            }
            Log.w(TAG, "SSE stream ended")
        } catch (e: IOException) {
            Log.w(TAG, "SSE stream read error", e)
        } finally {
            _connectionState.value = SseConnectionState.DISCONNECTED
            scheduleReconnect()
        }
    }

    // ═══════════════════════════════════════════════════════
    //  重连
    // ═══════════════════════════════════════════════════════

    private fun scheduleReconnect() {
        scope.launch {
            reconnectAttempt++
            val delayMs = (RECONNECT_DELAY_MS * reconnectAttempt)
                .coerceAtMost(MAX_RECONNECT_DELAY_MS)
            Log.i(TAG, "SSE reconnect in ${delayMs}ms (attempt $reconnectAttempt)")
            delay(delayMs)
            if (_connectionState.value != SseConnectionState.CONNECTED && scope.isActive) {
                doConnect()
            }
        }
    }
}
