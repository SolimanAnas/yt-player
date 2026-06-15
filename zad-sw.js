/* زاد المسلم — Service Worker
   • Caches app shell for offline use
   • Blocks ad/tracker network requests
   • Enables background audio keepalive
*/

const CACHE_VERSION = 'zad-v1';
const CACHE_STATIC = [
  './zad.html',
  './zad-manifest.json',
  './icons/icon-192.png',
  'https://fonts.googleapis.com/css2?family=Cairo:wght@300;400;600;700;900&family=Amiri:wght@400;700&display=swap'
];

/* Ad & tracker domains to block */
const BLOCKED_DOMAINS = [
  'doubleclick.net', 'googlesyndication.com', 'googleadservices.com',
  'googletagmanager.com', 'googletagservices.com', 'adservice.google.com',
  'google-analytics.com', 'analytics.google.com', 'pagead2.googlesyndication.com',
  'adnxs.com', 'adsrvr.org', 'rubiconproject.com', 'pubmatic.com',
  'openx.net', 'criteo.com', 'taboola.com', 'outbrain.com',
  'amazon-adsystem.com', 'scorecardresearch.com', 'hotjar.com',
  'fullstory.com', 'mixpanel.com', 'segment.com', 'amplitude.com',
  'fbcdn.net', 'connect.facebook.net', 'an.facebook.com',
  'ads.twitter.com', 'static.ads-twitter.com',
  'demdex.net', 'everesttech.net', '2o7.net', 'omtrdc.net',
  'quantcast.com', 'lotame.com', 'bluekai.com',
  'casalemedia.com', 'yieldmo.com', 'sharethrough.com',
  'appnexus.com', 'media.net', 'revcontent.com',
  'propellerads.com', 'popads.net', 'popcash.net',
  'trafficjunky.com', 'exoclick.com', 'zedo.com',
  'kontera.com', 'viglink.com', 'infolinks.com',
  'adform.net', 'adjust.com', 'appsflyer.com', 'branch.io',
  'kochava.com', 'tune.com', 'singular.net',
  'clarity.ms', 'bat.bing.com'
];

/* Domains allowed even if they match ad patterns (Islamic content CDNs) */
const ALLOWED_DOMAINS = [
  'cdn.islamic.network', 'server8.mp3quran.net', 'server7.mp3quran.net',
  'server6.mp3quran.net', 'download.quranicaudio.com',
  'api.aladhan.com', 'api.alquran.cloud', 'quran.com',
  'fonts.googleapis.com', 'fonts.gstatic.com'
];

function isBlocked(url) {
  try {
    const host = new URL(url).hostname.replace(/^www\./, '');
    if (ALLOWED_DOMAINS.some(d => host.includes(d))) return false;
    return BLOCKED_DOMAINS.some(d => host.includes(d));
  } catch { return false; }
}

/* ——— Install ——— */
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_VERSION).then(cache =>
      cache.addAll(CACHE_STATIC).catch(() => {})
    ).then(() => self.skipWaiting())
  );
});

/* ——— Activate ——— */
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_VERSION).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

/* ——— Fetch: ad blocking + cache ——— */
self.addEventListener('fetch', e => {
  const { request } = e;
  const url = request.url;

  /* Block ads & trackers — return empty 200 */
  if (isBlocked(url)) {
    e.respondWith(new Response('', {
      status: 200,
      headers: { 'Content-Type': 'text/plain', 'X-Blocked-By': 'ZadAdBlock' }
    }));
    return;
  }

  /* Serve audio from network (streaming), don't cache large files */
  if (url.includes('.mp3') || url.includes('/audio/')) {
    e.respondWith(fetch(request).catch(() => new Response('', { status: 503 })));
    return;
  }

  /* API calls: network first */
  if (url.includes('api.aladhan.com') || url.includes('api.alquran.cloud')) {
    e.respondWith(
      fetch(request).catch(() => caches.match(request))
    );
    return;
  }

  /* App shell: cache first, then network */
  e.respondWith(
    caches.match(request).then(cached => {
      if (cached) return cached;
      return fetch(request).then(response => {
        if (response && response.status === 200 && response.type !== 'opaque') {
          const clone = response.clone();
          caches.open(CACHE_VERSION).then(cache => cache.put(request, clone));
        }
        return response;
      }).catch(() => caches.match('./zad.html'));
    })
  );
});
