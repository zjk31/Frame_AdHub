package cn.manxinghai.zhuimange.core.network

/**
 * 通用 SSE 事件。
 *
 * [eventType] 来自 SSE 协议的 `event:` 字段（可为 null）
 * [data] 来自 `data:` 字段（非空）
 *
 * 子 App 自行解析 [data] 的 JSON 并路由不同的 [eventType]。
 */
data class SseEvent(
    val eventType: String?,
    val data: String,
)
