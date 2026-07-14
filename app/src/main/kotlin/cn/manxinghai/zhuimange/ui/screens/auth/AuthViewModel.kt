package cn.manxinghai.zhuimange.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.data.repository.UserSession
import cn.manxinghai.zhuimange.domain.model.User
import cn.manxinghai.zhuimange.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false      // 登录成功后置 true
)

enum class AuthMode { LOGIN, REGISTER, FORGOT_PASSWORD }

class AuthViewModel(
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun onUsernameChange(v: String) {
        _ui.update { it.copy(username = v, error = null) }
    }

    fun onPasswordChange(v: String) {
        _ui.update { it.copy(password = v, error = null) }
    }

    fun onConfirmPasswordChange(v: String) {
        _ui.update { it.copy(confirmPassword = v, error = null) }
    }

    fun switchMode(mode: AuthMode) {
        _ui.update { it.copy(mode = mode, error = null) }
    }

    fun submit() {
        val state = _ui.value
        when (state.mode) {
            AuthMode.LOGIN -> doLogin()
            AuthMode.REGISTER -> doRegister()
            AuthMode.FORGOT_PASSWORD -> doResetPassword()
        }
    }

    private fun doLogin() {
        val state = _ui.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _ui.update { it.copy(error = "请输入用户名和密码") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            repository.login(state.username, state.password)
                .onSuccess {
                    _ui.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isLoading = false, error = e.message ?: "登录失败") }
                }
        }
    }

    private fun doRegister() {
        val state = _ui.value
        when {
            state.username.length < 3 -> _ui.update { it.copy(error = "用户名至少3个字符") }
            state.password.length < 6 -> _ui.update { it.copy(error = "密码至少6位") }
            state.password != state.confirmPassword -> _ui.update { it.copy(error = "两次密码不一致") }
            else -> {
                viewModelScope.launch {
                    _ui.update { it.copy(isLoading = true, error = null) }
                    repository.register(
                        username = state.username,
                        password = state.password,
                        checkPassword = state.confirmPassword,
                        deviceId = null,
                        inviterId = null
                    ).onSuccess { ok ->
                        if (ok) {
                            // 注册成功后自动登录
                            repository.login(state.username, state.password)
                                .onSuccess {
                                    _ui.update { it.copy(isLoading = false, success = true) }
                                }
                                .onFailure {
                                    // 注册成功但登录失败，提示用户去登录
                                    _ui.update {
                                        it.copy(
                                            isLoading = false,
                                            mode = AuthMode.LOGIN,
                                            password = "",
                                            confirmPassword = "",
                                            error = "注册成功，请登录"
                                        )
                                    }
                                }
                        } else {
                            _ui.update { it.copy(isLoading = false, error = "注册失败") }
                        }
                    }.onFailure { e ->
                        _ui.update { it.copy(isLoading = false, error = e.message ?: "注册失败") }
                    }
                }
            }
        }
    }

    private fun doResetPassword() {
        val state = _ui.value
        if (state.username.isBlank()) {
            _ui.update { it.copy(error = "请输入用户名") }
            return
        }
        if (state.password.isBlank() || state.confirmPassword.isBlank()) {
            _ui.update { it.copy(error = "请输入新密码") }
            return
        }
        if (state.password != state.confirmPassword) {
            _ui.update { it.copy(error = "两次密码不一致") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            repository.changePassword(state.username, state.password, state.confirmPassword)
                .onSuccess { ok ->
                    if (ok) {
                        _ui.update {
                            it.copy(
                                isLoading = false,
                                mode = AuthMode.LOGIN,
                                password = "",
                                confirmPassword = "",
                                error = null
                            )
                        }
                    } else {
                        _ui.update { it.copy(isLoading = false, error = "修改失败") }
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isLoading = false, error = e.message ?: "修改失败") }
                }
        }
    }
}
