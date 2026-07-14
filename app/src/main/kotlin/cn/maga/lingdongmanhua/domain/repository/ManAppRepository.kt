package cn.maga.lingdongmanhua.domain.repository

import cn.maga.lingdongmanhua.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 漫画 App Repository 接口
 * 定义业务层对数据的需求，解耦数据来源（网络/本地/缓存）
 */
interface ManAppRepository {

    // ===================== 漫画 =====================

    /** 最受欢迎漫画（首页） */
    suspend fun getMostReadManga(
        gender: Int? = null,
        tags: String? = null,
        page: Int = 1
    ): Result<PagedResult<Manga>>

    /** 人气榜 */
    suspend fun getPopularRank(gender: Int? = null): Result<List<Manga>>

    /** 收藏榜 */
    suspend fun getCollectRank(gender: Int? = null): Result<List<Manga>>

    /** 热门榜 */
    suspend fun getHotRank(gender: Int? = null): Result<List<Manga>>

    /** 详情页随机推荐 */
    suspend fun getRandomBrowse(mangaId: Long): Result<List<Manga>>

    /** 漫画详情 */
    suspend fun getMangaDetail(id: Long): Result<MangaDetail>

    /** 二级分类漫画列表 */
    suspend fun getMangaBySubCategory(
        subCategoryName: String,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<PagedResult<Manga>>

    /** 搜索联想 */
    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<PagedResult<SearchSuggestion>>

    /** 阅读统计 */
    suspend fun getReadCount(mangaId: Long): Result<Int>

    /** 首页 Banner 列表 */
    suspend fun getHomeBanners(): Result<List<BannerManga>>

    /** 全局分类配置 */
    suspend fun getGlobalCategories(): Result<List<Category>>

    // ===================== 章节 =====================

    /** 章节详情 */
    suspend fun getChapter(id: Long): Result<Chapter>

    // ===================== 用户 =====================

    /** 登录 */
    suspend fun login(username: String, password: String): Result<LoginResult>

    /** 注册 */
    suspend fun register(
        username: String,
        password: String,
        checkPassword: String,
        deviceId: String? = null,
        inviterId: String? = null
    ): Result<Boolean>

    /** 修改密码 */
    suspend fun changePassword(
        username: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Boolean>

    /** 获取当前用户信息 */
    suspend fun getCurrentUser(): Result<User?>

    // ===================== 收藏 =====================

    /** 收藏列表 */
    suspend fun getFavorites(userId: Long, page: Int = 1): Result<PagedResult<FavoriteItem>>

    /** 收藏同步对齐 */
    suspend fun syncFavorites(userId: Long, mangaIds: List<Long>): Result<List<FavoriteItem>>

    /** 批量删除收藏 */
    suspend fun batchDeleteFavorites(userId: Long, mangaIds: List<Long>): Result<Boolean>

    /** 获取收藏数 */
    suspend fun getFavoriteCount(mangaId: Long): Result<Int>

    /** 是否已收藏 */
    suspend fun isFavorite(userId: Long, mangaId: Long): Result<Boolean>

    /** 切换收藏状态 */
    suspend fun toggleFavorite(userId: Long, mangaId: Long): Result<Boolean>

    // ===================== 阅读历史 =====================

    /** 查询指定漫画阅读记录 */
    suspend fun queryHistory(mangaId: Long, userId: Long? = null): Result<ReadingHistory?>

    /** 历史列表 */
    suspend fun getHistoryList(userId: Long? = null): Result<List<ReadingHistory>>

    /** 清空历史 */
    suspend fun clearHistory(userId: Long? = null): Result<Boolean>

    /** 同步阅读进度 */
    suspend fun syncHistory(
        userId: Long? = null,
        items: List<Triple<Long, Long, Int>>  // mangaId, chapterId, page
    ): Result<Int>

    // ===================== 激励/奖励 =====================

    /** 获取激励数据 */
    suspend fun getReward(inviteCode: String): Result<RewardData>

    /** 是否显示激励广告 */
    suspend fun isReward(inviteCode: String): Result<Boolean>

    /** 获取下载配额 */
    suspend fun getDownloadQuota(inviteCode: String): Result<DownloadQuota>

    /** 扣减下载配额 */
    suspend fun deductDownloadQuota(inviteCode: String, count: Int): Result<Int>

    /** 纯净任务是否完成 */
    suspend fun isPureTaskCompleted(inviteCode: String): Result<Boolean>

    /** 每日签到 */
    suspend fun signIn(inviteCode: String, day: Int): Result<DownloadQuota>

    /** 激励任务完成通知 */
    suspend fun completeRewardTask(inviteCode: String, isTask: Boolean): Result<Boolean>

    /** 纯净任务完成通知 */
    suspend fun completePureTask(inviteCode: String): Result<Boolean>

    /** 观看激励广告后更新下载次数 */
    suspend fun updateDownloadCount(inviteCode: String): Result<DownloadQuota>

    // ===================== 消息 =====================

    /** 离线消息列表 */
    suspend fun getOfflineMessages(): Result<List<SystemMessage>>

    /** 未读消息数 */
    suspend fun getUnreadCount(): Result<Int>

    /** 确认消息已读 */
    suspend fun ackMessages(messageIds: List<String>): Result<Boolean>

    // ===================== 用户反馈 =====================

    /** 提交系统意见反馈 */
    suspend fun submitSystemFeedback(type: String, message: String): Result<Boolean>

    /** 提交找漫画反馈 */
    suspend fun submitFindBookFeedback(title: String, keyword: String? = null): Result<Boolean>

    /** 提交书籍纠错反馈 */
    suspend fun submitBookErrorFeedback(
        type: String,
        message: String,
        mangaId: Long
    ): Result<Boolean>

    // ===================== 版本更新 =====================

    data class AppUpdateInfo(
        val version: String,
        val versionCode: Int,
        val downloadUrl: String,
        val updateContent: String,
        val forceUpdate: Boolean
    )

    /** 检查版本更新 */
    suspend fun checkAppUpdate(
        packageName: String,
        version: String,
        buildNumber: String? = null
    ): Result<AppUpdateInfo?>

    // ===================== 分享 =====================

    /** 获取分享下载链接 */
    suspend fun getShareLink(inviteCode: String): Result<String>

    /** 已推广人数 */
    suspend fun getShareCodeCount(inviteCode: String): Result<Int>
}
