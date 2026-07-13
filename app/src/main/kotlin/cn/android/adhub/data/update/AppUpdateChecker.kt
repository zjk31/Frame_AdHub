package cn.android.adhub.data.update

import android.content.Context
import android.content.pm.PackageManager
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * APP 版本更新检查器
 * 在 App 启动时自动检查，也可手动触发
 */
class AppUpdateChecker(
    private val repository: ManAppRepository,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * 检查更新（静默，不显示错误）
     */
    fun check(autoCheck: Boolean = false) {
        scope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = packageInfo.versionName ?: "1.0.0"
                val buildNumber = packageInfo.longVersionCode.toString()

                repository.checkAppUpdate(
                    packageName = context.packageName,
                    version = currentVersion,
                    buildNumber = buildNumber
                ).onSuccess { info ->
                    if (info != null && info.versionCode > packageInfo.longVersionCode) {
                        _updateState.value = UpdateState.UpdateAvailable(info)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                }.onFailure {
                    if (autoCheck) {
                        _updateState.value = UpdateState.Idle
                    } else {
                        _updateState.value = UpdateState.Error(it.message ?: "检查更新失败")
                    }
                }
            } catch (e: Exception) {
                _updateState.value = if (autoCheck) UpdateState.Idle else UpdateState.Error(e.message ?: "检查更新失败")
            }
        }
    }

    /**
     * 重置状态
     */
    fun reset() {
        _updateState.value = UpdateState.Idle
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class UpdateAvailable(val info: ManAppRepository.AppUpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
