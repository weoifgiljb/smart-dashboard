export function isAuthEntryPath(path: string) {
  return path === '/login' || path === '/register'
}

export function routeRequiresAuth(to: {
  path: string
  matched: { meta: { requiresAuth?: boolean } }[]
}) {
  if (isAuthEntryPath(to.path)) return false
  return to.matched.some((record) => record.meta.requiresAuth === true)
}
