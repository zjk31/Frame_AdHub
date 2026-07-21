package cn.manxinghai.zhuimange.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * 调起系统安装器安装 APK。
 */
object ApkInstaller {
    private const val TAG = "ApkInstaller"
    private const val PROVIDER_AUTHORITY_SUFFIX = ".update.provider"

    /**
     * 使用系统安装器打开本地 APK 文件。
     * 需要 AndroidManifest.xml 中声明对应的 FileProvider：
     * ```xml
     * <provider
     *     android:name="androidx.core.content.FileProvider"
     *     android:authorities="${applicationId}.update.provider"
     *     android:exported="false"
     *     android:grantUriPermissions="true">
     *     <meta-data
     *         android:name="android.support.FILE_PROVIDER_PATHS"
     *         android:resource="@xml/update_file_paths" />
     * </provider>
     * ```
     * @return true 安装器成功启动
     */
    fun install(context: Context, apkPath: String): Boolean {
        val file = File(apkPath)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "APK 文件不存在或不可读: $apkPath")
            return false
        }

        val authority = context.packageName + PROVIDER_AUTHORITY_SUFFIX
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "启动安装器失败", e)
            false
        }
    }
}
