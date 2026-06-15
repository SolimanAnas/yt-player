package com.solimananas.ytbrowser.engine

import android.webkit.WebView

object YouTubeEnhancer {

    // JavaScript injected into YouTube pages for enhanced behavior
    private val BACKGROUND_PLAYBACK_JS = """
        (function() {
            // Override visibility API so YouTube thinks page is always visible
            const origHidden = Object.getOwnPropertyDescriptor(Document.prototype, 'hidden') ||
                               Object.getOwnPropertyDescriptor(HTMLDocument.prototype, 'hidden');
            Object.defineProperty(document, 'hidden', {
                get: function() { return false; },
                configurable: true
            });

            const origVisState = Object.getOwnPropertyDescriptor(Document.prototype, 'visibilityState') ||
                                 Object.getOwnPropertyDescriptor(HTMLDocument.prototype, 'visibilityState');
            Object.defineProperty(document, 'visibilityState', {
                get: function() { return 'visible'; },
                configurable: true
            });

            // Suppress visibilitychange events that YouTube uses to pause
            const origAdd = document.addEventListener.bind(document);
            document.addEventListener = function(type, listener, options) {
                if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                    console.log('[YTBrowser] Blocked visibilitychange listener');
                    return;
                }
                return origAdd(type, listener, options);
            };

            // Keep AudioContext running
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                const audioCtx = new AudioContext();
                const oscillator = audioCtx.createOscillator();
                const gainNode = audioCtx.createGain();
                gainNode.gain.value = 0; // Silent
                oscillator.connect(gainNode);
                gainNode.connect(audioCtx.destination);
                oscillator.start();
            }

            console.log('[YTBrowser] Background playback mode enabled');
        })();
    """.trimIndent()

    private val DISABLE_ADS_JS = """
        (function() {
            // Remove ad overlay elements after page load
            function removeAds() {
                const adSelectors = [
                    '.ad-showing',
                    '.ytp-ad-module',
                    '.video-ads',
                    '.ytp-ad-player-overlay',
                    '.ytp-ad-text-overlay',
                    '#masthead-ad',
                    '.ytd-banner-promo-renderer',
                    'ytd-ad-slot-renderer',
                    'ytd-promoted-sparkles-web-renderer',
                    '.ytd-rich-item-renderer[is-slim-media]',
                    'ytd-display-ad-renderer',
                    'ytd-statement-banner-renderer'
                ];
                adSelectors.forEach(sel => {
                    document.querySelectorAll(sel).forEach(el => el.remove());
                });

                // Skip ads automatically
                const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button');
                if (skipBtn) skipBtn.click();

                // If ad is playing, seek to end
                const video = document.querySelector('video');
                const adContainer = document.querySelector('.ad-showing');
                if (video && adContainer) {
                    video.currentTime = video.duration;
                }
            }

            // Run immediately and observe for dynamic ad insertion
            removeAds();
            const observer = new MutationObserver(removeAds);
            observer.observe(document.body || document.documentElement, {
                childList: true, subtree: true
            });
        })();
    """.trimIndent()

    private val MEDIA_SESSION_JS = """
        (function() {
            // Enhanced Media Session for lock-screen controls
            if ('mediaSession' in navigator) {
                const updateMetadata = () => {
                    const titleEl = document.querySelector('h1.ytd-watch-metadata yt-formatted-string, h1.title');
                    const channelEl = document.querySelector('#channel-name yt-formatted-string, .ytd-channel-name');
                    const thumbEl = document.querySelector('link[itemprop="thumbnailUrl"]') ||
                                   document.querySelector('meta[property="og:image"]');

                    const title = titleEl?.textContent?.trim() || document.title;
                    const artist = channelEl?.textContent?.trim() || 'YouTube';
                    const artwork = thumbEl?.getAttribute('href') || thumbEl?.getAttribute('content');

                    navigator.mediaSession.metadata = new MediaMetadata({
                        title: title,
                        artist: artist,
                        artwork: artwork ? [{ src: artwork, sizes: '512x512', type: 'image/jpeg' }] : []
                    });
                };

                // Update on navigation
                let lastUrl = location.href;
                new MutationObserver(() => {
                    if (location.href !== lastUrl) {
                        lastUrl = location.href;
                        setTimeout(updateMetadata, 1500);
                    }
                }).observe(document, { subtree: true, childList: true });

                setTimeout(updateMetadata, 2000);

                // Wire action handlers to YouTube's player
                const getVideo = () => document.querySelector('video');

                navigator.mediaSession.setActionHandler('play', () => getVideo()?.play());
                navigator.mediaSession.setActionHandler('pause', () => getVideo()?.pause());
                navigator.mediaSession.setActionHandler('seekbackward', (e) => {
                    const v = getVideo();
                    if (v) v.currentTime = Math.max(0, v.currentTime - (e.seekOffset || 10));
                });
                navigator.mediaSession.setActionHandler('seekforward', (e) => {
                    const v = getVideo();
                    if (v) v.currentTime = Math.min(v.duration, v.currentTime + (e.seekOffset || 10));
                });
                navigator.mediaSession.setActionHandler('previoustrack', () => {
                    document.querySelector('.ytp-prev-button')?.click();
                });
                navigator.mediaSession.setActionHandler('nexttrack', () => {
                    document.querySelector('.ytp-next-button')?.click();
                });

                console.log('[YTBrowser] Media Session handlers registered');
            }
        })();
    """.trimIndent()

