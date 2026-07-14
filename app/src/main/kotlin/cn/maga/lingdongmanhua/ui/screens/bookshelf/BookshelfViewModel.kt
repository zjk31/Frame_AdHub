package cn.maga.lingdongmanhua.ui.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.domain.model.FavoriteItem
import cn.maga.lingdongmanhua.domain.model.ReadingHistory
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookshelfTab { FAVORITES, HISTORY }

data class BookshelfUiState(
    val tab: BookshelfTab = BookshelfTab.FAVORITES,
    val favorites: List<FavoriteItem> = emptyList(),
    val history: List<ReadingHistory> = emptyList(),
    val unreadChapterCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class BookshelfViewModel(
    private val userId: Long?,
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(BookshelfUiState())
    val ui: StateFlow<BookshelfUiState> = _ui.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            // 并行加载
            val favResult = if (userId != null) {
                repository.getFavorites(userId)
            } else {
                Result.success(cn.maga.lingdongmanhua.domain.model.PagedResult(emptyList(), 1, 0, 0))
            }

            val historyResult = repository.getHistoryList(userId)

            favResult.onSuccess { paged ->
                _ui.update { it.copy(favorites = paged.items) }

                // 为每个收藏漫画加载未读章节数
                // TODO: 后端如果提供批量接口更好，暂时逐个查询
                val unreadMap = mutableMapOf<Long, Int>()
                for (item in paged.items) {
                    // 未读数 = 总章节数 - 已读章节数
                    // 需要通过 history 判断
                    val history = historyResult.getOrNull()?.find { it.mangaId == item.mangaId }
                    if (history != null) {
                        // 有阅读记录，计算未读
                        // 这里简化处理，实际需要后端返回未读数
                    }
                }
                _ui.update { it.copy(unreadChapterCounts = unreadMap) }
            }

            historyResult.onSuccess { list ->
                _ui.update { it.copy(history = list) }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message) }
            }

            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun switchTab(tab: BookshelfTab) {
        _ui.update { it.copy(tab = tab) }
    }

    fun deleteFavorite(mangaId: Long) {
        if (userId == null) return
        viewModelScope.launch {
            repository.batchDeleteFavorites(userId, listOf(mangaId)).onSuccess {
                _ui.update { state ->
                    state.copy(favorites = state.favorites.filter { it.mangaId != mangaId })
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(userId).onSuccess {
                _ui.update { it.copy(history = emptyList()) }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}
