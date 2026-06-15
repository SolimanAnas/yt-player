package com.solimananas.ytbrowser

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.solimananas.ytbrowser.ui.BrowserApp
import com.solimananas.ytbrowser.ui.theme.YTBrowserTheme
import com.solimananas.ytbrowser.viewmodel.BrowserViewModel
import com.solimananas.ytbrowser.viewmodel.BrowserViewModelFactory
import com.solimananas.ytbrowser.viewmodel.SettingsViewModel
import com.solimananas.ytbrowser.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {

    private val app get() = application as App

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(app.settingsRepository)
    }

    private val browserViewModel: BrowserViewModel by viewModels {
        BrowserViewModelFactory(
            app.bookmarkRepository,
            app.historyRepository,
            app.adBlockEngine,
            app.settingsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()

            YTBrowserTheme(
                darkTheme = when (settings.themeMode) {
                    "dark", "amoled" -> true
                    "light" -> false
                    else -> systemDark
                },
                amoled = settings.themeMode == "amoled",
                dynamicColor = settings.dynamicColors
            ) {
                BrowserApp(
                    browserViewModel = browserViewModel,
                    settingsViewModel = settingsViewModel,
                    onEnterPiP = { enterPiPMode() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.data?.toString()
        if (!url.isNullOrEmpty()) {
            browserViewModel.loadUrl(url)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (browserViewModel.isVideoPlaying() && supportsPiP()) {
            enterPiPMode()
        }
    }

    private fun enterPiPMode() {
        if (!supportsPiP()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun supportsPiP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        browserViewModel.onPiPModeChanged(isInPictureInPictureMode)
    }

    override fun onBackPressed() {
        if (!browserViewModel.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
