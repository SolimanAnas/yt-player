package com.solimananas.ytbrowser.engine

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class AdBlockEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val blockPatterns = mutableListOf<Regex>()
    private val siteExceptions = ConcurrentHashMap.newKeySet<String>()
    private val cosmeticFilters = ConcurrentHashMap<String, MutableList<String>>()

    val adsBlockedTotal = AtomicInteger(0)
    val trackersBlockedTotal = AtomicInteger(0)
    val bandwidthSavedBytes = AtomicLong(0L)

    // Per-tab counters: tabId -> count
    private val adsBlockedPerTab = ConcurrentHashMap<String, AtomicInteger>()
    private val trackersBlockedPerTab = ConcurrentHashMap<String, AtomicInteger>()

    fun initialize() {
        scope.launch {
            loadBuiltInLists()
        }
    }

    private fun loadBuiltInLists() {
        loadDomainList("adblock/ad-domains.txt")
        loadDomainList("adblock/tracker-domains.txt")
        loadFilterList("adblock/easylist-sample.txt")
    }

    fun reloadLists() {
        scope.launch {
            blockedDomains.clear()
            blockPatterns.clear()
            loadBuiltInLists()
        }
    }

    private fun loadDomainList(assetPath: String) {
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("!") }
                    .forEach { blockedDomains.add(it.lowercase()) }
            }
        }
    }

    private fun loadFilterList(assetPath: String) {
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { line -> parseFilterLine(line) }
            }
        }
    }

    fun loadFilterListFromText(text: String) {
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { parseFilterLine(it) }
    }

    private fun parseFilterLine(line: String) {
        when {
            line.startsWith("!") || line.startsWith("[") -> return  // comments
            line.startsWith("##") -> return  // global cosmetic (no domain)
            line.contains("##") -> {
                // Cosmetic filter: domain##selector
                val parts = line.split("##", limit = 2)
                val domains = parts[0].split(",")
                val selector = parts[1]
                domains.forEach { domain ->
                    cosmeticFilters.getOrPut(domain.trim().lowercase()) { mutableListOf() }
                        .add(selector)
                }
            }
            line.startsWith("||") -> {
                // Domain anchor: ||ads.example.com^
                val domain = line.removePrefix("||")
                    .split("^", "/", "$").first()
                    .lowercase()
                    .trim()
                if (domain.isNotEmpty()) blockedDomains.add(domain)
            }
            line.startsWith("|") -> {
                // URL anchor — convert to domain extraction
                val url = line.removePrefix("|")
                extractDomainFromUrl(url)?.let { blockedDomains.add(it) }
            }
            line.startsWith("/") && line.endsWith("/") -> {
                // Regex filter
                runCatching {
                    blockPatterns.add(Regex(line.removeSurrounding("/")))
                }
            }
            line.startsWith("@@") -> return  // exceptions - handled per-site
        }
    }

    fun shouldBlock(request: WebResourceRequest, pageUrl: String, tabId: String): Boolean {
        val url = request.url.toString()
        val host = request.url.host?.lowercase() ?: return false

        // Never block main-frame navigations
        if (request.isForMainFrame) return false

        // Check site exceptions
        val pageHost = extractDomainFromUrl(pageUrl) ?: ""
        if (siteExceptions.contains(pageHost)) return false

        // Check domain blocklist
        if (isDomainBlocked(host)) {
            recordBlock(url, tabId)
            return true
        }

        // Check regex patterns
        if (blockPatterns.any { it.containsMatchIn(url) }) {
            recordBlock(url, tabId)
            return true
        }

        return false
    }

    private fun isDomainBlocked(host: String): Boolean {
        if (blockedDomains.contains(host)) return true
        // Check parent domains
        var domain = host
        while (domain.contains('.')) {
            domain = domain.substringAfter('.')
            if (blockedDomains.contains(domain)) return true
        }
        return false
    }

    private fun recordBlock(url: String, tabId: String) {
        val isTracker = TRACKER_KEYWORDS.any { url.contains(it, ignoreCase = true) }
        if (isTracker) {
            trackersBlockedTotal.incrementAndGet()
            trackersBlockedPerTab.getOrPut(tabId) { AtomicInteger(0) }.incrementAndGet()
        } else {
            adsBlockedTotal.incrementAndGet()
            adsBlockedPerTab.getOrPut(tabId) { AtomicInteger(0) }.incrementAndGet()
        }
        bandwidthSavedBytes.addAndGet(AVERAGE_AD_SIZE_BYTES)
    }

    fun createBlockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "utf-8", 200, "OK",
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(ByteArray(0))
    )

    fun getCosmeticFilters(host: String): List<String> {
        val result = mutableListOf<String>()
        cosmeticFilters[host]?.let { result.addAll(it) }
        var domain = host
        while (domain.contains('.')) {
            domain = domain.substringAfter('.')
            cosmeticFilters[domain]?.let { result.addAll(it) }
        }
        return result
    }

    fun addSiteException(host: String) = siteExceptions.add(host.lowercase())
    fun removeSiteException(host: String) = siteExceptions.remove(host.lowercase())
    fun hasSiteException(host: String) = siteExceptions.contains(host.lowercase())

    fun getTabStats(tabId: String): Pair<Int, Int> {
        return Pair(
            adsBlockedPerTab[tabId]?.get() ?: 0,
            trackersBlockedPerTab[tabId]?.get() ?: 0
        )
    }

    fun clearTabStats(tabId: String) {
        adsBlockedPerTab.remove(tabId)
        trackersBlockedPerTab.remove(tabId)
    }

    private fun extractDomainFromUrl(url: String): String? = runCatching {
        URI(url).host?.lowercase()?.removePrefix("www.")
    }.getOrNull()

    companion object {
        private const val AVERAGE_AD_SIZE_BYTES = 50_000L
        private val TRACKER_KEYWORDS = listOf(
            "analytics", "tracking", "tracker", "beacon", "telemetry",
            "metrics", "stat", "pixel", "fingerprint", "collect"
        )
    }
}
