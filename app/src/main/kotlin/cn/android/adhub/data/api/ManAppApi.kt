package cn.android.adhub.data.api

import cn.android.adhub.data.dto.*
import retrofit2.http.*

/**
 * 漫画 App API 接口
 * Base URL: http://192.168.2.184:8123
 *
 * 文档: 漫画功能文档.md
 */
interface ManAppApi {

    // ===================== 漫画相关（9个） =====================

    @GET("api/manga/getmostreadmanga")
    suspend fun getMostReadManga(
        @Query("gender") gender: Int? = null,
        @Query("tags") tags: String? = null,
        @Query("current") current: Int = 1
    ): ApiResponse<List<MangaMostReadDto>>

    @GET("api/manga/getpopularrankmanga")
    suspend fun getPopularRankManga(
        @Query("gender") gender: Int? = null
    ): ApiResponse<List<MangaMostReadDto>>

    @GET("api/manga/getcollectrankmanga")
    suspend fun getCollectRankManga(
        @Query("gender") gender: Int? = null
    ): ApiResponse<List<MangaMostReadDto>>

    @GET("api/manga/gethotrankmanga")
    suspend fun getHotRankManga(
        @Query("gender") gender: Int? = null
    ): ApiResponse<List<MangaMostReadDto>>

    @GET("api/manga/getrandombrowse")
    suspend fun getRandomBrowse(
        @Query("mangaId") mangaId: Long
    ): ApiResponse<List<MangaMostReadDto>>

    @GET("api/manga/{id}")
    suspend fun getMangaDetail(
        @Path("id") id: Long
    ): ApiResponse<MangaDetailDto>

