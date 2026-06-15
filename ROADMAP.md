# YT Browser — Roadmap

## Vision

A privacy-focused, Chromium-based Android browser that makes YouTube the best it can be — background audio, no ads, PiP, lock-screen controls — while providing a clean, fast, Material Design 3 browsing experience for all websites.

---

## v0.1 — Foundation (Current)

**Status:** ✅ Architecture complete, code generated, not yet compiled

Deliverables:
- Full Android project scaffolding (Kotlin, Jetpack Compose, Material 3)
- WebView-based browser engine (Chromium WebView)
- Basic tab management (open, close, switch)
- Address bar with URL input + search
- Top bar + Bottom navigation toolbar
- Ad/tracker blocking via bundled domain lists + EasyList subset
- Background playback via JavaScript visibility override
- Media Session API integration (lock-screen controls)
- Picture-in-Picture entry point
- Bookmarks (Room DB)
- History (Room DB)
- Settings with DataStore persistence
- Shields panel (per-page stats)
- Download infrastructure (OkHttp)
- Incognito mode (no history/cookie persistence)
- HTTPS-only mode
- Fingerprint protection (Canvas/WebGL/AudioContext noise)
- Dark/AMOLED/Dynamic Color themes

---

## v0.2 — YouTube First (Q3 2026)

Focus: Make the YouTube experience excellent.

- [ ] Auto-enter PiP when YouTube video is playing and user leaves app
- [ ] PiP controls: play/pause, next/previous via custom PiP actions (Android 12+)
- [ ] Lock-screen media controls with video thumbnail artwork
- [ ] Screen-off playback verified on Android 13/14
- [ ] YouTube ad skip automation (auto-click skip button)
- [ ] YouTube Shorts background playback
- [ ] Playlist awareness in media session (previous/next)
- [ ] New Tab page with YouTube quick access
- [ ] Download manager UI (list active/complete downloads)

---

## v0.3 — Polish & Performance (Q4 2026)

Focus: Stability, UX refinements, performance tuning.

- [ ] Tab thumbnails (bitmap snapshots)
- [ ] Swipe-to-close tabs
- [ ] Tab groups with color labels
- [ ] Recently closed tabs (10 items)
- [ ] Address bar autocomplete (history + bookmarks)
- [ ] Filter list auto-update (WorkManager daily job)
- [ ] Per-site permission management UI
- [ ] Page translation (Google Translate endpoint)
- [ ] QR code scanner
- [ ] Accessibility improvements (TalkBack, large text)
- [ ] Battery optimization: pause background WebView rendering
- [ ] Memory profiling and WebView lifecycle fixes
- [ ] Full-screen video mode with proper system bar handling

---

## v0.4 — Privacy Deep Dive (Q1 2027)

Focus: Privacy features comparable to Brave's full shield set.

- [ ] DNS-over-HTTPS (Cloudflare / Quad9 / Google)
- [ ] Anti-bounce-tracking (clear storage after cross-site redirects)
- [ ] Global Privacy Control (GPC) header injection
- [ ] CNAME uncloaking (detect CNAME-based tracker bypasses)
- [ ] Referrer trimming (strip query params from Referer header)
- [ ] User-Agent randomization option
- [ ] Cookie consent automatic rejection (CMP bypass)
- [ ] Privacy report (weekly stats: ads blocked, trackers blocked, bandwidth saved)
- [ ] Private DNS + DoH settings UI
- [ ] Tor integration research (proxy to Orbot)

---

## v0.5 — Power User Features (Q2 2027)

- [ ] Custom filter list subscriptions (uBlock-compatible)
- [ ] Bookmark folders and editing
- [ ] Password manager with biometric unlock (EncryptedSharedPreferences)
- [ ] Autofill service
- [ ] Browser sync (bookmarks, history, settings)
- [ ] Import/export bookmarks (HTML format)
- [ ] Reader mode (built-in, without JS)
- [ ] Custom themes (user color picker)
- [ ] Desktop mode improvements (better UA switching, viewport)
- [ ] Multi-window for Android tablets

---

## v1.0 — Stable Public Release (Q3 2027)

Full-featured, polished, stable browser ready for the Play Store.

Requirements:
- All v0.x features stable and tested
- Play Store listing and privacy policy
- F-Droid compatibility (no proprietary SDKs in core)
- Automated test suite (unit + UI)
- ProGuard-optimized release build
- Crash reporting (optional, privacy-preserving)
- Localization: English, Arabic, Spanish, French, German

---

## Future Exploration (Post-v1.0)

### Extensions
Building Chromium from source to enable extension support is a significant undertaking (100+ GB build, weeks of work). The plan:
1. Fork Chromium's Android shell (`chrome/android`)
2. Enable extension APIs (`extensions/browser/`)
3. Ship a curated extension gallery (uBlock Origin, Dark Reader, etc.)

This is a v2.0+ feature.

### Upstream Chromium Engine
Replace WebView with a custom Chromium build to:
- Control update cadence
- Enable extension support
- Remove WebView's privacy-leaking components
- Enable custom certificate pinning and TLS configuration

Architecture would follow Brave Android's approach (build on top of `//chrome/android`).

---

## Architecture Notes

### Current Engine: Android System WebView
- Chromium-based on Android 5.0+
- Updated independently via Play Store
- Limited API surface (no extension support, no CDP)
- Good enough for v0.x releases

### Target Engine: Custom Chromium Build
- Full Chromium source integration
- Chrome DevTools Protocol (CDP) for debugging
- Extension loading APIs
- Custom network stack for DNS-over-HTTPS

### State Management
- Jetpack Compose + StateFlow (current)
- Will migrate to MVI pattern for v0.3
- Room for persistent storage
- DataStore Preferences for settings

### Content Blocking
- Currently: domain list + EasyList pattern matching in WebViewClient
- Target: Network-level filtering via VpnService (like Brave's Rust-based ad blocker)
- Will implement: Bloom filter for O(1) domain lookup at scale
