package cn.manxinghai.zhuimange.core.network

/**
 * SSE 连接状态。
 */
enum class SseConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
}
