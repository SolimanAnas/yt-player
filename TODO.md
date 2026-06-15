# YT Browser — TODO

## Critical (Must fix before v1.0 release)

### Build & Architecture
- [ ] Add `gradlew` shell script to project root
- [ ] Resolve Accompanist deprecation: `SystemUiController` moved to Compose 1.5+
- [ ] Replace `BasicTextField` in AddressBar with standard Compose `TextField` with proper decorators
- [ ] Fix WebView `blockThirdPartyCookies` reference (requires settings to flow into WebViewClient)
- [ ] Add proper ViewModel injection via Hilt or manual DI instead of factory pattern
- [ ] Implement `onBackPressedDispatcher` properly (replace deprecated `onBackPressed()`)

### Features — In Progress
- [ ] Tab snapshot/thumbnail capture (requires off-screen bitmap rendering)
- [ ] Proper `about:blank` / New Tab homepage (currently shows blank WebView)
- [ ] Complete download manager UI (list, pause, resume, cancel from UI)
- [ ] Implement QR code scanner (ZXing or Google ML Kit)
- [ ] Implement page translation (Google Translate integration or built-in)
- [ ] Password manager / credential storage (EncryptedSharedPreferences)
- [ ] Autofill provider service implementation
- [ ] Sync engine (bookmarks/history sync via account)

### YouTube Specific
- [ ] Test background playback JS injection on latest YouTube web UI
- [ ] PiP mode: auto-enter when a YouTube video is playing and user presses home
- [ ] Scrub bar / seek controls in PiP window (Android 12+)
- [ ] Media session artwork: fetch thumbnail from YouTube oEmbed API
- [ ] Test screen-off playback on Android 13/14 (battery optimization may block)

### Content Blocking
- [ ] Implement WorkManager job to auto-update filter lists daily
- [ ] UI for managing filter list subscriptions (add/remove URLs)
- [ ] Per-site shields override persistence (survive app restart)
- [ ] DNS-over-HTTPS implementation (OkHttp + custom DNS resolver)
- [ ] Verify cosmetic filter injection doesn't break page layout

### UI / UX
- [ ] New Tab page with Top Sites grid and quick links
- [ ] Tab groups (visual grouping, color labels)
- [ ] Recently closed tabs list
- [ ] Swipe-to-close tabs in tab grid
- [ ] Address bar suggestions (history + bookmarks autocomplete)
- [ ] Share sheet integration (share URL to other apps)
- [ ] Print page support
- [ ] Dark mode for WebView content (force dark via WebSettings)
- [ ] Full-screen video: show/hide system bars properly
- [ ] Find-in-page result count display

### Permissions
- [ ] Per-site permission dialogs (camera, mic, location, notifications)
- [ ] Persistent permission grants stored in database
- [ ] Permissions manager screen in Settings

### Accessibility
- [ ] TalkBack / accessibility node info for WebView overlays
- [ ] Dynamic font size applied to WebView via `textZoom`
- [ ] High contrast mode

## Nice to Have (Post-v1.0)

- [ ] Extension support via Chromium extension APIs (requires custom build)
- [ ] Browser sync (open-source sync server option)
- [ ] Multi-window support (Android 12 multi-panel)
- [ ] Tablet layout (sidebar navigation)
- [ ] Custom CSS injection
- [ ] Cookie manager UI (view/edit/delete individual cookies)
- [ ] Certificate viewer dialog
- [ ] Phishing / safe browsing integration
- [ ] JavaScript console access (DevTools bridge)
- [ ] HTTPS certificate pinning for the browser's own APIs

## Bugs

- [ ] `onUserLeaveHint` PiP auto-enter may trigger on home button during non-video pages
- [ ] WebView memory leak when tabs are closed (need `webView.destroy()`)
- [ ] `CookieManager.setAcceptThirdPartyCookies` called incorrectly (wrong WebView ref in lambda)
- [ ] Tab card shows no actual page preview (only placeholder icon)
- [ ] History grouping: "Yesterday" calculation breaks across DST boundary
