package com.solimananas.ytbrowser.engine

import android.content.Context
import com.solimananas.ytbrowser.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FilterListManager(
    private val context: Context,
    private val adBlockEngine: AdBlockEngine,
    private val settingsRepository: SettingsRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val knownLists = listOf(
        FilterList("EasyList", EASYLIST_URL, true),
        FilterList("EasyPrivacy", EASYPRIVACY_URL, true),
        FilterList("Fanboy Annoyances", FANBOY_URL, false),
        FilterList("uBlock Filters", UBLOCK_URL, false),
        FilterList("Peter Lowe Ad List", PETERLO_URL, false)
    )

    fun updateAllLists(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            var success = true
            knownLists.filter { it.enabled }.forEach { list ->
                if (!downloadAndApply(list.url)) success = false
            }
            settingsRepository?.updateFilterListLastUpdated(System.currentTimeMillis())
            onComplete?.invoke(success)
        }
    }

    fun updateList(url: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = downloadAndApply(url)
            onComplete?.invoke(result)
        }
    }

    private suspend fun downloadAndApply(url: String): Boolean {
        return runCatching {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val text = response.body?.string() ?: return false
                    adBlockEngine.loadFilterListFromText(text)
                    saveListCache(url, text)
                    true
                } else false
            }
        }.getOrDefault(false)
    }

    private fun saveListCache(url: String, content: String) {
        runCatching {
            val fileName = url.hashCode().toString() + ".txt"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        }
    }

    fun loadCachedLists() {
        scope.launch {
            context.fileList().filter { it.endsWith(".txt") }.forEach { name ->
                runCatching {
                    context.openFileInput(name).bufferedReader().use { reader ->
                        adBlockEngine.loadFilterListFromText(reader.readText())
                    }
                }
            }
        }
    }

    companion object {
        const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
        const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
        const val FANBOY_URL = "https://easylist.to/easylist/fanboy-annoyance.txt"
        const val UBLOCK_URL = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"
        const val PETERLO_URL = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0"
    }
}

data class FilterList(
    val name: String,
    val url: String,
    val enabled: Boolean
)
