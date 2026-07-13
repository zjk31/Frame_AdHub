package cn.android.adhub.data.repository

import cn.android.adhub.data.dto.*
import cn.android.adhub.domain.model.*

/** DTO → Domain 模型映射器 */
object MangaMapper {

    fun MangaMostReadDto.toDomain() = Manga(
        id = id,
        name = name,
        author = author ?: "",
        coverUrl = coverUrl ?: "",
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        status = MangaStatus.fromInt(status),
        description = description ?: "",
        readCount = readCount ?: 0,
        favoriteCount = favoriteCount ?: 0,
        chapterCount = chapterCount ?: 0,
        lastUpdateChapterName = lastUpdateChapterName,
        lastUpdateTime = lastUpdateTime
    )

    fun MangaDetailDto.toDomain() = MangaDetail(
        id = id,
        name = name,
        author = author ?: "",
        coverUrl = coverUrl ?: "",
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        status = MangaStatus.fromInt(status),
        description = description ?: "",
        readCount = readCount ?: 0,
        favoriteCount = favoriteCount ?: 0,
        chapterCount = chapterCount ?: 0,
        categoryName = categoryName ?: "",
        subCategoryName = subCategoryName ?: "",
        chapters = chapterList?.map { it.toDomain() } ?: emptyList()
    )

    fun ChapterBriefDto.toDomain() = Chapter(
        id = id,
        name = name ?: "",
        chapterIndex = chapterIndex ?: 0,
        mangaId = 0,
        mangaName = "",
        coverUrl = "",
        prevChapterId = null,
        nextChapterId = null,
        pages = emptyList()
    )

    fun MangaChapterVo.toDomain() = Chapter(
        id = id,
        name = name ?: "",
        chapterIndex = chapterIndex ?: 0,
        mangaId = mangaId,
        mangaName = mangaName ?: "",
        coverUrl = coverUrl ?: "",
        prevChapterId = prevChapterId,
        nextChapterId = nextChapterId,
        pages = pageList?.map { it.toDomain() } ?: emptyList()
    )

    fun ChapterPageDto.toDomain() = ChapterPage(
        pageIndex = pageIndex,
        imageUrl = imageUrl ?: ""
    )

    fun SearchSuggestionDto.toDomain() = SearchSuggestion(
        id = id,
        name = name,
        author = author ?: "",
        coverUrl = coverUrl ?: "",
        isManga = type == 1
    )

    fun MangaVoDto.toFavoriteItem() = FavoriteItem(
        mangaId = id,
        mangaName = name,
        author = author ?: "",
        coverUrl = coverUrl ?: "",
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        status = MangaStatus.fromInt(status),
        lastUpdateChapterName = lastUpdateChapterName,
        lastUpdateTime = lastUpdateTime
    )

    fun UserLoginResult.toLoginResult() = LoginResult(
        user = User(
            id = userId,
            username = username ?: "",
            nickname = nickname ?: username ?: "",
            avatar = avatar,
            inviteCode = inviteCode ?: "",
            gender = null,
            phone = null,
            email = null
        ),
        token = token ?: ""
    )

    fun ManappUserInfo.toDomain() = User(
        id = id,
        username = username ?: "",
        nickname = nickname ?: username ?: "",
        avatar = avatar,
        inviteCode = inviteCode ?: "",
        gender = gender,
        phone = phone,
        email = email
    )

    fun UserMangaHistoryVoDto.toDomain() = ReadingHistory(
        mangaId = mangaId,
        mangaName = mangaName ?: "",
        coverUrl = coverUrl ?: "",
        lastChapterId = lastChapterId ?: 0,
        lastChapterName = lastChapterName ?: "",
        lastReadPage = lastReadPage ?: 0,
        chapterCount = chapterCount ?: 0,
        updateTime = updateTime ?: 0
    )

    fun RewardVo.toDomain() = RewardData(
        todaySignedIn = todaySignIn ?: false,
        signInDays = signInDays ?: 0,
        adFreeEndTime = adFreeEndTime,
        downloadQuota = downloadQuota ?: 0,
        pureTaskCompleted = pureTaskCompleted ?: false,
        taskList = taskList?.map { it.toDomain() } ?: emptyList()
    )

    fun TaskItemDto.toDomain() = TaskItem(
        taskKey = taskKey ?: "",
        taskName = taskName ?: "",
        completed = completed ?: false,
        rewardAmount = rewardAmount ?: 0
    )

    fun RewardDownloadQuotaVo.toDomain() = DownloadQuota(
        total = totalQuota ?: 0,
        used = usedQuota ?: 0,
        remaining = remainingQuota ?: 0
    )

    fun MangaVoDto.toBanner() = BannerManga(
        id = id,
        name = name,
        coverUrl = coverUrl ?: ""
    )

    fun CategoryDto.toDomain() = Category(
        id = id,
        name = name,
        icon = icon
    )

    fun GlobalSettings.toCategories() = categories?.map { it.toDomain() } ?: emptyList()

    fun <S, T> PageDto<S>.toPaged(items: List<T>, current: Int): PagedResult<T> = PagedResult(
        items = items,
        current = current,
        total = total,
        pages = pages,
    )
}
