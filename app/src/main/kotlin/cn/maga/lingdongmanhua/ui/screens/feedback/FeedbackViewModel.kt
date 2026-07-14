package cn.maga.lingdongmanhua.ui.screens.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val type: FeedbackType = FeedbackType.SYSTEM,
    val message: String = "",
    val bookTitle: String = "",         // 找漫画/书籍纠错
    val bookKeyword: String = "",       // 找漫画
    val bookErrorType: String = "",     // 书籍纠错
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)

enum class FeedbackType(val label: String) {
    SYSTEM("系统反馈"),
    FIND_BOOK("找漫画"),
    BOOK_ERROR("书籍纠错")
}

class FeedbackViewModel(
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FeedbackUiState())
    val ui: StateFlow<FeedbackUiState> = _ui.asStateFlow()

    fun onTypeChange(type: FeedbackType) {
        _ui.update { it.copy(type = type, error = null) }
    }

    fun onMessageChange(msg: String) {
        _ui.update { it.copy(message = msg, error = null) }
    }

    fun onBookTitleChange(title: String) {
        _ui.update { it.copy(bookTitle = title, error = null) }
    }

    fun onBookKeywordChange(keyword: String) {
        _ui.update { it.copy(bookKeyword = keyword, error = null) }
    }

    fun onBookErrorTypeChange(type: String) {
        _ui.update { it.copy(bookErrorType = type, error = null) }
    }

    fun submit() {
        val state = _ui.value

        when (state.type) {
            FeedbackType.SYSTEM -> {
                if (state.message.isBlank()) {
                    _ui.update { it.copy(error = "请输入反馈内容") }
                    return
                }
                submitSystemFeedback(state.message)
            }
            FeedbackType.FIND_BOOK -> {
                if (state.bookTitle.isBlank()) {
                    _ui.update { it.copy(error = "请输入漫画名称") }
                    return
                }
                submitFindBook(state.bookTitle, state.bookKeyword)
            }
            FeedbackType.BOOK_ERROR -> {
                if (state.message.isBlank() || state.bookErrorType.isBlank()) {
                    _ui.update { it.copy(error = "请选择错误类型并填写描述") }
                    return
                }
                // TODO: 传入 mangaId
            }
        }
    }

    private fun submitSystemFeedback(message: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null) }
            repository.submitSystemFeedback("system", message)
                .onSuccess {
                    _ui.update { it.copy(isSubmitting = false, submitSuccess = true) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isSubmitting = false, error = e.message ?: "提交失败") }
                }
        }
    }

    private fun submitFindBook(title: String, keyword: String?) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null) }
            repository.submitFindBookFeedback(title, keyword)
                .onSuccess {
                    _ui.update { it.copy(isSubmitting = false, submitSuccess = true) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isSubmitting = false, error = e.message ?: "提交失败") }
                }
        }
    }

    fun reset() {
        _ui.update { FeedbackUiState() }
    }
}
