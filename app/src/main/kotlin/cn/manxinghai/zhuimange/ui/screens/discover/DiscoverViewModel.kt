package cn.manxinghai.zhuimange.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.domain.model.Manga
import cn.manxinghai.zhuimange.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val popularRank: List<Manga> = emptyList(),
    val collectRank: List<Manga> = emptyList(),
    val hotRank: List<Manga> = emptyList(),
    val recommendations: List<Manga> = emptyList(),
    val error: String? = null
)

class DiscoverViewModel(
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(DiscoverUiState())
    val ui: StateFlow<DiscoverUiState> = _ui.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            val popularResult = repository.getPopularRank(null)
            val collectResult = repository.getCollectRank(null)
            val hotResult = repository.getHotRank(null)

            popularResult.onSuccess { list ->
                _ui.update { it.copy(popularRank = list.take(20)) }
            }
            collectResult.onSuccess { list ->
                _ui.update { it.copy(collectRank = list.take(20)) }
            }
            hotResult.onSuccess { list ->
                _ui.update { it.copy(hotRank = list.take(20)) }
            }

            // 推荐：取人气榜前几个
            val popular = _ui.value.popularRank
            if (popular.isNotEmpty()) {
                repository.getRandomBrowse(popular.first().id).onSuccess { list ->
                    _ui.update { it.copy(recommendations = list.take(12)) }
                }
            }

            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        loadData()
    }
}
