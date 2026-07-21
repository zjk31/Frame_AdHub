package cn.manxinghai.zhuimange.core.update

/**
 * AppUpdateManager 状态机。
 */
sealed class PatchState {
    /** 空闲 */
    data object Idle : PatchState()

    /** 下载中（差量或整包） */
    data object Downloading : PatchState()

    /** 差量合成中 */
    data object Patching : PatchState()

    /** 就绪，可安装 */
    data class ReadyToInstall(val apkPath: String) : PatchState()

    /** 出错 */
    data class Error(val message: String) : PatchState()
}
