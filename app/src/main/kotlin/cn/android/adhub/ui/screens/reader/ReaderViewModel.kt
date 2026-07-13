package cn.android.adhub.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.android.adhub.data.repository.UserSession
import cn.android.adhub.domain.model.Chapter
import cn.android.adhub.domain.model.ChapterPage
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val isLoading: Boolean = true,
    val chapter: Chapter? = null,
    val pages: List<ChapterPage> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false,
    val showControls: Boolean = true,
    val isNightMode: Boolean = false,
    val autoScroll: Boolean = false,
    val autoScrollInterval: Long = 3000L,
    val error: String? = null
)

class ReaderViewModel(
    private val chapterId: Long,
    private val initialPage: Int,      // 从历史记录恢复的页码
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ReaderUiState())
    val ui: StateFlow<ReaderUiState> = _ui.asStateFlow()

    init {
        loadChapter(chapterId, initialPage)
    }

    private fun loadChapter(id: Long, restorePage: Int = 0) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            repository.getChapter(id).onSuccess { chapter ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        chapter = chapter,
                        pages = chapter.pages,
                        totalPages = chapter.pages.size,
                        currentPage = restorePage.coerceIn(0, chapter.pages.size - 1),
                        hasPrev = chapter.prevChapterId != null,
                        hasNext = chapter.nextChapterId != null
                    )
                }
                // 同步阅读进度
                syncProgress(id, restorePage)
            }.onFailure { e ->
                _ui.update {
                    it.copy(isLoading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    private fun syncProgress(chapterId: Long, page: Int) {
        viewModelScope.launch {
            val chapter = _ui.value.chapter ?: return@launch
            repository.syncHistory(
                items = listOf(Triple(chapter.mangaId, chapterId, page + 1))
            )
        }
    }

    fun onPageChanged(page: Int) {
        val pages = _ui.value.pages
        if (page < 0 || page >= pages.size) return
        _ui.update { it.copy(currentPage = page) }
        syncProgress(_ui.value.chapter?.id ?: return, page)
    }

    fun toggleControls() {
        _ui.update { it.copy(showControls = !it.showControls) }
    }

    fun toggleNightMode() {
        _ui.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun toggleAutoScroll() {
        _ui.update { it.copy(autoScroll = !it.autoScroll) }
    }

    fun setAutoScrollInterval(interval: Long) {
        _ui.update { it.copy(autoScrollInterval = interval) }
    }

    fun goPrevChapter() {
        _ui.value.chapter?.prevChapterId?.let { prevId ->
            loadChapter(prevId)
        }
    }

    fun goNextChapter() {
        _ui.value.chapter?.nextChapterId?.let { nextId ->
            loadChapter(nextId)
        }
    }

    fun retry() {
        _ui.value.chapter?.id?.let { loadChapter(it) }
    }
}
