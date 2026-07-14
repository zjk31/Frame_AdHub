package cn.maga.lingdongmanhua.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.repository.UserSession
import cn.maga.lingdongmanhua.domain.model.Chapter
import cn.maga.lingdongmanhua.domain.model.Manga
import cn.maga.lingdongmanhua.domain.model.MangaDetail
import cn.maga.lingdongmanhua.domain.model.ReadingHistory
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val mangaDetail: MangaDetail? = null,
    val isFavorited: Boolean = false,
    val favoriteCount: Int = 0,
    val readCount: Int = 0,
    val recommendations: List<Manga> = emptyList(),
    // 阅读历史（继续阅读）
    val readingHistory: ReadingHistory? = null,
    val error: String? = null
)

class DetailViewModel(
    private val mangaId: Long,
    private val repository: ManAppRepository,
    private val userSession: UserSession
) : ViewModel() {

    private val _ui = MutableStateFlow(DetailUiState())
    val ui: StateFlow<DetailUiState> = _ui.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            // 并行加载详情 + 阅读数 + 收藏数 + 收藏状态
            val detailResult = repository.getMangaDetail(mangaId)
            val readCountResult = repository.getReadCount(mangaId)
            val favCountResult = repository.getFavoriteCount(mangaId)
            val historyResult = loadHistory()
            val recommendResult = repository.getRandomBrowse(mangaId)

            detailResult.onSuccess { detail ->
                _ui.update { it.copy(mangaDetail = detail) }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "加载失败") }
            }

            readCountResult.onSuccess { count ->
                _ui.update { it.copy(readCount = count) }
            }

            favCountResult.onSuccess { count ->
                _ui.update { it.copy(favoriteCount = count) }
            }

            historyResult.onSuccess { history ->
                _ui.update { it.copy(readingHistory = history) }
            }

            recommendResult.onSuccess { list ->
                _ui.update { it.copy(recommendations = list.take(9)) }
            }

            // 检查收藏状态
            userSession.currentUser.first()?.let { user ->
                repository.isFavorite(user.id, mangaId).onSuccess { favorited ->
                    _ui.update { it.copy(isFavorited = favorited) }
                }
            }

            _ui.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadHistory(): Result<ReadingHistory?> {
        return repository.queryHistory(mangaId, userSession.currentUser.first()?.id)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val user = userSession.currentUser.first()
            if (user == null) {
                // TODO: 导航到登录页
                return@launch
            }

            repository.toggleFavorite(user.id, mangaId).onSuccess { result ->
                _ui.update {
                    it.copy(
                        isFavorited = result,
                        favoriteCount = if (result) it.favoriteCount + 1 else maxOf(0, it.favoriteCount - 1)
                    )
                }
            }
        }
    }

    fun refresh() {
        loadDetail()
    }

    /**
     * 获取最新章节（用于快速阅读入口）
     */
    fun getLatestChapter(): Chapter? {
        return _ui.value.mangaDetail?.chapters?.lastOrNull()
    }

    /**
     * 获取继续阅读的章节
     */
    fun getContinueChapter(): Chapter? {
        val history = _ui.value.readingHistory ?: return null
        return _ui.value.mangaDetail?.chapters?.find { it.id == history.lastChapterId }
    }
}
