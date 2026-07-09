package com.example.adhub.data.remote.dto

/**
 * 通用 API 响应包装，复用后端统一的 { code, data, message } 信封格式。
 *
 * 后端约定 code == 0 为成功，非 0 为业务错误。
 */
data class ApiResponse<T>(
    val code: Int,
    val data: T?,
    val message: String?,
) {
    val isSuccess: Boolean get() = code == 0
}
