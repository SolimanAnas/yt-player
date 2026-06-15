package com.solimananas.ytbrowser.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.solimananas.ytbrowser.App
import com.solimananas.ytbrowser.MainActivity
import com.solimananas.ytbrowser.R

class MediaPlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    private val binder = LocalBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    var currentTitle: String = "YT Browser"
        private set
    var currentArtist: String = ""
        private set
    var isPlaying: Boolean = false
        private set

    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { resumePlayback() }
        override fun onPause() { pausePlayback() }
        override fun onStop() { stopSelf() }
        override fun onSeekTo(pos: Long) {
            mediaSession.setPlaybackState(
                stateBuilder().setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    pos, 1f
                ).build()
            )
        }
        override fun onSkipToNext() {
            broadcastAction(ACTION_NEXT)
        }
        override fun onSkipToPrevious() {
            broadcastAction(ACTION_PREVIOUS)
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pausePlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
            AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService()!!
        setupMediaSession()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: currentArtist
                startPlayback(title, artist)
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP -> stopSelf()
            ACTION_NEXT -> broadcastAction(ACTION_NEXT)
            ACTION_PREVIOUS -> broadcastAction(ACTION_PREVIOUS)
        }
        return START_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "YTBrowserSession").apply {
            setCallback(callback)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    fun startPlayback(title: String, artist: String) {
        currentTitle = title
        currentArtist = artist
        isPlaying = true

        requestAudioFocus()
        updateMediaSession()
        showNotification()
    }

    fun pausePlayback() {
        isPlaying = false
        updateMediaSession()
        showNotification()
    }

    fun resumePlayback() {
        isPlaying = true
        requestAudioFocus()
        updateMediaSession()
        showNotification()
    }

    fun updateMetadata(title: String, artist: String) {
        currentTitle = title
        currentArtist = artist
        updateMediaSession()
        if (isPlaying) showNotification()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun updateMediaSession() {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "YouTube")
                .build()
        )
        mediaSession.setPlaybackState(
            stateBuilder().setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
            ).build()
        )
    }

    private fun stateBuilder() = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PAUSE or
        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
        PlaybackStateCompat.ACTION_SEEK_TO or
        PlaybackStateCompat.ACTION_STOP
    )

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                buildActionIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                buildActionIntent(ACTION_RESUME)
            )
        }

        val notification = NotificationCompat.Builder(this, App.CHANNEL_MEDIA_PLAYBACK)
            .setSmallIcon(R.drawable.ic_browser_logo)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_media_previous, "Previous",
                buildActionIntent(ACTION_PREVIOUS)
            )
            .addAction(playPauseAction)
            .addAction(
                android.R.drawable.ic_media_next, "Next",
                buildActionIntent(ACTION_NEXT)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastAction(action: String) {
        sendBroadcast(Intent(action).apply {
            `package` = packageName
        })
    }

    override fun onDestroy() {
        abandonAudioFocus()
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.solimananas.ytbrowser.PLAY"
        const val ACTION_PAUSE = "com.solimananas.ytbrowser.PAUSE"
        const val ACTION_RESUME = "com.solimananas.ytbrowser.RESUME"
        const val ACTION_STOP = "com.solimananas.ytbrowser.STOP"
        const val ACTION_NEXT = "com.solimananas.ytbrowser.NEXT"
        const val ACTION_PREVIOUS = "com.solimananas.ytbrowser.PREVIOUS"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
    }
}
