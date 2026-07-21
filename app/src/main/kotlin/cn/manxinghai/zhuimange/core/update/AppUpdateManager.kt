package cn.manxinghai.zhuimange.core.update

import android.content.Context
import android.util.Log
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.core.network.AppNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 应用更新管理器 — 纯引擎，无业务依赖。
 *
 * 流程：下载差量/整包 → HDiffPatch 合成 → MD5 校验 → 安装。
 *
 * 子 App 负责：
 *   1. 从自己的后端获取 [AppUpdateInfo]
 *   2. 传入 [startUpdate] 执行更新
 *   3. 监听 [state] / [progress] 展示 UI
 *   4. 就绪后调用 [installApk] 触发安装
 *
 * 差量更新需要 [libapkpatch.so]（HDiffPatch 原生库）。
 * 未集成原库时差量自动失败 → 回退整包下载。
 */
class AppUpdateManager {

    companion object {
        private const val TAG = "AppUpdateManager"
        const val UPDATE_DIR = "update"
    }

    // ── 下载客户端（长超时，大文件） ──

    private val downloadClient: OkHttpClient by lazy {
        AppNetworkClient.buildClient(readTimeoutSec = 120)
            .newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── 状态 ──

    private val _state = MutableStateFlow<PatchState>(PatchState.Idle)
    val state: StateFlow<PatchState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /** 合成/下载后的 APK 本地路径，供 [installApk] 使用 */
    var apkLocalPath: String = ""
        private set

    // ═══════════════════════════════════════════════════════
    //  公开 API
    // ═══════════════════════════════════════════════════════

    /**
     * 开始更新。
     *
     * @param info 后端返回的更新信息
     * @param currentVersion 当前应用版本（BuildConfig.VERSION_NAME）
     * @return true 成功进入就绪状态，false 失败
     */
    suspend fun startUpdate(info: AppUpdateInfo, currentVersion: String): Boolean {
        // 差量路径：有补丁 URL 且基准版本匹配
        if (!info.patchUrl.isNullOrEmpty() && info.fromVersion == currentVersion) {
            val ok = downloadAndApplyPatch(info)
            if (ok) return true
            Log.w(TAG, "差量合成失败，回退整包更新")
        }

        // 整包路径
        return downloadFullApk(info)
    }

    /**
     * 调起系统安装器。
     * @return true 安装器成功启动
     */
    fun installApk(): Boolean {
        if (apkLocalPath.isEmpty()) return false
        return ApkInstaller.install(AppContextHolder.context, apkLocalPath)
    }

    // ═══════════════════════════════════════════════════════
    //  差量更新
    // ═══════════════════════════════════════════════════════

    private suspend fun downloadAndApplyPatch(info: AppUpdateInfo): Boolean {
        val patchUrl = info.patchUrl ?: return false
        val ctx = AppContextHolder.context
        val updateDir = ensureUpdateDir(ctx) ?: return false
        val patchFile = File(updateDir, "patch-${info.fromVersion}-${info.version}.hdiff")
        val outputApk = File(updateDir, "app-${info.version}.apk")

        _progress.value = 0f
        _state.value = PatchState.Downloading

        if (!downloadFile(patchUrl, patchFile) { _progress.value = it }) {
            _state.value = PatchState.Error("补丁下载失败"); return false
        }

        // 大小校验
        info.patchSize?.let { expected ->
            if (expected > 0 && patchFile.length() != expected) {
                _state.value = PatchState.Error("补丁文件大小不匹配"); return false
            }
        }

        // 错误页检测（< 1KB 且含 HTML → 服务端返回错误）
        if (patchFile.length() < 1024) {
            val head = patchFile.inputStream().use { ins ->
                val buf = ByteArray(100.coerceAtMost(patchFile.length().toInt()))
                ins.read(buf); String(buf)
            }
            if (head.contains("<!DOCTYPE") || head.contains("<html") || head.contains("error")) {
                _state.value = PatchState.Error("补丁下载失败：服务器返回错误"); return false
            }
        }

        // MD5 校验
        info.patchMd5?.let { expected ->
            if (!verifyMd5(patchFile, expected)) {
                _state.value = PatchState.Error("补丁校验失败"); return false
            }
        }

        // 合成
        _state.value = PatchState.Patching
        _progress.value = 0f
        val ok = withContext(Dispatchers.IO) {
            try {
                ApkPatch.nativeApplyPatch(patchFile.absolutePath, outputApk.absolutePath)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "libapkpatch.so 未集成", e)
                false
            }
        }
        if (!ok) { _state.value = PatchState.Error("补丁合成失败"); return false }

        // 合成包校验
        info.apkMd5?.let { expected ->
            if (!verifyMd5(outputApk, expected)) {
                _state.value = PatchState.Error("合成包校验失败"); return false
            }
        }

        apkLocalPath = outputApk.absolutePath
        _state.value = PatchState.ReadyToInstall(apkLocalPath)
        return true
    }

    // ═══════════════════════════════════════════════════════
    //  整包更新
    // ═══════════════════════════════════════════════════════

    private suspend fun downloadFullApk(info: AppUpdateInfo): Boolean {
        val ctx = AppContextHolder.context
        val updateDir = ensureUpdateDir(ctx) ?: return false
        val apkFile = File(updateDir, "app-${info.version}.apk")

        _progress.value = 0f
        _state.value = PatchState.Downloading

        if (!downloadFile(info.downloadUrl, apkFile) { _progress.value = it }) {
            _state.value = PatchState.Error("下载失败"); return false
        }

        info.apkMd5?.let { expected ->
            if (!verifyMd5(apkFile, expected)) {
                _state.value = PatchState.Error("安装包校验失败"); return false
            }
        }

        apkLocalPath = apkFile.absolutePath
        _state.value = PatchState.ReadyToInstall(apkLocalPath)
        return true
    }

    // ═══════════════════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════════════════

    private fun ensureUpdateDir(ctx: Context): File? {
        val dir = File(ctx.externalCacheDir ?: ctx.cacheDir, UPDATE_DIR)
        return try { if (!dir.exists()) dir.mkdirs(); dir } catch (_: Exception) { null }
    }

    private suspend fun downloadFile(
        url: String, dest: File, onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = downloadClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) { Log.e(TAG, "HTTP ${response.code}"); return@withContext false }
            val body = response.body ?: return@withContext false
            val len = body.contentLength()
            var total = 0L
            body.byteStream().use { ins ->
                FileOutputStream(dest).use { outs ->
                    val buf = ByteArray(8192); var n: Int
                    while (ins.read(buf).also { n = it } != -1) {
                        outs.write(buf, 0, n); total += n
                        if (len > 0) onProgress(total.toFloat() / len)
                    }
                }
            }
            true
        } catch (e: Exception) { Log.e(TAG, "下载异常", e); false }
    }

    private fun verifyMd5(file: File, expected: String): Boolean {
        if (expected.isEmpty()) return true
        val ok = fileMd5(file).equals(expected, ignoreCase = true)
        if (!ok) Log.e(TAG, "MD5 mismatch: $expected")
        return ok
    }

    private fun fileMd5(file: File): String {
        if (!file.exists()) return ""
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { ins ->
            val buf = ByteArray(8192); var n: Int
            while (ins.read(buf).also { n = it } != -1) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
