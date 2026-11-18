const VERSION = '__VERSION__';
const PATHS = '__PATHS__';
const scope = self.registration.scope;
const prefix = 'dota-spy-web:' + scope + ':';
const cacheName = prefix + VERSION;
const home = new URL('index.html', scope).href;
const files = PATHS.map(path => new URL(path, scope).href);
const urls = new Set(files);

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(cacheName);
    try {
      for (let index = 0; index < files.length; index += 12) {
        await cache.addAll(files.slice(index, index + 12).map(url => new Request(url, { cache: 'reload' })));
      }
    } catch (error) {
      await caches.delete(cacheName);
      throw error;
    }
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith(prefix) && key !== cacheName).map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  url.search = '';
  if (event.request.mode === 'navigate' && (url.href === scope || url.href === home)) {
    event.respondWith(caches.open(cacheName).then(async cache => (await cache.match(home)) || fetch(event.request)));
  } else if (urls.has(url.href)) {
    event.respondWith(caches.open(cacheName).then(async cache => (await cache.match(url.href)) || fetch(event.request)));
  }
});
