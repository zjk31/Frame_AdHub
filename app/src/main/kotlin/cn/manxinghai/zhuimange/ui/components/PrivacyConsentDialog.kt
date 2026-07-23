package cn.manxinghai.zhuimange.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * 隐私同意状态管理。
 *
 * 与 [PrivacyConsentHelper]（View-based，供 SplashAdActivity 使用）共享同一 SharedPreferences。
 */
object PrivacyConsentManager {
    private const val PREFS = "privacy_consent"
    private const val KEY_AGREED = "agreed"

    fun isAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    fun setAgreed(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AGREED, true).apply()
    }

    fun revoke(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AGREED, false).apply()
    }
}

/**
 * 首次安装隐私同意弹窗（Compose 版本）。
 *
 * 子 App 使用时直接修改文案即可，无需额外配置类。
 *
 * @param onAgree 同意后回调
 * @param onDisagree 不同意回调（通常 finish Activity）
 */
@Composable
fun PrivacyConsentDialog(onAgree: () -> Unit, onDisagree: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text("隐私保护提示") },
        text = {
            Column {
                Text("欢迎使用", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("本应用需要以下权限：", fontSize = 13.sp, color = Color.Gray)
                Text("• 存储权限：缓存内容以提供离线访问", fontSize = 13.sp, color = Color.Gray)
                Text("• 网络权限：加载内容", fontSize = 13.sp, color = Color.Gray)
                Text("• 设备标识：用于账号安全与广告投放", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                val annotated = buildAnnotatedString {
                    append("在使用服务前，请你认真阅读")
                    pushStringAnnotation("privacy", "privacy")
                    withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                        append("《隐私政策》")
                    }
                    pop()
                    append("和")
                    pushStringAnnotation("user", "user")
                    withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                        append("《用户协议》")
                    }
                    pop()
                    append("。点击同意并继续即表示你已阅读并接受上述协议。")
                }
                Text(annotated, fontSize = 13.sp, lineHeight = 20.sp)
            }
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("同意并继续") } },
        dismissButton = { TextButton(onClick = onDisagree) { Text("暂不同意") } }
    )
}
