package cn.android.adhub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载的章节实体
 */
@Entity(tableName = "downloaded_chapters")
data class DownloadedChapterEntity(
    @PrimaryKey
    val chapterId: Long,
    val mangaId: Long,
    val mangaName: String,
    val chapterName: String,
    val chapterIndex: Int,
    val coverUrl: String,
    val totalPages: Int,
    val downloadedPages: Int = 0,
    val downloadTime: Long = System.currentTimeMillis(),
    val downloadPath: String
)

/**
 * 下载的页面图片实体
 */
@Entity(tableName = "downloaded_pages")
data class DownloadedPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val pageIndex: Int,
    val localPath: String,
    val originalUrl: String
)

/**
 * 下载任务实体（进行中/排队/失败）
 */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val chapterId: Long,
    val mangaId: Long,
    val mangaName: String,
    val chapterName: String,
    val chapterIndex: Int,
    val coverUrl: String,
    val totalPages: Int,
    val status: Int,    // 0=排队, 1=下载中, 2=完成, 3=失败, 4=暂停
    val progress: Int = 0,  // 0-100
    val errorMessage: String? = null,
    val createTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_QUEUED = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_FAILED = 3
        const val STATUS_PAUSED = 4
    }
}
