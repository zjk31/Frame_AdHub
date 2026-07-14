package cn.maga.lingdongmanhua.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 下载缓存 DAO
 */
@Dao
interface DownloadDao {

    // ===================== 下载任务 =====================

    @Query("SELECT * FROM download_tasks ORDER BY createTime DESC")
    fun observeDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status != 2 ORDER BY createTime DESC")
    suspend fun getActiveTasks(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE chapterId = :chapterId")
    suspend fun getTask(chapterId: Long): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, progress = :progress WHERE chapterId = :chapterId")
    suspend fun updateTaskStatus(chapterId: Long, status: Int, progress: Int)

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :error WHERE chapterId = :chapterId")
    suspend fun updateTaskError(chapterId: Long, status: Int, error: String?)

    @Query("DELETE FROM download_tasks WHERE chapterId = :chapterId")
    suspend fun deleteTask(chapterId: Long)

    @Query("DELETE FROM download_tasks WHERE mangaId = :mangaId")
    suspend fun deleteTasksByManga(mangaId: Long)

    // ===================== 已下载章节 =====================

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId ORDER BY chapterIndex ASC")
    suspend fun getDownloadedChapters(mangaId: Long): List<DownloadedChapterEntity>

    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadTime DESC")
    fun observeDownloadedChapters(): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE chapterId = :chapterId")
    suspend fun getDownloadedChapter(chapterId: Long): DownloadedChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedChapter(chapter: DownloadedChapterEntity)

    @Query("UPDATE downloaded_chapters SET downloadedPages = :pages WHERE chapterId = :chapterId")
    suspend fun updateDownloadedPages(chapterId: Long, pages: Int)

    @Query("DELETE FROM downloaded_chapters WHERE chapterId = :chapterId")
    suspend fun deleteDownloadedChapter(chapterId: Long)

    @Query("DELETE FROM downloaded_chapters WHERE mangaId = :mangaId")
    suspend fun deleteDownloadedChaptersByManga(mangaId: Long)

    @Query("SELECT COUNT(*) FROM downloaded_chapters")
    suspend fun getDownloadedChapterCount(): Int

    // ===================== 已下载页面 =====================

    @Query("SELECT * FROM downloaded_pages WHERE chapterId = :chapterId ORDER BY pageIndex ASC")
    suspend fun getDownloadedPages(chapterId: Long): List<DownloadedPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedPage(page: DownloadedPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedPages(pages: List<DownloadedPageEntity>)

    @Query("DELETE FROM downloaded_pages WHERE chapterId = :chapterId")
    suspend fun deleteDownloadedPages(chapterId: Long)

    @Query("SELECT COUNT(*) FROM downloaded_pages WHERE chapterId = :chapterId")
    suspend fun getDownloadedPageCount(chapterId: Long): Int

    // ===================== 统计 =====================

    @Query("""
        SELECT COUNT(DISTINCT mangaId) FROM downloaded_chapters
    """)
    suspend fun getDownloadedMangaCount(): Int
}
