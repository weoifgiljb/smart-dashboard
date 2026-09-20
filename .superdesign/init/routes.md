# Routes

Config: `frontend/src/router/index.ts` (Vue Router, `createWebHistory`).

Auth: `meta.requiresAuth`; unauthenticated users redirect to `/login`; authenticated users on login/register redirect home.

| Path | Name | Component | Layout |
|------|------|-----------|--------|
| `/login` | Login | `frontend/src/views/Login.vue` | none |
| `/register` | Register | `frontend/src/views/Register.vue` | none |
| `/` | Dashboard | `frontend/src/views/Dashboard.vue` | MainLayout |
| `/calendar` | Calendar | `frontend/src/views/Calendar.vue` | MainLayout |
| `/checkin` | CheckIn | redirect `/calendar` | — |
| `/words` | Words | `frontend/src/views/Words.vue` | MainLayout |
| `/vocabulary/review` | VocabularyReview | `frontend/src/views/VocabularyReview.vue` | MainLayout |
| `/pomodoro` | Pomodoro | `frontend/src/views/Pomodoro.vue` | MainLayout |
| `/ai-chat` | AIChat | `frontend/src/views/AIChat.vue` | MainLayout |
| `/books` | Books | `frontend/src/views/Books.vue` | MainLayout |
| `/books/:id` | BookDetail | `frontend/src/views/BookDetail.vue` | MainLayout |
| `/diary` | Diary | `frontend/src/views/Diary.vue` | MainLayout |
| `/tasks` | Tasks | `frontend/src/views/Tasks.vue` | MainLayout |
| `/*` | NotFound | `frontend/src/views/NotFound.vue` | MainLayout |

## Page summaries

- **Dashboard**: welcome + CTA (打卡/专注) + date range; 4 KPI cards; today tasks; 3 charts; recent activity.
- **Calendar**: month calendar, check-in, heat/stats charts.
- **Words / VocabularyReview**: word list, SM-2 review cards.
- **Pomodoro**: timer ring + history chart.
- **Tasks**: kanban/table + stats chart.
- **Diary**: markdown editor + timeline.
- **AIChat**: chat transcript + input.
- **Books / BookDetail**: waterfall list + detail.
- **Login / Register**: centered card form on gray page.
