package cn.manxinghai.zhuimange.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.data.repository.FileUploadRepository
import cn.manxinghai.zhuimange.data.repository.UserSession
import cn.manxinghai.zhuimange.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val historyCount: Int = 0,
    val downloadCount: Int = 0,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val userSession: UserSession,
    private val repository: cn.manxinghai.zhuimange.domain.repository.ManAppRepository,
    private val fileUploadRepo: FileUploadRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            val user = userSession.currentUser.first()
            _ui.update { it.copy(user = user) }

            // 加载历史数量
            repository.getHistoryList(user?.id).onSuccess { history ->
                _ui.update { it.copy(historyCount = history.size) }
            }

            // TODO: 加载下载数量（DownloadManager 暂未暴露 count API）

            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(isUploadingAvatar = true) }

            fileUploadRepo.uploadAvatar(uri).onSuccess { avatarUrl ->
                val currentUser = _ui.value.user
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(avatar = avatarUrl)
                    userSession.saveUser(
                        cn.manxinghai.zhuimange.domain.model.LoginResult(
                            user = updatedUser,
                            token = userSession.getToken() ?: ""
                        )
                    )
                    _ui.update { it.copy(user = updatedUser, isUploadingAvatar = false) }
                }
            }.onFailure { e ->
                _ui.update { it.copy(isUploadingAvatar = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userSession.logout()
            _ui.update { it.copy(user = null, historyCount = 0, downloadCount = 0) }
        }
    }

    fun refresh() {
        loadUserData()
    }
}
