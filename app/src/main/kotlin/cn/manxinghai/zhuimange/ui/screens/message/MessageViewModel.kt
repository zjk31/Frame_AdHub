package cn.manxinghai.zhuimange.ui.screens.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.data.sse.MessageRepository
import cn.manxinghai.zhuimange.data.sse.SseConnectionState
import cn.manxinghai.zhuimange.domain.model.SystemMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageUiState(
    val isLoading: Boolean = true,
    val messages: List<SystemMessage> = emptyList(),
    val unreadCount: Int = 0,
    val connectionState: SseConnectionState = SseConnectionState.DISCONNECTED
)

class MessageViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MessageUiState())
    val ui: StateFlow<MessageUiState> = _ui.asStateFlow()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            messageRepository.messages.collect { msgs ->
                _ui.update {
                    it.copy(
                        messages = msgs.sortedByDescending { m -> m.createTime },
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            messageRepository.unreadCount.collect { count ->
                _ui.update { it.copy(unreadCount = count) }
            }
        }

        viewModelScope.launch {
            messageRepository.connectionState.collect { state ->
                _ui.update { it.copy(connectionState = state) }
            }
        }
    }

    fun markAsRead(messageId: String) {
        messageRepository.ackMessages(listOf(messageId))
    }

    fun markAllRead() {
        messageRepository.markAllRead()
    }

    fun reconnect() {
        messageRepository.reconnect()
    }
}
