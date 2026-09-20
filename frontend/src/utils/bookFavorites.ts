const LEGACY_KEY = 'favoriteBooks'

export function favoriteStorageKey(userId: string) {
  return `favoriteBooks:${userId || 'anon'}`
}

function readIdList(raw: string | null) {
  if (!raw) return [] as string[]
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is string => typeof item === 'string' && item.length > 0)
  } catch {
    return []
  }
}

export function loadFavoriteIds(userId: string) {
  const scoped = readIdList(localStorage.getItem(favoriteStorageKey(userId)))
  if (scoped.length) return scoped
  const legacy = readIdList(localStorage.getItem(LEGACY_KEY))
  if (legacy.length && userId) {
    localStorage.setItem(favoriteStorageKey(userId), JSON.stringify(legacy))
  }
  return legacy
}

export function saveFavoriteIds(userId: string, ids: string[]) {
  localStorage.setItem(favoriteStorageKey(userId), JSON.stringify(ids))
}

export function toggleFavoriteId(ids: Set<string>, id: string) {
  if (ids.has(id)) {
    ids.delete(id)
    return false
  }
  ids.add(id)
  return true
}
