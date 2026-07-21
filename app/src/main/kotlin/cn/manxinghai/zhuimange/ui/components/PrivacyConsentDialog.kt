package cn.manxinghai.zhuimange.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 隐私同意弹窗配置。
 *
 * 子 App 可自定义品牌名称、权限列表、隐私/用户协议链接。
 */
data class PrivacyConsentConfig(
    val appName: String = "",
    val permissions: List<String> = listOf(
        "存储权限：缓存内容以提供离线访问",
        "网络权限：加载内容",
        "设备标识：用于账号安全与广告投放",
    ),
    /** 隐私政策 URL，为空时不显示链接 */
    val privacyPolicyUrl: String = "",
    /** 隐私政策链接文本 */
    val privacyPolicyText: String = "《隐私政策》",
    /** 用户协议 URL，为空时不显示链接 */
    val userAgreementUrl: String = "",
    /** 用户协议链接文本 */
    val userAgreementText: String = "《用户协议》",
)

/**
 * 首次安装隐私同意弹窗。
 *
 * 用法：
 * ```
 * if (!PrivacyConsentManager.isAgreed(context)) {
 *     PrivacyConsentDialog(
 *         config = PrivacyConsentConfig(appName = "追漫阁"),
 *         onAgree = {
 *             PrivacyConsentManager.setAgreed(context)
 *             // 重新创建 Activity 以启动 SDK 初始化
 *             activity.recreate()
 *         },
 *         onDisagree = { activity.finish() },
 *     )
 * }
 * ```
 *
 * 对齐 flutter_merge：蓝字下划线链接，同意后持久化，不同意退出。
 */
@Composable
fun PrivacyConsentDialog(
    config: PrivacyConsentConfig,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
) {
    val context = LocalContext.current
    val title = if (config.appName.isNotEmpty())
        "${config.appName} 隐私保护提示" else "隐私保护提示"

    AlertDialog(
        onDismissRequest = { /* 不可外部关闭 */ },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                val welcome = if (config.appName.isNotEmpty())
                    "欢迎使用${config.appName}" else "欢迎使用"
                Text(welcome, fontSize = 14.sp)

                Spacer(Modifier.height(8.dp))
                Text(
                    if (config.appName.isNotEmpty())
                        "${config.appName}需要以下权限：" else "本应用需要以下权限：",
                    fontSize = 13.sp, color = Color.Gray
                )
                config.permissions.forEach { perm ->
                    Text("• $perm", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(12.dp))

                // 隐私政策 + 用户协议链接
                val annotated = buildAnnotatedString {
                    append("在使用服务前，请你认真阅读")
                    if (config.privacyPolicyUrl.isNotEmpty()) {
                        pushStringAnnotation("privacy", config.privacyPolicyUrl)
                        withStyle(SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                        )) { append(config.privacyPolicyText) }
                        pop()
                        append("和")
                    }
                    if (config.userAgreementUrl.isNotEmpty()) {
                        pushStringAnnotation("user", config.userAgreementUrl)
                        withStyle(SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                        )) { append(config.userAgreementText) }
                        pop()
                    }
                    append("。点击同意并继续即表示你已阅读并接受上述协议。")
                }
                Text(
                    annotated,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.clickable {
                        // 点击任意链接时打开对应 URL
                        annotated.getStringAnnotations("privacy", 0, 0).firstOrNull()?.let {
                            openUrl(context, it.item)
                        }
                        annotated.getStringAnnotations("user", 0, 0).firstOrNull()?.let {
                            openUrl(context, it.item)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text("同意并继续") }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) { Text("暂不同意") }
        },
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // 无浏览器时静默失败
    }
}

/**
 * 便捷方法：若隐私未同意则弹窗。
 *
 * @return true 如果弹窗已显示（调用方应 return，不再执行后续流程）
 */
@Composable
fun PrivacyConsentGate(
    config: PrivacyConsentConfig,
    onAgreed: () -> Unit,
    onDisagree: () -> Unit,
): Boolean {
    val context = LocalContext.current
    val needsConsent = !PrivacyConsentManager.isAgreed(context)

    if (needsConsent) {
        PrivacyConsentDialog(
            config = config,
            onAgree = {
                PrivacyConsentManager.setAgreed(context)
                onAgreed()
            },
            onDisagree = onDisagree,
        )
    }
    return needsConsent
}
