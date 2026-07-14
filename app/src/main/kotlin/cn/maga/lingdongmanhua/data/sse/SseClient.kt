package cn.maga.lingdongmanhua.data.sse

import android.util.Log
import cn.maga.lingdongmanhua.data.dto.SseMessageDto
import cn.maga.lingdongmanhua.data.repository.UserSession
import cn.maga.lingdongmanhua.domain.model.SystemMessage
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource

/**
 * SSE 客户端 — 持久连接 + 离线消息
 * 后端 EventSource: api/sse/connect
 */
class SseClient(
    private val baseUrl: String,
    private val userSession: UserSession,
    private val onMessageReceived: suspend (SystemMessage) -> Unit,
    private val onUnreadCountChanged: suspend (Int) -> Unit
) {
    companion object {
        private const val TAG = "SseClient"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_DELAY_MS = 60000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS) // SSE 长连接
        .build()

    private val gson = Gson()

    private val _connectionState = MutableStateFlow(SseConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SseConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<SystemMessage>>(emptyList())
    val messages: StateFlow<List<SystemMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var reconnectAttempt = 0

    /**
     * 启动 SSE 连接
     */
    fun connect() {
        if (connectJob?.isActive == true) return

        connectJob = scope.launch {
            _connectionState.value = SseConnectionState.CONNECTING
            try {
                val token = userSession.getToken()
                if (token.isNullOrBlank()) {
                    _connectionState.value = SseConnectionState.DISCONNECTED
                    return@launch
                }

                val request = Request.Builder()
                    .url("$baseUrl/api/sse/connect")
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .header("access-token", token)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "SSE connect failed: ${response.code}")
                    _connectionState.value = SseConnectionState.FAILED
                    scheduleReconnect()
                    return@launch
                }

                _connectionState.value = SseConnectionState.CONNECTED
                reconnectAttempt = 0

                // 读取流
                val source = response.body?.source()
                if (source != null) {
                    readEventStream(source)
                }

                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "SSE connection error", e)
                _connectionState.value = SseConnectionState.FAILED
                scheduleReconnect()
            }
        }

        // 同时拉取离线消息
        scope.launch { fetchOfflineMessages() }
    }

    /**
     * 读取 SSE 事件流
     */
    private suspend fun readEventStream(source: BufferedSource) {
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data.isNotBlank() && data != "[DONE]") {
                        parseSseEvent(data)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SSE stream ended, will reconnect", e)
            _connectionState.value = SseConnectionState.DISCONNECTED
            scheduleReconnect()
        }
    }

    /**
     * 解析 SSE 事件
     */
    private suspend fun parseSseEvent(data: String) {
        try {
            val json = JsonParser.parseString(data).asJsonObject
            val type = json.get("type")?.asString ?: "message"

            when (type) {
                "message", "notification" -> {
                    val msg = gson.fromJson(data, SseMessageDto::class.java)
                    val systemMsg = SystemMessage(
                        id = msg.id ?: java.util.UUID.randomUUID().toString(),
                        title = msg.title ?: "",
                        content = msg.content ?: "",
                        type = msg.type ?: type,
                        createTime = msg.createTime ?: System.currentTimeMillis()
                    )
                    _messages.update { it + systemMsg }
                    _unreadCount.update { it + 1 }
                    onMessageReceived(systemMsg)
                }
                "unread_count" -> {
                    val count = json.get("count")?.asInt ?: 0
                    _unreadCount.value = count
                    onUnreadCountChanged(count)
                }
                "heartbeat" -> { /* 忽略心跳 */ }
                else -> Log.d(TAG, "Unknown SSE type: $type")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SSE event: $data", e)
        }
    }

    /**
     * 拉取离线消息
     */
    private suspend fun fetchOfflineMessages() {
        // 这里需要通过 Repository 调用 API
        // 实际由 MessageRepository 处理
    }

    /**
     * 重连
     */
    private fun scheduleReconnect() {
        scope.launch {
            reconnectAttempt++
            val delay = (RECONNECT_DELAY_MS * reconnectAttempt).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            kotlinx.coroutines.delay(delay)
            if (_connectionState.value != SseConnectionState.CONNECTED) {
                connect()
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        _connectionState.value = SseConnectionState.DISCONNECTED
    }

    /**
     * 标记已读
     */
    fun markAsRead(messageIds: List<String>) {
        scope.launch {
            _unreadCount.update { maxOf(0, it - messageIds.size) }
        }
    }

    /**
     * 清空消息
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _unreadCount.value = 0
    }
}

enum class SseConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}
