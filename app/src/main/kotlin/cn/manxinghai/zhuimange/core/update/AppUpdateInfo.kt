package cn.manxinghai.zhuimange.core.update

/**
 * 应用更新信息 — 子 App 从自己的后端获取后传入 [AppUpdateManager.startUpdate]。
 *
 * 差量更新：服务端返回 [patchUrl] + [fromVersion]，且当前版本匹配时才走差量。
 * 整包更新：差量不可用或失败时回退到 [downloadUrl] 全量下载。
 *
 * @param version 新版本号（如 "1.2.0"）
 * @param downloadUrl 整包下载地址（必填）
 * @param patchUrl 差量补丁地址（可选，需搭配 fromVersion）
 * @param fromVersion 差量补丁的基准版本（可选）
 * @param apkMd5 整包 MD5（可选，有则校验）
 * @param patchMd5 补丁 MD5（可选，有则校验）
 * @param patchSize 补丁期望大小（可选，有则校验）
 * @param updateLog 更新日志（纯展示）
 * @param forceUpdate 是否强制更新
 */
data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val patchUrl: String? = null,
    val fromVersion: String? = null,
    val apkMd5: String? = null,
    val patchMd5: String? = null,
    val patchSize: Long? = null,
    val updateLog: String? = null,
    val forceUpdate: Boolean = false,
)
