package cn.maga.lingdongmanhua.domain.model

/**
 * 漫画业务模型
 * 与 DTO 分离，代表 UI 层需要的数据结构
 */

// ===================== 漫画相关 =====================

/** 漫画基础信息（列表项/推荐） */
data class Manga(
    val id: Long,
    val name: String,
    val author: String,
    val coverUrl: String,
    val tags: List<String>,
    val status: MangaStatus,
    val description: String,
    val readCount: Int,
    val favoriteCount: Int,
    val chapterCount: Int,
    val lastUpdateChapterName: String?,
    val lastUpdateTime: Long?
)

enum class MangaStatus {
    SERIALIZING,   // 连载
    FINISHED,       // 完结
    UNKNOWN;

    companion object {
        fun fromInt(v: Int?) = when (v) {
            1 -> FINISHED
            0 -> SERIALIZING
            else -> UNKNOWN
        }
    }
}

/** 漫画详情（含章节列表） */
data class MangaDetail(
    val id: Long,
    val name: String,
    val author: String,
    val coverUrl: String,
    val tags: List<String>,
    val status: MangaStatus,
    val description: String,
    val readCount: Int,
    val favoriteCount: Int,
    val chapterCount: Int,
    val categoryName: String,
    val subCategoryName: String,
    val chapters: List<Chapter>
)

/** 章节 */
data class Chapter(
    val id: Long,
    val name: String,
    val chapterIndex: Int,
    val mangaId: Long,
    val mangaName: String,
    val coverUrl: String,
    val prevChapterId: Long?,
    val nextChapterId: Long?,
    val pages: List<ChapterPage>
)

/** 章节页 */
data class ChapterPage(
    val pageIndex: Int,
    val imageUrl: String
)

/** 搜索联想项 */
data class SearchSuggestion(
    val id: Long,
    val name: String,
    val author: String,
    val coverUrl: String,
    val isManga: Boolean
)

/** 首页 Banner 漫画 */
data class BannerManga(
    val id: Long,
    val name: String,
    val coverUrl: String
)

/** 分类 */
data class Category(
    val id: Long,
    val name: String,
    val icon: String?
)

/** 子分类 */
data class SubCategory(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String
)

/** 排行榜类型 */
enum class RankType {
    POPULAR,    // 人气榜
    COLLECT,    // 收藏榜
    HOT         // 热门榜
}

/** 性别筛选 */
enum class GenderFilter(val value: Int?) {
    ALL(null),
    MALE(0),
    FEMALE(1)
}
