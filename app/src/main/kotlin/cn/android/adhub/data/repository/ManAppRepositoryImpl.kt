package cn.android.adhub.data.repository

import cn.android.adhub.data.api.ManAppApi
import cn.android.adhub.data.dto.*
import cn.android.adhub.domain.model.*
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * ManAppRepository 实现
 * 通过 Retrofit 调用后端 API，DTO → Domain 模型转换
 */
class ManAppRepositoryImpl(
    private val api: ManAppApi,
    private val userSession: UserSession          // 注入用户态（token / userId）
) : ManAppRepository {

    private val mapper = MangaMapper

    // ===================== 漫画 =====================

    override suspend fun getMostReadManga(
        gender: Int?,
        tags: String?,
        page: Int
    ): Result<PagedResult<Manga>> = runCatching {
        val resp = api.getMostReadManga(gender, tags, page)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        val items = resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
        PagedResult(items, page, items.size, 1)
    }

    override suspend fun getPopularRank(gender: Int?): Result<List<Manga>> = runCatching {
        val resp = api.getPopularRankManga(gender)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
    }

    override suspend fun getCollectRank(gender: Int?): Result<List<Manga>> = runCatching {
        val resp = api.getCollectRankManga(gender)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
    }

    override suspend fun getHotRank(gender: Int?): Result<List<Manga>> = runCatching {
        val resp = api.getHotRankManga(gender)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
    }

    override suspend fun getRandomBrowse(mangaId: Long): Result<List<Manga>> = runCatching {
        val resp = api.getRandomBrowse(mangaId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
    }

    override suspend fun getMangaDetail(id: Long): Result<MangaDetail> = runCatching {
        val resp = api.getMangaDetail(id)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    override suspend fun getMangaBySubCategory(
        subCategoryName: String,
        page: Int,
        pageSize: Int
    ): Result<PagedResult<Manga>> = runCatching {
        val resp = api.getBySubCategoryId(subCategoryName, page, pageSize)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        val dto = resp.data!!
        val items = dto.records.map { mapper.run { it.toDomain() } }
        PagedResult(items, dto.current, dto.total, dto.pages)
    }

    override suspend fun search(
        keyword: String,
        page: Int,
        pageSize: Int
    ): Result<PagedResult<SearchSuggestion>> = runCatching {
        val resp = api.searchLanV2(keyword, page, pageSize)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        val dto = resp.data!!
        val items = dto.records.map { mapper.run { it.toDomain() } }
        PagedResult(items, dto.current, dto.total, dto.pages)
    }

    override suspend fun getReadCount(mangaId: Long): Result<Int> = runCatching {
        val resp = api.getReadCount(mangaId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: 0
    }

    override suspend fun getHomeBanners(): Result<List<BannerManga>> = runCatching {
        val resp = api.getHomeBannerMangaList()
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toBanner() } } ?: emptyList()
    }

    override suspend fun getGlobalCategories(): Result<List<Category>> = runCatching {
        val resp = api.getGlobalSetting()
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toCategories() }
    }

    // ===================== 章节 =====================

    override suspend fun getChapter(id: Long): Result<Chapter> = runCatching {
        val resp = api.getChapterById(id)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    // ===================== 用户 =====================

    override suspend fun login(username: String, password: String): Result<LoginResult> = runCatching {
        val resp = api.login(LoginRequest(username, password))
        check(resp.isSuccess) { resp.message ?: "登录失败" }
        val data = resp.data!!
        val loginResult = mapper.run { data.toLoginResult() }
        userSession.saveUser(loginResult)
        loginResult
    }

    override suspend fun register(
        username: String,
        password: String,
        checkPassword: String,
        deviceId: String?,
        inviterId: String?
    ): Result<Boolean> = runCatching {
        val resp = api.register(RegisterRequest(username, password, checkPassword, deviceId, inviterId))
        check(resp.isSuccess) { resp.message ?: "注册失败" }
        resp.data ?: false
    }

    override suspend fun changePassword(
        username: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Boolean> = runCatching {
        val resp = api.changePassword(ChangePasswordRequest(username, newPassword, confirmPassword))
        check(resp.isSuccess) { resp.message ?: "修改失败" }
        resp.data ?: false
    }

    override suspend fun getCurrentUser(): Result<User?> = runCatching {
        userSession.currentUser.first()
    }

    // ===================== 收藏 =====================

    override suspend fun getFavorites(userId: Long, page: Int): Result<PagedResult<FavoriteItem>> = runCatching {
        val resp = api.getFavoriteList(userId, page)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        val dto = resp.data!!
        val items = dto.records.map { mapper.run { it.toFavoriteItem() } }
        PagedResult(items, dto.current, dto.total, dto.pages)
    }

    override suspend fun syncFavorites(userId: Long, mangaIds: List<Long>): Result<List<FavoriteItem>> = runCatching {
        val resp = api.syncFavorite(userId, mangaIds)
        check(resp.isSuccess) { resp.message ?: "同步失败" }
        resp.data?.map { mapper.run { it.toFavoriteItem() } } ?: emptyList()
    }

    override suspend fun batchDeleteFavorites(userId: Long, mangaIds: List<Long>): Result<Boolean> = runCatching {
        val resp = api.batchDeleteFavorite(userId, mangaIds)
        check(resp.isSuccess) { resp.message ?: "删除失败" }
        resp.data ?: false
    }

    override suspend fun getFavoriteCount(mangaId: Long): Result<Int> = runCatching {
        val resp = api.getFavoriteCount(mangaId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: 0
    }

    override suspend fun isFavorite(userId: Long, mangaId: Long): Result<Boolean> = runCatching {
        val resp = api.isFavorite(userId, mangaId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    override suspend fun toggleFavorite(userId: Long, mangaId: Long): Result<Boolean> = runCatching {
        val resp = api.toggleFavorite(userId, mangaId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    // ===================== 阅读历史 =====================

    override suspend fun queryHistory(mangaId: Long, userId: Long?): Result<ReadingHistory?> = runCatching {
        val resp = api.queryHistory(mangaId, userId = userId)
        if (!resp.isSuccess) return@runCatching null
        resp.data?.let { dto ->
            ReadingHistory(
                mangaId = dto.mangaId,
                mangaName = "",
                coverUrl = "",
                lastChapterId = dto.chapterId,
                lastChapterName = "",
                lastReadPage = dto.page,
                chapterCount = 0,
                updateTime = dto.updateTime ?: 0
            )
        }
    }
    override suspend fun getHistoryList(userId: Long?): Result<List<ReadingHistory>> = runCatching {
        val resp = api.getHistoryList(userId = userId)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map { mapper.run { it.toDomain() } } ?: emptyList()
    }

    override suspend fun clearHistory(userId: Long?): Result<Boolean> = runCatching {
        val resp = api.clearHistory(userId = userId)
        check(resp.isSuccess) { resp.message ?: "清除失败" }
        resp.data ?: false
    }

    override suspend fun syncHistory(userId: Long?, items: List<Triple<Long, Long, Int>>): Result<Int> = runCatching {
        val body = items.map { HistorySyncItem(it.first, it.second, it.third) }
        val resp = api.syncHistory(userId = userId, items = body)
        check(resp.isSuccess) { resp.message ?: "同步失败" }
        resp.data ?: 0
    }

    // ===================== 激励/奖励 =====================

    override suspend fun getReward(inviteCode: String): Result<RewardData> = runCatching {
        val resp = api.getReward(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    override suspend fun isReward(inviteCode: String): Result<Boolean> = runCatching {
        val resp = api.isReward(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    override suspend fun getDownloadQuota(inviteCode: String): Result<DownloadQuota> = runCatching {
        val resp = api.getRewardDownloadQuota(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    override suspend fun deductDownloadQuota(inviteCode: String, count: Int): Result<Int> = runCatching {
        val resp = api.deductDownloadQuota(inviteCode, count)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: 0
    }

    override suspend fun isPureTaskCompleted(inviteCode: String): Result<Boolean> = runCatching {
        val resp = api.isPureTaskCompleted(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    override suspend fun signIn(inviteCode: String, day: Int): Result<DownloadQuota> = runCatching {
        val resp = api.signIn(inviteCode, day)
        check(resp.isSuccess) { resp.message ?: "签到失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    override suspend fun completeRewardTask(inviteCode: String, isTask: Boolean): Result<Boolean> = runCatching {
        val resp = api.completeRewardTask(inviteCode, if (isTask) 1 else 0)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    override suspend fun completePureTask(inviteCode: String): Result<Boolean> = runCatching {
        val resp = api.completePureTask(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    override suspend fun updateDownloadCount(inviteCode: String): Result<DownloadQuota> = runCatching {
        val resp = api.updateDownloadCount(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        mapper.run { resp.data!!.toDomain() }
    }

    // ===================== 消息 =====================

    override suspend fun getOfflineMessages(): Result<List<SystemMessage>> = runCatching {
        val resp = api.getOfflineMessages()
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.map {
            SystemMessage(
                id = it.id ?: "",
                title = it.title ?: "",
                content = it.content ?: "",
                type = it.type ?: "",
                createTime = it.createTime ?: 0
            )
        } ?: emptyList()
    }

    override suspend fun getUnreadCount(): Result<Int> = runCatching {
        val resp = api.getUnreadCount()
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: 0
    }

    override suspend fun ackMessages(messageIds: List<String>): Result<Boolean> = runCatching {
        val resp = api.ackMessages(messageIds)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: false
    }

    // ===================== 用户反馈 =====================

    private suspend fun getInviteCode(): String {
        return userSession.currentUser.first()?.inviteCode ?: ""
    }

    override suspend fun submitSystemFeedback(type: String, message: String): Result<Boolean> = runCatching {
        val resp = api.submitSystemFeedback(SystemFeedbackRequest(type, message, getInviteCode()))
        check(resp.isSuccess) { resp.message ?: "提交失败" }
        resp.data ?: false
    }

    override suspend fun submitFindBookFeedback(title: String, keyword: String?): Result<Boolean> = runCatching {
        val resp = api.submitFindBookFeedback(FindBookFeedbackRequest(title, getInviteCode(), keyword))
        check(resp.isSuccess) { resp.message ?: "提交失败" }
        resp.data ?: false
    }

    override suspend fun submitBookErrorFeedback(type: String, message: String, mangaId: Long): Result<Boolean> = runCatching {
        val resp = api.submitBookErrorFeedback(BookErrorFeedbackRequest(type, message, getInviteCode(), mangaId))
        check(resp.isSuccess) { resp.message ?: "提交失败" }
        resp.data ?: false
    }

    // ===================== 版本更新 =====================

    override suspend fun checkAppUpdate(
        packageName: String,
        version: String,
        buildNumber: String?
    ): Result<ManAppRepository.AppUpdateInfo?> = runCatching {
        val resp = api.getNextVersionApk(packageName = packageName, version = version, buildNumber = buildNumber)
        if (!resp.isSuccess || resp.data == null) return@runCatching null
        val d = resp.data
        ManAppRepository.AppUpdateInfo(
            version = d.version ?: "",
            versionCode = d.versionCode ?: 0,
            downloadUrl = d.downloadUrl ?: "",
            updateContent = d.updateContent ?: "",
            forceUpdate = d.forceUpdate ?: false
        )
    }

    // ===================== 分享 =====================

    override suspend fun getShareLink(inviteCode: String): Result<String> = runCatching {
        val resp = api.getShareDownloadLink(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data?.shareUrl ?: ""
    }

    override suspend fun getShareCodeCount(inviteCode: String): Result<Int> = runCatching {
        val resp = api.getShareCodeCount(inviteCode)
        check(resp.isSuccess) { resp.message ?: "请求失败" }
        resp.data ?: 0
    }
}

/** UserSession - 用户会话管理器，管理当前登录用户的 token 和基本信息 */
class UserSession(
    private val tokenStore: TokenStore   // 复用广告聚合框架的 TokenStore
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun load() {
        tokenStore.getToken()?.let {
            val cached = tokenStore.getCachedUserInfo()
            _currentUser.value = cached
        }
    }

    fun saveUser(result: LoginResult) {
        tokenStore.saveToken(result.token)
        tokenStore.saveCachedUserInfo(result.user)
        _currentUser.value = result.user
    }

    fun logout() {
        tokenStore.clearToken()
        _currentUser.value = null
    }

    fun getToken(): String? = tokenStore.getToken()
}
