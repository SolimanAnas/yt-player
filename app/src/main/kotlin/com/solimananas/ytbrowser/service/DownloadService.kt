package com.solimananas.ytbrowser.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.solimananas.ytbrowser.App
import com.solimananas.ytbrowser.R
import com.solimananas.ytbrowser.data.db.BrowserDatabase
import com.solimananas.ytbrowser.data.model.Download
import com.solimananas.ytbrowser.data.model.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = mutableMapOf<Long, Job>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val database by lazy { BrowserDatabase.getInstance(this) }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "download"
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "application/octet-stream"
                startDownload(url, fileName, mimeType)
            }
            ACTION_PAUSE -> {
                val id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) pauseDownload(id)
            }
            ACTION_RESUME -> {
                val id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) resumeDownload(id)
            }
            ACTION_CANCEL -> {
                val id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) cancelDownload(id)
            }
        }
        return START_STICKY
    }

    private fun startDownload(url: String, fileName: String, mimeType: String) {
        scope.launch {
            val downloadDir = getExternalFilesDir(null) ?: filesDir
            val filePath = File(downloadDir, fileName).absolutePath

            val download = Download(
                url = url,
                fileName = fileName,
                filePath = filePath,
                mimeType = mimeType,
                status = DownloadStatus.DOWNLOADING
            )
            val id = database.downloadDao().insert(download)

            showProgressNotification(id, fileName, 0, 0)

            activeDownloads[id] = launch {
                performDownload(id, url, filePath, fileName)
            }
        }
    }

    private suspend fun performDownload(id: Long, url: String, filePath: String, fileName: String) {
        runCatching {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateStatus(id, DownloadStatus.FAILED)
                    return
                }

                val total = response.body?.contentLength() ?: -1L
                val file = File(filePath)
                var downloaded = 0L

                FileOutputStream(file).use { output ->
                    response.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(8192)
                        var bytes: Int
                        var lastNotify = 0L
                        while (input.read(buffer).also { bytes = it } != -1) {
                            if (!isActive) {
                                updateStatus(id, DownloadStatus.PAUSED)
                                return
                            }
                            output.write(buffer, 0, bytes)
                            downloaded += bytes

                            val now = System.currentTimeMillis()
                            if (now - lastNotify > 500) {
                                lastNotify = now
                                database.downloadDao().updateProgress(
                                    id, DownloadStatus.DOWNLOADING, downloaded
                                )
                                val progress = if (total > 0) (downloaded * 100 / total).toInt() else -1
                                showProgressNotification(id, fileName, progress, total)
                            }
                        }
                    }
                }
                updateStatus(id, DownloadStatus.COMPLETED)
                showCompletionNotification(id, fileName, filePath)
            }
        }.onFailure {
            updateStatus(id, DownloadStatus.FAILED)
        }
    }

    private fun pauseDownload(id: Long) {
        activeDownloads[id]?.cancel()
        activeDownloads.remove(id)
        scope.launch { updateStatus(id, DownloadStatus.PAUSED) }
    }

    private fun resumeDownload(id: Long) {
        scope.launch {
            val download = database.downloadDao().getById(id) ?: return@launch
            activeDownloads[id] = launch {
                performDownload(id, download.url, download.filePath, download.fileName)
            }
        }
    }

    private fun cancelDownload(id: Long) {
        activeDownloads[id]?.cancel()
        activeDownloads.remove(id)
        scope.launch {
            val download = database.downloadDao().getById(id) ?: return@launch
            File(download.filePath).delete()
            updateStatus(id, DownloadStatus.CANCELLED)
        }
    }

    private suspend fun updateStatus(id: Long, status: DownloadStatus) {
        val download = database.downloadDao().getById(id) ?: return
        database.downloadDao().update(
            download.copy(
                status = status,
                completedAt = if (status == DownloadStatus.COMPLETED) System.currentTimeMillis() else null
            )
        )
        if (activeDownloads.isEmpty()) stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun showProgressNotification(id: Long, fileName: String, progress: Int, total: Long) {
        val notification = NotificationCompat.Builder(this, App.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $fileName")
            .setContentText(if (total > 0) "${formatBytes(total)} total" else "Downloading...")
            .setProgress(100, progress.coerceIn(0, 100), progress < 0)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_delete, "Cancel",
                buildActionIntent(ACTION_CANCEL, id)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_BASE_ID + id.toInt(), notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_BASE_ID + id.toInt(), notification)
        }
    }

    private fun showCompletionNotification(id: Long, fileName: String, filePath: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, App.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_BASE_ID + id.toInt() + 10000, notification)
    }

    private fun buildActionIntent(action: String, downloadId: Long): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        return PendingIntent.getService(
            this, action.hashCode() + downloadId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_BASE_ID = 2000
        const val ACTION_DOWNLOAD = "com.solimananas.ytbrowser.DOWNLOAD"
        const val ACTION_PAUSE = "com.solimananas.ytbrowser.DOWNLOAD_PAUSE"
        const val ACTION_RESUME = "com.solimananas.ytbrowser.DOWNLOAD_RESUME"
        const val ACTION_CANCEL = "com.solimananas.ytbrowser.DOWNLOAD_CANCEL"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
