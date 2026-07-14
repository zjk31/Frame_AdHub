package cn.maga.lingdongmanhua.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.download.DownloadManager
import cn.maga.lingdongmanhua.data.download.DownloadStats
import cn.maga.lingdongmanhua.data.local.DownloadTaskEntity
import cn.maga.lingdongmanhua.data.local.DownloadedChapterEntity
import cn.maga.lingdongmanhua.data.repository.UserSession
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadUiState(
    val isLoading: Boolean = true,
    val activeTasks: List<DownloadTaskEntity> = emptyList(),
    val completedTasks: List<DownloadTaskEntity> = emptyList(),
    val downloadedChapters: List<DownloadedChapterEntity> = emptyList(),
    val downloadStats: DownloadStats = DownloadStats(0, 0),
    val downloadSizeBytes: Long = 0L,
    val downloadQuota: cn.maga.lingdongmanhua.domain.model.DownloadQuota? = null,
    val error: String? = null
)

class DownloadViewModel(
    private val downloadManager: DownloadManager,
    private val userSession: UserSession,
    private val repository: ManAppRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(DownloadUiState())
    val ui: StateFlow<DownloadUiState> = _ui.asStateFlow()

    init {
        observeDownloads()
        loadQuota()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadManager.downloadTasks.collect { tasks ->
                val active = tasks.filter {
                    it.status != DownloadTaskEntity.STATUS_COMPLETED
                }
                val completed = tasks.filter {
                    it.status == DownloadTaskEntity.STATUS_COMPLETED
                }
                _ui.update {
                    it.copy(
                        activeTasks = active,
                        completedTasks = completed,
                        isLoading = false,
                        downloadSizeBytes = downloadManager.getDownloadSize()
                    )
                }
            }
        }

        viewModelScope.launch {
            downloadManager.downloadedChapters.collect { chapters ->
                _ui.update { it.copy(downloadedChapters = chapters) }
            }
        }
    }

    private fun loadQuota() {
        viewModelScope.launch {
            val user = userSession.currentUser.let { it.value }
            if (user != null) {
                repository.getDownloadQuota(user.inviteCode).onSuccess { quota ->
                    _ui.update { it.copy(downloadQuota = quota) }
                }
            }
        }
    }

    fun pauseDownload(chapterId: Long) {
        downloadManager.pauseDownload(chapterId)
    }

    fun resumeDownload(chapterId: Long) {
        downloadManager.resumeDownload(chapterId)
    }

    fun cancelDownload(chapterId: Long) {
        downloadManager.cancelDownload(chapterId)
    }

    fun deleteMangaDownloads(mangaId: Long) {
        downloadManager.deleteMangaDownloads(mangaId)
    }

    fun clearAllDownloads() {
        downloadManager.clearAllDownloads()
    }

    fun refresh() {
        loadQuota()
    }
}