    @GET("api/subcategory/getbysubcategoryid")
    suspend fun getBySubCategoryId(
        @Query("subCategoryName") subCategoryName: String,
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<PageDto<MangaMostReadDto>>

    @GET("api/manga/searchlanv2")
    suspend fun searchLanV2(
        @Query("keyword") keyword: String,
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<PageDto<SearchSuggestionDto>>

    @GET("api/mangareadstat/getReadCount")
    suspend fun getReadCount(
        @Query("mangaId") mangaId: Long
    ): ApiResponse<Int>

    // ===================== 章节相关（1个） =====================

    @GET("api/chapter/getchapterbyid")
    suspend fun getChapterById(
        @Query("id") id: Long
    ): ApiResponse<MangaChapterVo>

    // ===================== 用户相关（17个） =====================

    @POST("api/user/login")
    suspend fun login(
        @Body body: LoginRequest
    ): ApiResponse<UserLoginResult>

    @POST("api/user/registerDirect")
    suspend fun register(
        @Body body: RegisterRequest
    ): ApiResponse<Boolean>

    @POST("api/user/changepassword")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest
    ): ApiResponse<Boolean>

    @GET("api/user/getcurrentuser")
    suspend fun getCurrentUser(): ApiResponse<ManappUserInfo?>

    @GET("api/userfavorite/list")
    suspend fun getFavoriteList(
        @Query("userId") userId: Long,
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<PageDto<MangaVoDto>>

    @POST("api/userfavorite/sync")
    suspend fun syncFavorite(
        @Query("userId") userId: Long,
        @Body mangaIds: List<Long>
    ): ApiResponse<List<MangaVoDto>>

    @HTTP(method = "DELETE", path = "api/userfavorite/batchDelete", hasBody = true)
    suspend fun batchDeleteFavorite(
        @Query("userId") userId: Long,
        @Body mangaIds: List<Long>
    ): ApiResponse<Boolean>

    @GET("api/userfavorite/count")
    suspend fun getFavoriteCount(
        @Query("mangaId") mangaId: Long
    ): ApiResponse<Int>

    @GET("api/userfavorite/isfavorite")
    suspend fun isFavorite(
        @Query("userId") userId: Long,
        @Query("mangaId") mangaId: Long
    ): ApiResponse<Boolean>

    @POST("api/userfavorite/toggle")
    suspend fun toggleFavorite(
        @Query("userId") userId: Long,
        @Query("mangaId") mangaId: Long
    ): ApiResponse<Boolean>

    @GET("api/userhistory/query")
    suspend fun queryHistory(
        @Query("mangaId") mangaId: Long,
        @Query("userId") userId: Long? = null,
        @Query("userCode") userCode: String? = null
    ): ApiResponse<UserHistoryDto?>

    @GET("api/userhistory/historyList")
    suspend fun getHistoryList(
        @Query("userId") userId: Long? = null,
        @Query("userCode") userCode: String? = null
    ): ApiResponse<List<UserMangaHistoryVoDto>>

    @DELETE("api/userhistory/clearHistory")
    suspend fun clearHistory(
        @Query("userId") userId: Long? = null,
        @Query("userCode") userCode: String? = null
    ): ApiResponse<Boolean>

    @POST("api/userhistory/sync")
    suspend fun syncHistory(
        @Query("userId") userId: Long? = null,
        @Query("userCode") userCode: String? = null,
        @Body items: List<HistorySyncItem>
    ): ApiResponse<Int>

    @GET("api/user/shareDownloadLink")
    suspend fun getShareDownloadLink(
        @Query("inviteCode") inviteCode: String,
        @Query("share") share: Boolean = true
    ): ApiResponse<ShareLinkDto>

    @GET("api/codeInstall/getShareCodeCount")
    suspend fun getShareCodeCount(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<Int>

    @Multipart
    @POST("api/user/uploadAvatar")
    suspend fun uploadAvatar(
        @Part file: okhttp3.MultipartBody.Part
    ): ApiResponse<String>

    // ===================== 激励/奖励相关（11个） =====================

    @GET("api/reward/get")
    suspend fun getReward(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<RewardVo>

    @GET("api/reward/isReward")
    suspend fun isReward(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<Boolean>

    @GET("api/reward/getRewardDownloadQuota")
    suspend fun getRewardDownloadQuota(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<RewardDownloadQuotaVo>

    @POST("api/reward/deductRewardDownloadQuota")
    suspend fun deductDownloadQuota(
        @Query("inviteCode") inviteCode: String,
        @Query("downloadCount") downloadCount: Int
    ): ApiResponse<Int>

    @GET("api/reward/isPureTaskCompleted")
    suspend fun isPureTaskCompleted(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<Boolean>

    @GET("api/reward/getRewardTaskValidity")
    suspend fun getRewardTaskValidity(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<String>

    @POST("api/reward/signIn")
    suspend fun signIn(
        @Query("inviteCode") inviteCode: String,
        @Query("day") day: Int
    ): ApiResponse<RewardDownloadQuotaVo>

    @POST("api/reward/complete")
    suspend fun completeRewardTask(
        @Query("inviteCode") inviteCode: String,
        @Query("isTask") isTask: Int
    ): ApiResponse<Boolean>

    @POST("api/reward/completePureTask")
    suspend fun completePureTask(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<Boolean>

    @GET("api/reward/updateDownloadCount")
    suspend fun updateDownloadCount(
        @Query("inviteCode") inviteCode: String
    ): ApiResponse<RewardDownloadQuotaVo>

    @GET("api/reward/rewardPending")
    suspend fun rewardPending(
        @Query("user_id") userId: String,
        @Query("reward_type") rewardType: String
    ): ApiResponse<Boolean>

    // ===================== SSE 推送（4个） =====================

    @GET("api/sse/offline/messages")
    suspend fun getOfflineMessages(): ApiResponse<List<SseMessageDto>>

    @GET("api/sse/offline/unread-count")
    suspend fun getUnreadCount(): ApiResponse<Int>

    @POST("api/sse/offline/ack")
    suspend fun ackMessages(
        @Body messageIds: List<String>
    ): ApiResponse<Boolean>

    // ===================== 全局设置（2个） =====================

    @GET("api/globalSetting/get")
    suspend fun getGlobalSetting(): ApiResponse<GlobalSettings>

    @GET("api/globalSetting/getHomeBannerMangaList")
    suspend fun getHomeBannerMangaList(): ApiResponse<List<MangaVoDto>>

    // ===================== 用户反馈（3个） =====================

    @POST("api/user/feedback/add/system")
    suspend fun submitSystemFeedback(
        @Body body: SystemFeedbackRequest
    ): ApiResponse<Boolean>

    @POST("api/user/feedback/add/findbook")
    suspend fun submitFindBookFeedback(
        @Body body: FindBookFeedbackRequest
    ): ApiResponse<Boolean>

    @POST("api/user/feedback/add/bookerror")
    suspend fun submitBookErrorFeedback(
        @Body body: BookErrorFeedbackRequest
    ): ApiResponse<Boolean>

    // ===================== 其他（3个） =====================

    @GET("api/updateinfo/getNextVersionApk")
    suspend fun getNextVersionApk(
        @Query("appName") appName: String? = null,
        @Query("packageName") packageName: String,
        @Query("version") version: String,
        @Query("buildNumber") buildNumber: String? = null
    ): ApiResponse<AppUpdateVo>

    @GET("api/posts/{id}")
    suspend fun getPost(
        @Path("id") id: Long
    ): ApiResponse<PostDto>
}
