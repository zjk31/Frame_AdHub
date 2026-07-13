package cn.android.adhub.data.download

import android.content.Context
import android.os.Environment
import cn.android.adhub.data.local.DownloadDao
import cn.android.adhub.data.local.DownloadTaskEntity
import cn.android.adhub.data.local.DownloadedChapterEntity
import cn.android.adhub.data.local.DownloadedPageEntity
import cn.android.adhub.data.local.MangaDatabase
import cn.android.adhub.domain.model.Chapter
import cn.android.adhub.domain.model.ChapterPage
import cn.android.adhub.domain.repository.ManAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 漫画下载管理器
 * 负责章节图片下载、缓存管理、下载队列
 */
class DownloadManager(
    private val context: Context,
    private val repository: ManAppRepository,
    private val downloadDao: DownloadDao = MangaDatabase.getInstance(context).downloadDao()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _activeDownloads = MutableStateFlow<Map<Long, DownloadTaskEntity>>(emptyMap())
    val activeDownloads: StateFlow<Map<Long, DownloadTaskEntity>> = _activeDownloads.asStateFlow()

    val downloadTasks: Flow<List<DownloadTaskEntity>> = downloadDao.observeDownloadTasks()
    val downloadedChapters: Flow<List<DownloadedChapterEntity>> = downloadDao.observeDownloadedChapters()

    /** 下载根目录 */
    private val downloadRoot: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "manga_downloads").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 获取漫画下载目录
     */
    private fun getMangaDir(mangaId: Long, mangaName: String): File {
        val dir = File(downloadRoot, "${mangaId}_${sanitizeFileName(mangaName)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取章节下载目录
     */
    private fun getChapterDir(mangaId: Long, mangaName: String, chapterId: Long, chapterName: String): File {
        val dir = File(getMangaDir(mangaId, mangaName), "${chapterId}_${sanitizeFileName(chapterName)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 添加下载任务到队列
     */
    fun enqueueDownload(
        mangaId: Long,
        mangaName: String,
        chapter: Chapter
    ) {
        scope.launch {
            // 检查是否已存在
            val existing = downloadDao.getTask(chapter.id)
            if (existing != null && existing.status == DownloadTaskEntity.STATUS_COMPLETED) return@launch

            val task = DownloadTaskEntity(
                chapterId = chapter.id,
                mangaId = mangaId,
                mangaName = mangaName,
                chapterName = chapter.name,
                chapterIndex = chapter.chapterIndex,
                coverUrl = chapter.coverUrl,
                totalPages = chapter.pages.size,
                status = DownloadTaskEntity.STATUS_QUEUED
            )
            downloadDao.insertTask(task)
            processQueue()
        }
    }

    /**
     * 批量下载（整部漫画）
     */
    fun enqueueBatchDownload(
        mangaId: Long,
        mangaName: String,
        chapters: List<Chapter>
    ) {
        scope.launch {
            chapters.forEach { chapter ->
                val existing = downloadDao.getTask(chapter.id)
                if (existing == null || existing.status != DownloadTaskEntity.STATUS_COMPLETED) {
                    val task = DownloadTaskEntity(
                        chapterId = chapter.id,
                        mangaId = mangaId,
                        mangaName = mangaName,
                        chapterName = chapter.name,
                        chapterIndex = chapter.chapterIndex,
                        coverUrl = chapter.coverUrl,
                        totalPages = chapter.pages.size,
                        status = DownloadTaskEntity.STATUS_QUEUED
                    )
                    downloadDao.insertTask(task)
                }
            }
            processQueue()
        }
    }

    /**
     * 暂停下载
     */
    fun pauseDownload(chapterId: Long) {
        scope.launch {
            downloadDao.updateTaskStatus(chapterId, DownloadTaskEntity.STATUS_PAUSED, 0)
        }
    }

    /**
     * 恢复下载
     */
    fun resumeDownload(chapterId: Long) {
        scope.launch {
            downloadDao.updateTaskStatus(chapterId, DownloadTaskEntity.STATUS_QUEUED, 0)
            processQueue()
        }
    }

    /**
     * 取消/删除下载
     */
    fun cancelDownload(chapterId: Long) {
        scope.launch {
            val task = downloadDao.getTask(chapterId)
            val chapter = downloadDao.getDownloadedChapter(chapterId)

            // 删除本地文件
            chapter?.let {
                val dir = getChapterDir(it.mangaId, it.mangaName, chapterId, it.chapterName)
                dir.deleteRecursively()
            }

            downloadDao.deleteDownloadedPages(chapterId)
            downloadDao.deleteDownloadedChapter(chapterId)
            downloadDao.deleteTask(chapterId)

            // 如果 task 没有对应的 downloaded chapter，也要清理目录
            task?.let { t ->
                val dir = getChapterDir(t.mangaId, t.mangaName, chapterId, t.chapterName)
                if (dir.exists()) dir.deleteRecursively()
            }
        }
    }

    /**
     * 删除漫画所有下载
     */
    fun deleteMangaDownloads(mangaId: Long) {
        scope.launch {
            val chapters = downloadDao.getDownloadedChapters(mangaId)
            chapters.forEach { chapter ->
                val dir = getChapterDir(mangaId, chapter.mangaName, chapter.chapterId, chapter.chapterName)
                dir.deleteRecursively()
                downloadDao.deleteDownloadedPages(chapter.chapterId)
            }
            downloadDao.deleteDownloadedChaptersByManga(mangaId)
            downloadDao.deleteTasksByManga(mangaId)
        }
    }

    /**
     * 处理下载队列
     */
    private suspend fun processQueue() {
        mutex.withLock {
            val activeTasks = downloadDao.getActiveTasks()
            if (activeTasks.isEmpty()) return@withLock

            // 逐个处理（单线程下载避免带宽竞争）
            for (task in activeTasks) {
                if (task.status == DownloadTaskEntity.STATUS_PAUSED) continue

                downloadChapter(task)
            }
        }
    }

    /**
     * 下载单个章节
     */
    private suspend fun downloadChapter(task: DownloadTaskEntity) {
        try {
            downloadDao.updateTaskStatus(task.chapterId, DownloadTaskEntity.STATUS_DOWNLOADING, 0)

            // 获取章节详情
            val chapterResult = repository.getChapter(task.chapterId)
            val chapter = chapterResult.getOrNull() ?: run {
                downloadDao.updateTaskError(task.chapterId, DownloadTaskEntity.STATUS_FAILED, "获取章节失败")
                return
            }

            val chapterDir = getChapterDir(task.mangaId, task.mangaName, task.chapterId, task.chapterName)

            // 创建已下载章节记录
            val downloadedChapter = DownloadedChapterEntity(
                chapterId = task.chapterId,
                mangaId = task.mangaId,
                mangaName = task.mangaName,
                chapterName = task.chapterName,
                chapterIndex = task.chapterIndex,
                coverUrl = task.coverUrl,
                totalPages = chapter.pages.size,
                downloadedPages = 0,
                downloadPath = chapterDir.absolutePath
            )
            downloadDao.insertDownloadedChapter(downloadedChapter)

            // 逐页下载
            val pages = chapter.pages
            val pageEntities = mutableListOf<DownloadedPageEntity>()

            for ((index, page) in pages.withIndex()) {
                // 检查是否暂停
                val current = downloadDao.getTask(task.chapterId)
                if (current?.status == DownloadTaskEntity.STATUS_PAUSED) return

                val localPath = downloadImage(page.imageUrl, chapterDir, task.chapterId, page.pageIndex)
                if (localPath != null) {
                    val pageEntity = DownloadedPageEntity(
                        chapterId = task.chapterId,
                        pageIndex = page.pageIndex,
                        localPath = localPath,
                        originalUrl = page.imageUrl
                    )
                    pageEntities.add(pageEntity)
                    downloadDao.insertDownloadedPage(pageEntity)
                }

                // 更新进度
                val progress = ((index + 1) * 100 / pages.size)
                downloadDao.updateTaskStatus(task.chapterId, DownloadTaskEntity.STATUS_DOWNLOADING, progress)
                downloadDao.updateDownloadedPages(task.chapterId, index + 1)
            }

            // 标记完成
            downloadDao.updateTaskStatus(task.chapterId, DownloadTaskEntity.STATUS_COMPLETED, 100)

        } catch (e: Exception) {
            downloadDao.updateTaskError(task.chapterId, DownloadTaskEntity.STATUS_FAILED, e.message ?: "下载失败")
        }
    }

    /**
     * 下载单张图片
     */
    private fun downloadImage(url: String, dir: File, chapterId: Long, pageIndex: Int): String? {
        if (url.isBlank()) return null

        val fileName = "page_${pageIndex.toString().padStart(4, '0')}.jpg"
        val targetFile = File(dir, fileName)

        // 如果已存在，跳过
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile.absolutePath
        }

        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取已下载章节的页面路径列表
     */
    suspend fun getLocalPages(chapterId: Long): List<ChapterPage> {
        val pages = downloadDao.getDownloadedPages(chapterId)
        return pages.sortedBy { it.pageIndex }.map { page ->
            ChapterPage(
                pageIndex = page.pageIndex,
                imageUrl = "file://${page.localPath}"
            )
        }
    }

    /**
     * 检查章节是否已下载
     */
    suspend fun isChapterDownloaded(chapterId: Long): Boolean {
        val task = downloadDao.getTask(chapterId)
        return task?.status == DownloadTaskEntity.STATUS_COMPLETED
    }

    /**
     * 获取下载统计
     */
    suspend fun getDownloadStats(): DownloadStats {
        return DownloadStats(
            mangaCount = downloadDao.getDownloadedMangaCount(),
            chapterCount = downloadDao.getDownloadedChapterCount()
        )
    }

    /**
     * 计算下载目录总大小
     */
    fun getDownloadSize(): Long {
        return downloadRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 清理所有下载
     */
    fun clearAllDownloads() {
        scope.launch {
            val chapters = downloadDao.observeDownloadedChapters()
            downloadRoot.deleteRecursively()
            downloadRoot.mkdirs()
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^\\w\\u4e00-\\u9fa5\\-]"), "_").take(50)
    }
}

/**
 * 下载统计数据
 */
data class DownloadStats(
    val mangaCount: Int,
    val chapterCount: Int
)
