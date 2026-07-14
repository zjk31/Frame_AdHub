package cn.maga.lingdongmanhua.domain.usecase

import android.content.Context
import cn.maga.lingdongmanhua.data.AdSdkManager
import cn.maga.lingdongmanhua.domain.provider.AdProvider

/**
 * 撤回隐私同意 -> 所有已初始化的 SDK 清除状态，然后重新初始化。
 * 调用时机：用户在设置页声明撤回隐私授权后。
 */
class RevokePrivacyConsentUseCase(
    private val sdkManager: AdSdkManager,
    private val allProviders: List<AdProvider>,
) {
    suspend operator fun invoke(context: Context): Result<Unit> {
        return try {
            allProviders.forEach { provider ->
                provider.revokePrivacyConsent(context)
            }
            // 重新初始化当前通道
            sdkManager.ensureProviderReady()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
