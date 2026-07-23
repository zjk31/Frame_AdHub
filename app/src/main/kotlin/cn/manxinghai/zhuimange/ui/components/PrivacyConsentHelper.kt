package cn.manxinghai.zhuimange.ui.components

import android.app.Activity
import android.app.AlertDialog
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color as AColor

/**
 * View-based 隐私同意辅助（用于 SplashAdActivity 等纯 Activity）。
 *
 * Compose 页面请使用 [PrivacyConsentDialog] + [PrivacyConsentManager]。
 */
object PrivacyConsentHelper {

    /**
     * 隐私同意配置。
     */
    data class ConsentConfig(
        val appName: String = "",
        val privacyPolicyUrl: String = "",
        val privacyPolicyText: String = "《隐私政策》",
        val userAgreementUrl: String = "",
        val userAgreementText: String = "《用户协议》",
    )

    /**
     * 若未同意隐私政策则弹出对话框。
     *
     * @return true 如果弹窗已显示（调用方应 return，不再执行后续流程）
     */
    fun showIfNeeded(activity: Activity, config: ConsentConfig = ConsentConfig()): Boolean {
        val prefs = activity.getSharedPreferences("privacy_consent", Activity.MODE_PRIVATE)
        if (prefs.getBoolean("agreed", false)) return false

        val tv = TextView(activity).apply {
            setPadding(48, 32, 48, 16)
            text = buildSpannable(activity, config)
            movementMethod = LinkMovementMethod.getInstance()
            setTextColor(AColor.DKGRAY)
            textSize = 14f
            setLineSpacing(4f, 1f)
        }

        AlertDialog.Builder(activity)
            .setTitle(if (config.appName.isNotEmpty()) "${config.appName} 隐私保护提示" else "隐私保护提示")
            .setView(tv)
            .setCancelable(false)
            .setPositiveButton("同意并继续") { _, _ ->
                prefs.edit().putBoolean("agreed", true).apply()
                activity.recreate()
            }
            .setNegativeButton("暂不同意") { _, _ -> activity.finish() }
            .show()
        return true
    }

    private fun buildSpannable(activity: Activity, config: ConsentConfig): SpannableString {
        val appName = config.appName.ifEmpty { "本应用" }
        val text = "欢迎使用${appName}\n\n" +
                "${appName}需要以下权限：\n" +
                "• 存储权限：缓存内容以提供离线访问\n" +
                "• 网络权限：加载内容\n" +
                "• 设备标识：用于账号安全与广告投放\n" +
                "• 位置权限：广告精准投放（可选）\n\n" +
                "在使用服务前，请你认真阅读" +
                config.privacyPolicyText +
                "和" +
                config.userAgreementText +
                "。点击同意并继续即表示你已阅读并接受上述协议。"

        val spannable = SpannableString(text)

        if (config.privacyPolicyUrl.isNotEmpty()) {
            val privacyStart = text.indexOf(config.privacyPolicyText)
            if (privacyStart >= 0) {
                spannable.setSpan(
                    createLinkSpan { showHtmlDialog(activity, config.privacyPolicyUrl, "隐私政策") },
                    privacyStart, privacyStart + config.privacyPolicyText.length, 0
                )
            }
        }

        if (config.userAgreementUrl.isNotEmpty()) {
            val userStart = text.indexOf(config.userAgreementText)
            if (userStart >= 0) {
                spannable.setSpan(
                    createLinkSpan { showHtmlDialog(activity, config.userAgreementUrl, "用户协议") },
                    userStart, userStart + config.userAgreementText.length, 0
                )
            }
        }

        return spannable
    }

    private fun createLinkSpan(onClick: () -> Unit): ClickableSpan {
        return object : ClickableSpan() {
            override fun onClick(widget: View) = onClick()
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = AColor.BLUE
            }
        }
    }

    private fun showHtmlDialog(activity: Activity, url: String, title: String) {
        val webView = WebView(activity).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = false
            loadUrl(url)
        }
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(webView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (600 * activity.resources.displayMetrics.density).toInt()
            ))
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("关闭", null)
            .show()
    }
}
