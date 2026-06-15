package com.solimananas.ytbrowser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.solimananas.ytbrowser.data.db.BrowserDatabase
import com.solimananas.ytbrowser.data.repository.BookmarkRepository
import com.solimananas.ytbrowser.data.repository.HistoryRepository
import com.solimananas.ytbrowser.data.repository.SettingsRepository
import com.solimananas.ytbrowser.engine.AdBlockEngine
import com.solimananas.ytbrowser.engine.FilterListManager

class App : Application() {

    val database by lazy { BrowserDatabase.getInstance(this) }
    val bookmarkRepository by lazy { BookmarkRepository(database.bookmarkDao()) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val adBlockEngine by lazy { AdBlockEngine(this) }
    val filterListManager by lazy { FilterListManager(this, adBlockEngine) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        adBlockEngine.initialize()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                CHANNEL_MEDIA_PLAYBACK,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background media playback"
                setShowBadge(false)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Download progress and completion"
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General browser notifications"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        lateinit var instance: App
            private set

        const val CHANNEL_MEDIA_PLAYBACK = "media_playback"
        const val CHANNEL_DOWNLOADS = "downloads"
        const val CHANNEL_GENERAL = "general"
    }
}
