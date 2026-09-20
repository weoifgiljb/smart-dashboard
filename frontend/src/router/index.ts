import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { safeRedirect } from '@/utils/safeRedirect'
import { isAuthEntryPath, routeRequiresAuth } from '@/router/authGuard'
import { routes } from '@/router/routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  await userStore.ensureSession()
  if (isAuthEntryPath(to.path)) {
    if (userStore.isAuthenticated) return safeRedirect(to.query.redirect)
    return true
  }
  if (routeRequiresAuth(to) && !userStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
export { routes }
