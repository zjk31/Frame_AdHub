package cn.android.adhub.data.sse

import cn.android.adhub.data.repository.UserSession
import cn.android.adhub.domain.model.SystemMessage
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 消息中心 Repository
 * 管理消息的获取、已读状态、未读数
 */
class MessageRepository(
    private val apiRepository: ManAppRepository,
    private val userSession: UserSession,
    private val baseUrl: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sseClient = SseClient(
        baseUrl = baseUrl,
        userSession = userSession,
        onMessageReceived = { msg ->
            _messages.update { it + msg }
        },
        onUnreadCountChanged = { count ->
            _unreadCount.value = count
        }
    )

    private val _messages = MutableStateFlow<List<SystemMessage>>(emptyList())
    val messages: StateFlow<List<SystemMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    val connectionState = sseClient.connectionState

    /**
     * 初始化：拉取离线消息 + 启动 SSE
     */
    fun init() {
        scope.launch {
            // 拉取离线消息
            apiRepository.getOfflineMessages().onSuccess { msgs ->
                _messages.value = msgs
            }

            // 拉取未读数
            apiRepository.getUnreadCount().onSuccess { count ->
                _unreadCount.value = count
            }

            // 启动 SSE 连接
            sseClient.connect()
        }
    }

    /**
     * 确认消息已读
     */
    fun ackMessages(messageIds: List<String>) {
        scope.launch {
            apiRepository.ackMessages(messageIds).onSuccess {
                sseClient.markAsRead(messageIds)
                _messages.update { msgs ->
                    msgs.map { it.copy() } // 触发更新
                }
            }
        }
    }

    /**
     * 全部已读
     */
    fun markAllRead() {
        scope.launch {
            val allIds = _messages.value.map { it.id }
            if (allIds.isNotEmpty()) {
                apiRepository.ackMessages(allIds).onSuccess {
                    sseClient.markAsRead(allIds)
                    _unreadCount.value = 0
                }
            }
        }
    }

    /**
     * 清空消息列表
     */
    fun clearMessages() {
        sseClient.clearMessages()
        _messages.value = emptyList()
    }

    /**
     * 断开连接（用户退出登录时）
     */
    fun disconnect() {
        sseClient.disconnect()
    }

    /**
     * 重新连接
     */
    fun reconnect() {
        sseClient.connect()
    }
}
