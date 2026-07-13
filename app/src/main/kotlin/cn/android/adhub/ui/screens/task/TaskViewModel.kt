package cn.android.adhub.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.android.adhub.data.repository.UserSession
import cn.android.adhub.domain.model.DownloadQuota
import cn.android.adhub.domain.model.RewardData
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskUiState(
    val isLoading: Boolean = true,
    val rewardData: RewardData? = null,
    val downloadQuota: DownloadQuota? = null,
    val isSigningIn: Boolean = false,
    val signInSuccess: Boolean = false,
    val error: String? = null
)

class TaskViewModel(
    private val repository: ManAppRepository,
    private val userSession: UserSession
) : ViewModel() {

    private val _ui = MutableStateFlow(TaskUiState())
    val ui: StateFlow<TaskUiState> = _ui.asStateFlow()

    init {
        loadRewardData()
    }

    private suspend fun getInviteCode(): String {
        return userSession.currentUser.first()?.inviteCode ?: ""
    }

    private fun loadRewardData() {
        viewModelScope.launch {
            val inviteCode = getInviteCode()
            if (inviteCode.isBlank()) return@launch
            _ui.update { it.copy(isLoading = true, error = null) }

            val rewardResult = repository.getReward(inviteCode)
            val quotaResult = repository.getDownloadQuota(inviteCode)

            rewardResult.onSuccess { data ->
                _ui.update { it.copy(rewardData = data) }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message) }
            }

            quotaResult.onSuccess { quota ->
                _ui.update { it.copy(downloadQuota = quota) }
            }

            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            val inviteCode = getInviteCode()
            if (inviteCode.isBlank()) return@launch
            val signInDays = (_ui.value.rewardData?.signInDays ?: 0) + 1

            _ui.update { it.copy(isSigningIn = true, signInSuccess = false) }
            repository.signIn(inviteCode, signInDays)
                .onSuccess { quota ->
                    _ui.update {
                        it.copy(
                            isSigningIn = false,
                            signInSuccess = true,
                            downloadQuota = quota,
                            rewardData = it.rewardData?.copy(
                                todaySignedIn = true,
                                signInDays = signInDays
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(isSigningIn = false, error = e.message ?: "签到失败")
                    }
                }
        }
    }

    fun refresh() {
        loadRewardData()
    }
}
