package com.netflix.moviebox.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.netflix.moviebox.api.MovieBoxClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class DownloadTask(
    val downloadId: Long,
    val title: String,
    val destinationPath: String,
    var status: String = "Queued",
    var progress: Int = 0
)

object MovieDownloader {

    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks

    suspend fun startDownload(
        context: Context,
        subjectId: String,
        movieTitle: String,
        directDownloadUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadUrl = directDownloadUrl ?: MovieBoxClient.extractStream(subjectId).fullStreamUrl ?: return@withContext false

        val sanitizedTitle = movieTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val fileName = "${sanitizedTitle}.mp4"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)

        val request = DownloadManager.Request(uri).apply {
            setTitle(movieTitle)
            setDescription("Downloading movie...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MovieBox/$fileName")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val id = downloadManager.enqueue(request)
        val task = DownloadTask(
            downloadId = id,
            title = movieTitle,
            destinationPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/MovieBox/$fileName"
        )

        _downloadTasks.value = _downloadTasks.value + task
        true
    }

    fun getCompletedDownloads(): List<File> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MovieBox")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.equals("mp4", ignoreCase = true) }?.toList() ?: emptyList()
    }
}
