package cn.android.adhub.domain.model

/**
 * 激励视频回调结果。
 *
 * 对齐 flutter_merge 四家 SDK 的回调语义：
 * - [shown]: 广告是否成功曝光
 * - [finished]: 广告流程是否结束（用户关闭 / 加载失败 / 展示失败）
 * - [rewardGranted]: 激励是否发放（onReward / onRewardArrived 回调）
 * - [rewardType]: 激励场景标识（"reward" / "task" / "pure" / "download"）
 * - [extra]: SDK 附带的额外信息（ECPM、rewardAmount 等）
 * - [errorMessage]: 失败原因
 */
data class RewardResult(
    val shown: Boolean = false,
    val finished: Boolean = false,
    val rewardGranted: Boolean = false,
    val rewardType: String = "",
    val extra: Map<String, Any?> = emptyMap(),
    val errorMessage: String? = null,
)
