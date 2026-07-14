package cn.maga.lingdongmanhua.data.dto

import com.google.gson.annotations.SerializedName

// ===================== 通用 =====================

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
) {
    val isSuccess: Boolean get() = code == 0
}

data class PageDto<T>(
    @SerializedName("records") val records: List<T>,
    @SerializedName("total") val total: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("current") val current: Int,
    @SerializedName("pages") val pages: Int
)

// ===================== 漫画相关 =====================

data class MangaMostReadDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("author") val author: String?,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("status") val status: Int?,   // 0=连载, 1=完结
    @SerializedName("description") val description: String?,
    @SerializedName("readCount") val readCount: Int?,
    @SerializedName("favoriteCount") val favoriteCount: Int?,
    @SerializedName("chapterCount") val chapterCount: Int?,
    @SerializedName("lastUpdateChapterName") val lastUpdateChapterName: String?,
    @SerializedName("lastUpdateTime") val lastUpdateTime: Long?
)

data class MangaDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("author") val author: String?,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("status") val status: Int?,
    @SerializedName("description") val description: String?,
    @SerializedName("readCount") val readCount: Int?,
    @SerializedName("favoriteCount") val favoriteCount: Int?,
    @SerializedName("chapterCount") val chapterCount: Int?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("subCategoryName") val subCategoryName: String?,
    @SerializedName("chapterList") val chapterList: List<ChapterBriefDto>?
)

data class ChapterBriefDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("chapterIndex") val chapterIndex: Int?
)

data class MangaChapterVo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("chapterIndex") val chapterIndex: Int?,
    @SerializedName("mangaId") val mangaId: Long,
    @SerializedName("mangaName") val mangaName: String?,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("prevChapterId") val prevChapterId: Long?,
    @SerializedName("nextChapterId") val nextChapterId: Long?,
    @SerializedName("pageList") val pageList: List<ChapterPageDto>?
)

data class ChapterPageDto(
    @SerializedName("pageIndex") val pageIndex: Int,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class SearchSuggestionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("author") val author: String?,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("type") val type: Int?  // 1=漫画
)

// 分类漫画结果（同 MangaMostReadDto）
typealias MangaVoDto = MangaMostReadDto

// ===================== 用户相关 =====================

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("checkpassword") val checkPassword: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("inviterId") val inviterId: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("checkpassword") val checkPassword: String
)

data class UserLoginResult(
    @SerializedName("userId") val userId: Long,
    @SerializedName("username") val username: String?,
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("inviteCode") val inviteCode: String?,
    @SerializedName("token") val token: String?
)

data class ManappUserInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String?,
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("inviteCode") val inviteCode: String?,
    @SerializedName("gender") val gender: Int?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?
)

data class UserHistoryDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("mangaId") val mangaId: Long,
    @SerializedName("chapterId") val chapterId: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("updateTime") val updateTime: Long?
)

data class UserMangaHistoryVoDto(
    @SerializedName("mangaId") val mangaId: Long,
    @SerializedName("mangaName") val mangaName: String?,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("lastChapterId") val lastChapterId: Long?,
    @SerializedName("lastChapterName") val lastChapterName: String?,
    @SerializedName("lastReadPage") val lastReadPage: Int?,
    @SerializedName("updateTime") val updateTime: Long?,
    @SerializedName("chapterCount") val chapterCount: Int?
)

data class HistorySyncItem(
    @SerializedName("mangaId") val mangaId: Long,
    @SerializedName("chapterId") val chapterId: Long,
    @SerializedName("page") val page: Int
)

data class ShareLinkDto(
    @SerializedName("shareUrl") val shareUrl: String?
)

// ===================== 激励/奖励相关 =====================

data class RewardVo(
    @SerializedName("todaySignIn") val todaySignIn: Boolean?,
    @SerializedName("signInDays") val signInDays: Int?,
    @SerializedName("adFreeEndTime") val adFreeEndTime: String?,
    @SerializedName("downloadQuota") val downloadQuota: Int?,
    @SerializedName("pureTaskCompleted") val pureTaskCompleted: Boolean?,
    @SerializedName("taskList") val taskList: List<TaskItemDto>?
)

data class TaskItemDto(
    @SerializedName("taskKey") val taskKey: String?,
    @SerializedName("taskName") val taskName: String?,
    @SerializedName("completed") val completed: Boolean?,
    @SerializedName("rewardAmount") val rewardAmount: Int?
)

data class RewardDownloadQuotaVo(
    @SerializedName("totalQuota") val totalQuota: Int?,
    @SerializedName("usedQuota") val usedQuota: Int?,
    @SerializedName("remainingQuota") val remainingQuota: Int?
)

// ===================== SSE 推送 =====================

data class SseMessageDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("createTime") val createTime: Long?
)

// ===================== 全局设置 =====================

data class GlobalSettings(
    @SerializedName("adEnabled") val adEnabled: Boolean?,
    @SerializedName("categories") val categories: List<CategoryDto>?,
    @SerializedName("subCategories") val subCategories: List<SubCategoryDto>?
)

data class CategoryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String?
)

data class SubCategoryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("categoryId") val categoryId: Long?,
    @SerializedName("categoryName") val categoryName: String?
)

// ===================== 用户反馈 =====================

data class SystemFeedbackRequest(
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String,
    @SerializedName("inviteCode") val inviteCode: String
)

data class FindBookFeedbackRequest(
    @SerializedName("title") val title: String,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("keyword") val keyword: String? = null
)

data class BookErrorFeedbackRequest(
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("mangaId") val mangaId: Long
)

// ===================== 其他 =====================

data class AppUpdateVo(
    @SerializedName("version") val version: String?,
    @SerializedName("versionCode") val versionCode: Int?,
    @SerializedName("downloadUrl") val downloadUrl: String?,
    @SerializedName("updateContent") val updateContent: String?,
    @SerializedName("forceUpdate") val forceUpdate: Boolean?
)

data class PostDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("createTime") val createTime: Long?
)
