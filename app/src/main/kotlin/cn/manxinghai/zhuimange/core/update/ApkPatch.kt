package cn.manxinghai.zhuimange.core.update

/**
 * HDiffPatch 原生桥接。
 *
 * **需要 libapkpatch.so**（HDiffPatch C++ 编译产物）。
 * 未集成时 [nativeApplyPatch] 抛出 [UnsatisfiedLinkError]，
 * [AppUpdateManager] 会自动回退整包下载。
 *
 * 集成步骤：
 *   1. 将 HDiffPatch 源码放入 `app/src/main/cpp/libHDiffPatch/`
 *   2. CMakeLists.txt 编译目标 `apkpatch`
 *   3. build.gradle.kts 添加 `externalNativeBuild { cmake { ... } }`
 */
object ApkPatch {

    init {
        try {
            System.loadLibrary("apkpatch")
        } catch (_: UnsatisfiedLinkError) {
            // 未集成 native 库，差量更新不可用
        }
    }

    /**
     * 使用 HDiffPatch 合成新 APK。
     * @param patchPath  .hdiff 差量补丁路径
     * @param outputPath 合成后新 APK 输出路径
     * @return true 合成成功
     * @throws UnsatisfiedLinkError 如果 libapkpatch.so 未集成
     */
    @JvmStatic
    external fun nativeApplyPatch(patchPath: String, outputPath: String): Boolean
}
