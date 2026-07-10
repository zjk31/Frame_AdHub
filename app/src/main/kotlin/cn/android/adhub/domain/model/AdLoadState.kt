package cn.android.adhub.domain.model

/**
 * 广告加载状态。UI 层根据此 sealed class 决定展示 Loading / 广告 / 空白。
 */
sealed class AdLoadState<out T> {
    data object Idle : AdLoadState<Nothing>()
    data object Loading : AdLoadState<Nothing>()
    data class Loaded<T>(val ad: T) : AdLoadState<T>()
    data class Error(val message: String?) : AdLoadState<Nothing>()
}
