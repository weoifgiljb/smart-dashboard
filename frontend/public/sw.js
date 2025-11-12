self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open('app-shell-v1').then((cache) =>
      cache.addAll(['/','/index.html'])
    )
  )
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim())
})

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (req.method !== 'GET') return
  event.respondWith(
    caches.match(req).then((cached) => {
      if (cached) return cached
      return fetch(req).then((res) => {
        const resClone = res.clone()
        caches.open('app-shell-v1').then((cache) => cache.put(req, resClone))
        return res
      }).catch(() => cached)
    })
  )
})


