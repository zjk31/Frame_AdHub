package cn.maga.lingdongmanhua.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.domain.model.BannerManga
import cn.maga.lingdongmanhua.domain.model.Category
import cn.maga.lingdongmanhua.domain.model.Manga
import cn.maga.lingdongmanhua.domain.model.RankType
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val selectedCategoryIndex: Int = 0,       // 顶部 Tab 索引（0=全部, 1..9=漫画分类, 后续=后端分类）
    val selectedBackendCategoryIndex: Int = 0, // 后端分类下的子分类索引
    // 漫画列表
    val mangaList: List<Manga> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    // 排行榜
    val popularRank: List<Manga> = emptyList(),
    val collectRank: List<Manga> = emptyList(),
    val hotRank: List<Manga> = emptyList(),
    // Banner
    val banners: List<BannerManga> = emptyList(),
    // 随机推荐（猜你喜欢）
    val recommendations: List<Manga> = emptyList(),
    // 错误
    val error: String? = null
)

class HomeViewModel(
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    private var currentPage = 1
    private var currentCategoryName: String? = null   // 后端子分类名
    private var currentMangaCategory: String? = null    // 漫画分类（热血、冒险等）

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            // 真正并行拉取：分类 + 三个榜单 + Banner + 漫画列表
            val categoriesDef = async { repository.getGlobalCategories() }
            val popularDef = async { repository.getPopularRank(null) }
            val collectDef = async { repository.getCollectRank(null) }
            val hotDef = async { repository.getHotRank(null) }
            val bannerDef = async { repository.getHomeBanners() }
            val mangaDef = async {
                repository.getMostReadManga(page = 1)
            }

            val categoriesResult = categoriesDef.await()
            val popularResult = popularDef.await()
            val collectResult = collectDef.await()
            val hotResult = hotDef.await()
            val bannerResult = bannerDef.await()
            val mangaResult = mangaDef.await()

            categoriesResult.onSuccess { cats ->
                _ui.update { it.copy(categories = cats) }
            }
            popularResult.onSuccess { list ->
                _ui.update { it.copy(popularRank = list.take(10)) }
            }
            collectResult.onSuccess { list ->
                _ui.update { it.copy(collectRank = list.take(10)) }
            }
            hotResult.onSuccess { list ->
                _ui.update { it.copy(hotRank = list.take(10)) }
            }
            bannerResult.onSuccess { banners ->
                _ui.update { it.copy(banners = banners) }
            }
            mangaResult.onSuccess { paged ->
                _ui.update { it.copy(mangaList = paged.items, hasMore = paged.hasNext) }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "加载失败") }
            }

            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun loadMangaList(refresh: Boolean = false) {
        if (refresh) currentPage = 1
        viewModelScope.launch {
            if (refresh) {
                _ui.update { it.copy(isLoading = true, error = null) }
            } else {
                _ui.update { it.copy(isLoadingMore = true) }
            }

            val result = when {
                // 优先：后端子分类
                currentCategoryName != null -> {
                    repository.getMangaBySubCategory(currentCategoryName!!, currentPage)
                }
                // 其次：漫画分类标签（热血、冒险等）
                currentMangaCategory != null -> {
                    repository.getMostReadManga(tags = currentMangaCategory, page = currentPage)
                }
                // 默认：最热
                else -> {
                    repository.getMostReadManga(page = currentPage)
                }
            }

            result.onSuccess { paged ->
                _ui.update {
                    it.copy(
                        mangaList = if (refresh) paged.items else it.mangaList + paged.items,
                        hasMore = paged.hasNext,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        error = e.message ?: "加载失败",
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (!_ui.value.isLoadingMore && _ui.value.hasMore) {
            currentPage++
            loadMangaList(refresh = false)
        }
    }

    /** 顶部 Tab 选择 */
    fun onCategorySelected(index: Int) {
        if (index == _ui.value.selectedCategoryIndex) return

        val catList = _ui.value.categories
        val catOffset = 1 + MANGA_CATEGORIES.size  // "全部" + 漫画分类 之后的才是后端分类

        currentCategoryName = null
        currentMangaCategory = null

        when {
            index == 0 -> {
                // "全部"：默认最热
            }
            index in 1..MANGA_CATEGORIES.size -> {
                // 漫画分类（热血、冒险等）
                currentMangaCategory = MANGA_CATEGORIES[index - 1]
            }
            index >= catOffset -> {
                // 后端下发的分类
                val catIdx = index - catOffset
                val cat = catList.getOrNull(catIdx)
                currentCategoryName = cat?.name
            }
        }

        _ui.update {
            it.copy(
                selectedCategoryIndex = index,
                selectedBackendCategoryIndex = 0,
                mangaList = emptyList()
            )
        }
        loadMangaList(refresh = true)
    }

    /** 后端分类的子分类选择（暂未启用，Category 模型不含 subCategories） */
    fun onBackendCategorySelected(subIndex: Int) {
        // 预留接口
    }

    fun refresh() {
        loadInitial()
    }

    fun loadRecommendations(mangaId: Long) {
        viewModelScope.launch {
            repository.getRandomBrowse(mangaId).onSuccess { list ->
                _ui.update { it.copy(recommendations = list.take(9)) }
            }
        }
    }

    companion object {
        val MANGA_CATEGORIES = listOf(
            "热血", "冒险", "悬疑", "奇幻", "科幻",
            "古风", "其他", "竞技", "玄幻"
        )
    }
}