    private val FINGERPRINT_PROTECTION_JS = """
        (function() {
            // Canvas fingerprint protection
            const origGetContext = HTMLCanvasElement.prototype.getContext;
            HTMLCanvasElement.prototype.getContext = function(type, attrs) {
                const ctx = origGetContext.call(this, type, attrs);
                if (type === '2d' && ctx) {
                    const origGetImageData = ctx.getImageData.bind(ctx);
                    ctx.getImageData = function(x, y, w, h) {
                        const data = origGetImageData(x, y, w, h);
                        for (let i = 0; i < data.data.length; i += 100) {
                            data.data[i] = data.data[i] ^ 1;
                        }
                        return data;
                    };
                }
                return ctx;
            };

            // AudioContext fingerprint protection
            const OrigAudioContext = window.AudioContext || window.webkitAudioContext;
            if (OrigAudioContext) {
                const origCreateAnalyser = OrigAudioContext.prototype.createAnalyser;
                OrigAudioContext.prototype.createAnalyser = function() {
                    const analyser = origCreateAnalyser.call(this);
                    const origGetFloat = analyser.getFloatFrequencyData.bind(analyser);
                    analyser.getFloatFrequencyData = function(array) {
                        origGetFloat(array);
                        for (let i = 0; i < array.length; i++) {
                            array[i] += (Math.random() - 0.5) * 0.001;
                        }
                    };
                    return analyser;
                };
            }

            // WebGL fingerprint protection
            const origGetParam = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(param) {
                if (param === 37445) return 'Intel Inc.';  // UNMASKED_VENDOR_WEBGL
                if (param === 37446) return 'Intel Iris OpenGL';  // UNMASKED_RENDERER_WEBGL
                return origGetParam.call(this, param);
            };

            console.log('[YTBrowser] Fingerprint protection active');
        })();
    """.trimIndent()

    private val READER_MODE_JS = """
        (function() {
            // Simple reader mode extraction
            const article = document.querySelector('article') ||
                           document.querySelector('[role="main"]') ||
                           document.querySelector('.content') ||
                           document.querySelector('#content') ||
                           document.querySelector('main');

            if (!article) return;

            const title = document.querySelector('h1')?.textContent || document.title;
            const content = article.innerHTML;

            document.body.innerHTML = `
                <style>
                    body { max-width: 700px; margin: 0 auto; padding: 20px; font-family: Georgia, serif;
                           font-size: 18px; line-height: 1.7; color: #333; background: #fafafa; }
                    h1 { font-size: 28px; margin-bottom: 20px; }
                    img { max-width: 100%; height: auto; }
                    @media (prefers-color-scheme: dark) {
                        body { background: #1a1a1a; color: #e0e0e0; }
                    }
                </style>
                <h1>${'$'}{title}</h1>
                ${'$'}{content}
            `;
        })();
    """.trimIndent()

    fun injectBackgroundPlayback(webView: WebView) {
        webView.evaluateJavascript(BACKGROUND_PLAYBACK_JS, null)
    }

    fun injectAdRemoval(webView: WebView) {
        webView.evaluateJavascript(DISABLE_ADS_JS, null)
    }

    fun injectMediaSession(webView: WebView) {
        webView.evaluateJavascript(MEDIA_SESSION_JS, null)
    }

    fun injectFingerprintProtection(webView: WebView) {
        webView.evaluateJavascript(FINGERPRINT_PROTECTION_JS, null)
    }

    fun enableReaderMode(webView: WebView) {
        webView.evaluateJavascript(READER_MODE_JS, null)
    }

    fun isYouTubePage(url: String?): Boolean {
        if (url == null) return false
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    fun getYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("/embed/([a-zA-Z0-9_-]{11})"),
            Regex("/shorts/([a-zA-Z0-9_-]{11})"),
            Regex("/live/([a-zA-Z0-9_-]{11})")
        )
        patterns.forEach { pattern ->
            pattern.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    fun buildDesktopUserAgent(): String =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun buildMobileUserAgent(): String =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36"
}
