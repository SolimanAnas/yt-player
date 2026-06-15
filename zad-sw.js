/* زاد المسلم — Service Worker v2
   Three-layer ad blocking:
   1. CSP meta tag in HTML (browser enforces allowlist)
   2. Inline JS blocker in HTML (active on first load)
   3. This SW (active from second load onward, persists forever)
*/

const CACHE_VER = 'zad-v2';
const CACHE_STATIC = [
  './zad.html',
  './zad-manifest.json',
  './icons/icon-192.png'
];

/* ── Allowlist: these are NEVER blocked ── */
const ALLOW = [
  'mp3quran.net',
  'islamic.network',
  'aladhan.com',
  'alquran.cloud',
  'quranicaudio.com',
  'quran.com',
  'fonts.googleapis.com',
  'fonts.gstatic.com',
  'localhost',
  '127.0.0.1'
];

/* ── 150+ ad/tracker domains (suffix-matched) ── */
const BLOCK_DOMAINS = [
  /* Google ads/tracking */
  'doubleclick.net','googlesyndication.com','googleadservices.com',
  'googletagmanager.com','googletagservices.com','adservice.google.com',
  'google-analytics.com','analytics.google.com','ssl.google-analytics.com',
  'www.google-analytics.com','stats.g.doubleclick.net','ad.doubleclick.net',
  'cm.g.doubleclick.net','tpc.googlesyndication.com','pagead2.googlesyndication.com',
  'adwords.google.com','conversion.googleapis.com','optimize.google.com',
  /* Meta */
  'an.facebook.com','connect.facebook.net','pixel.facebook.com',
  'tr.facebook.com','graph.facebook.com','fbevents.js',
  /* Twitter/X */
  'ads.twitter.com','static.ads-twitter.com','analytics.twitter.com',
  'ads-twitter.com','t.co',
  /* Microsoft */
  'bat.bing.com','clarity.ms','c.clarity.ms','ads.msn.com',
  /* Amazon */
  'amazon-adsystem.com','adsystem.amazon.com','advertising.amazon.com',
  /* Major ad networks */
  'adnxs.com','appnexus.com','adsrvr.org','thetradedesk.com',
  'rubiconproject.com','openx.net','pubmatic.com','casalemedia.com',
  'criteo.com','rtax.criteo.com','dis.criteo.com',
  'taboola.com','cdn.taboola.com','trc.taboola.com',
  'outbrain.com','widgets.outbrain.com','traffic.outbrain.com',
  'media.net','yieldmo.com','sharethrough.com','smartadserver.com',
  'spotxchange.com','spotx.tv','triplelift.com','gumgum.com',
  'loopme.com','moat.com','integral-marketing.com',
  'bidswitch.net','eyereturn.com','brightroll.com',
  'adform.net','adform.com','sizmek.com','flashtalking.com',
  'serving-sys.com','adtechus.com','advertising.com',
  'aol.com','oath.com','verizonmedia.com',
  'undertone.com','yieldmanager.com','lijit.com','sovrn.com',
  /* Pop-up / adult networks */
  'popads.net','popcash.net','trafficjunky.net','trafficjunky.com',
  'propellerads.com','propellerclick.com',
  'mgid.com','revcontent.com','exoclick.com','exosrv.com',
  'plugrush.com','juicyads.com','clickadu.com','hilltopads.net',
  'zedo.com','kontera.com','viglink.com','infolinks.com','skimlinks.com',
  'buysellads.com','buysellads.net','carbonads.com',
  /* Analytics */
  'scorecardresearch.com','b.scorecardresearch.com',
  'quantcast.com','pixel.quantserve.com','e.nexac.com',
  'hotjar.com','fullstory.com','logrocket.com',
  'mouseflow.com','inspectlet.com','crazyegg.com',
  'luckyorange.com','smartlook.com','sessioncam.com',
  'mixpanel.com','segment.com','cdn.segment.com',
  'amplitude.com','heap.io','heapanalytics.com',
  'kissmetrics.com','statcounter.com','woopra.com','clicky.com',
  'chartbeat.com','parsely.com','newrelic.com','nr-data.net',
  /* Marketing automation */
  'marketo.com','pardot.com','hubspot.com','hubspot.net',
  'hs-analytics.net','hs-scripts.com','hsforms.com','hubapi.com',
  /* Data brokers */
  'demdex.net','everesttech.net','2o7.net','omtrdc.net',
  'lotame.com','bluekai.com','exelate.com','eyeota.com',
  'addthis.com','sharethis.com','socialtwist.com',
  'krxd.net','krux.com','semasio.com',
  'tiqcdn.com','tealiumiq.com','tagcommander.com',
  /* Mobile SDK ad networks */
  'adcolony.com','mopub.com','admob.com','inmobi.com',
  'chartboost.com','applovin.com','ironsource.com','fyber.com',
  'vungle.com','mobvista.com','mintegral.com',
  /* Attribution/tracking SDKs */
  'appsflyer.com','adjust.com','branch.io','kochava.com',
  'singular.net','tune.com','moengage.com','clevertap.com',
  'leanplum.com','urbanairship.com','onesignal.com','pushwoosh.com',
  /* Retargeting */
  'adroll.com','perfectaudience.com','fetchback.com',
  'mathtag.com','sync.mathtag.com',
  /* TikTok / Snap */
  'ads.tiktok.com','analytics.tiktok.com','muscdn.com',
  'ads.snapchat.com','tr.snapchat.com','sc-static.net',
  /* LinkedIn */
  'ads.linkedin.com','px.ads.linkedin.com','snap.licdn.com',
  /* Yandex */
  'an.yandex.ru','yadro.ru','metrika.yandex.ru','mc.yandex.ru',
  /* Cedexis / trust */
  'cedexis.com','cedexis-test.com','consensu.org','trustarc.com',
  /* Misc */
  'adconion.com','specificmedia.com','undertone.com',
  'contextweb.com','ybp.yahoo.com'
];

