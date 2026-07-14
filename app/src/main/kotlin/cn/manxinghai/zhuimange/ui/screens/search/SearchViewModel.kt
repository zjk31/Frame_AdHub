package cn.manxinghai.zhuimange.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.domain.model.Manga
import cn.manxinghai.zhuimange.domain.model.MangaStatus
import cn.manxinghai.zhuimange.domain.model.SearchSuggestion
import cn.manxinghai.zhuimange.domain.repository.ManAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun SearchSuggestion.toManga() = Manga(
    id = id,
    name = name,
    author = author,
    coverUrl = coverUrl,
    tags = emptyList(),
    status = MangaStatus.SERIALIZING,
    description = "",
    readCount = 0,
    favoriteCount = 0,
    chapterCount = 0,
    lastUpdateChapterName = null,
    lastUpdateTime = null
)

data class SearchUiState(
    val query: String = "",
    val suggestions: List<SearchSuggestion> = emptyList(),
    val results: List<Manga> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)

class SearchViewModel(
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var searchJob: Job? = null
    private var currentPage = 1

    fun onQueryChange(query: String) {
        _ui.update { it.copy(query = query) }
        if (query.isBlank()) {
            _ui.update { it.copy(suggestions = emptyList(), results = emptyList(), hasSearched = false) }
            return
        }

        // 防抖 300ms 触发搜索联想
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadSuggestions(query)
        }
    }

    private suspend fun loadSuggestions(keyword: String) {
        repository.search(keyword, pageSize = 5).onSuccess { paged ->
            _ui.update { it.copy(suggestions = paged.items) }
        }
    }

    fun onSearch(query: String = _ui.value.query) {
        if (query.isBlank()) return
        searchJob?.cancel()
        currentPage = 1
        _ui.update { it.copy(isSearching = true, hasSearched = true, suggestions = emptyList(), error = null) }

        viewModelScope.launch {
            repository.search(query, currentPage).onSuccess { paged ->
                val mangaItems = paged.items.map { it.toManga() }
                _ui.update {
                    it.copy(
                        isSearching = false,
                        results = mangaItems,
                        hasMore = paged.hasNext
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(isSearching = false, error = e.message ?: "搜索失败")
                }
            }
        }
    }

    fun loadMore() {
        if (_ui.value.isSearching || !_ui.value.hasMore) return
        currentPage++
        viewModelScope.launch {
            repository.search(_ui.value.query, currentPage).onSuccess { paged ->
                val mangaItems = paged.items.map { it.toManga() }
                _ui.update {
                    it.copy(results = it.results + mangaItems, hasMore = paged.hasNext)
                }
            }
        }
    }

    fun clearQuery() {
        _ui.update { SearchUiState() }
    }
}
