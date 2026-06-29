package com.example.admerge.domain.model

/**
 * 激励视频回调结果。
 */
data class RewardResult(
    val shown: Boolean,
    val finished: Boolean,
    val rewardGranted: Boolean,
    val errorMessage: String? = null,
)