/* ── URL patterns: block if the full URL contains any of these ── */
const BLOCK_PATTERNS = [
  '/ads/', '/ad/', '/advert/', '/adserver/',
  '/analytics/', '/tracker/', '/tracking/', '/telemetry/',
  '/beacon/', '/pixel/', '/collect?', '/collect/',
  '/log?', '/log/', '/event?', '/events?',
  'google-analytics', 'googletagmanager', 'googlesyndication',
  'analytics.js', 'gtag.js', 'ga.js', 'fbevents.js',
  'prebid.js', 'prebid.min.js', '/header-bidding',
  'openrtb', 'bidrequest', 'adrequest',
  'retarget', '/fingerprint', '/fp/',
  'utm_source=', '?adid=', '&adid=', 'clickid=',
  '/sponsor/', '/promoted/', '/native-ad'
];

/* ── Matching logic ── */
function isAllowed(host) {
  return ALLOW.some(a => host === a || host.endsWith('.' + a));
}

function isDomainBlocked(host) {
  // Proper suffix match: prevents evil-doubleclick.com from matching
  return BLOCK_DOMAINS.some(d => host === d || host.endsWith('.' + d));
}

function isPatternBlocked(url) {
  const lower = url.toLowerCase();
  return BLOCK_PATTERNS.some(p => lower.includes(p));
}

function isBlocked(url) {
  try {
    const u = new URL(url);
    const host = u.hostname.toLowerCase().replace(/^www\./, '');
    // Scheme guard: only block http/https
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return false;
    // Allowlist wins
    if (isAllowed(host)) return false;
    // Check domain list first (fast)
    if (isDomainBlocked(host)) return true;
    // Pattern check (slower but catches CDN-served ads)
    return isPatternBlocked(url);
  } catch { return false; }
}

/* ── INSTALL ── */
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_VER)
      .then(c => c.addAll(CACHE_STATIC).catch(() => {}))
      .then(() => self.skipWaiting())
  );
});

/* ── ACTIVATE ── */
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(k => k !== CACHE_VER).map(k => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

/* Empty response used for blocked requests */
const BLOCKED_RESP = () => new Response('', {
  status: 200,
  headers: {
    'Content-Type': 'text/plain',
    'Content-Length': '0',
    'X-Blocked-By': 'ZadAdBlock'
  }
});

/* ── FETCH ── */
self.addEventListener('fetch', e => {
  const url = e.request.url;

  /* 1. Block ads/trackers immediately */
  if (isBlocked(url)) {
    e.respondWith(BLOCKED_RESP());
    return;
  }

  /* 2. Don't cache or intercept audio streaming */
  if (url.includes('.mp3') || url.includes('/audio/')) {
    e.respondWith(
      fetch(e.request, { mode: 'cors' }).catch(() => BLOCKED_RESP())
    );
    return;
  }

  /* 3. Prayer / Quran APIs: network-first */
  if (url.includes('aladhan.com') || url.includes('alquran.cloud')) {
    e.respondWith(
      fetch(e.request).catch(() => caches.match(e.request))
    );
    return;
  }

  /* 4. App shell: cache-first */
  e.respondWith(
    caches.match(e.request).then(cached => {
      if (cached) return cached;
      return fetch(e.request).then(res => {
        if (res && res.status === 200 && res.type !== 'opaque') {
          const clone = res.clone();
          caches.open(CACHE_VER).then(c => c.put(e.request, clone));
        }
        return res;
      }).catch(() => caches.match('./zad.html'));
    })
  );
});
