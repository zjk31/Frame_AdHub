package cn.manxinghai.zhuimange.domain.model

// ===================== 用户相关 =====================

/** 当前登录用户 */
data class User(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatar: String?,
    val inviteCode: String,
    val gender: Int?,
    val phone: String?,
    val email: String?
)

/** 登录结果 */
data class LoginResult(
    val user: User,
    val token: String
)

/** 阅读历史记录 */
data class ReadingHistory(
    val mangaId: Long,
    val mangaName: String,
    val coverUrl: String,
    val lastChapterId: Long,
    val lastChapterName: String,
    val lastReadPage: Int,
    val chapterCount: Int,
    val updateTime: Long
)

// ===================== 书架 =====================

/** 书架收藏项 */
data class FavoriteItem(
    val mangaId: Long,
    val mangaName: String,
    val author: String,
    val coverUrl: String,
    val tags: List<String>,
    val status: MangaStatus,
    val lastUpdateChapterName: String?,
    val lastUpdateTime: Long?
)

// ===================== 激励/奖励相关 =====================

/** 任务项 */
data class TaskItem(
    val taskKey: String,
    val taskName: String,
    val completed: Boolean,
    val rewardAmount: Int
)

/** 奖励数据 */
data class RewardData(
    val todaySignedIn: Boolean,
    val signInDays: Int,
    val adFreeEndTime: String?,
    val downloadQuota: Int,
    val pureTaskCompleted: Boolean,
    val taskList: List<TaskItem>
)

/** 下载配额 */
data class DownloadQuota(
    val total: Int,
    val used: Int,
    val remaining: Int
)

// ===================== SSE 消息 =====================

/** 系统消息 */
data class SystemMessage(
    val id: String,
    val title: String,
    val content: String,
    val type: String,
    val createTime: Long
)

// ===================== 通用 =====================

/** 分页结果 */
data class PagedResult<T>(
    val items: List<T>,
    val current: Int,
    val total: Int,
    val pages: Int
) {
    val hasNext: Boolean get() = current < pages
}
