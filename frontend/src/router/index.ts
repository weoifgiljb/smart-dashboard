import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/NotFound.vue'),
      },
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/views/Calendar.vue'),
      },
      {
        path: 'checkin',
        name: 'CheckIn',
        redirect: '/calendar',
      },
      {
        path: 'words',
        name: 'Words',
        component: () => import('@/views/Words.vue'),
      },
      {
        path: 'vocabulary/review',
        name: 'VocabularyReview',
        component: () => import('@/views/VocabularyReview.vue'),
      },
      {
        path: 'pomodoro',
        name: 'Pomodoro',
        component: () => import('@/views/Pomodoro.vue'),
      },
      {
        path: 'ai-chat',
        name: 'AIChat',
        component: () => import('@/views/AIChat.vue'),
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/Books.vue'),
      },
      {
        path: 'books/:id',
        name: 'BookDetail',
        component: () => import('@/views/BookDetail.vue'),
        props: true,
      },
      {
        path: 'diary',
        name: 'Diary',
        component: () => import('@/views/Diary.vue'),
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/Tasks.vue'),
      },
    ],
  },
]

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

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if ((to.path === '/login' || to.path === '/register') && userStore.isAuthenticated) {
    const redirect = (to.query.redirect as string) || '/'
    next(redirect)
  } else {
    next()
  }
})

export default router
