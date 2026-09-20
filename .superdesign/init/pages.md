# Page dependency trees

## / (Dashboard)
Entry: `frontend/src/views/Dashboard.vue`
Dependencies:
- `frontend/src/composables/useDashboard.ts`
  - `frontend/src/api/dashboard.ts`
  - `frontend/src/api/checkin.ts`
  - `frontend/src/api/calendar.ts`
  - `frontend/src/api/pomodoro.ts`
  - `frontend/src/api/words.ts`
- `frontend/src/components/charts/BaseChart.vue`
- layout: `frontend/src/layouts/MainLayout.vue`
  - `frontend/src/components/ErrorBoundary.vue`
  - `frontend/src/components/SkeletonPage.vue`
  - `frontend/src/components/ui/AppButton.vue`
  - `frontend/src/store/user.ts`

## /login
Entry: `frontend/src/views/Login.vue`
Dependencies:
- `frontend/src/store/user.ts`

## /register
Entry: `frontend/src/views/Register.vue`
Dependencies:
- `frontend/src/store/user.ts`

## /calendar
Entry: `frontend/src/views/Calendar.vue`
Dependencies:
- `frontend/src/api/calendar.ts`
- `frontend/src/api/checkin.ts`
- `frontend/src/components/charts/BaseChart.vue`
- layout: `frontend/src/layouts/MainLayout.vue`

## /words
Entry: `frontend/src/views/Words.vue`
Dependencies:
- `frontend/src/components/charts/BaseChart.vue`
- `frontend/src/components/virtual/VirtualList.vue`
- `frontend/src/api/ai.ts`
- `frontend/src/utils/sm2.ts`

## /vocabulary/review
Entry: `frontend/src/views/VocabularyReview.vue`
Dependencies:
- `frontend/src/api/words.ts`

## /pomodoro
Entry: `frontend/src/views/Pomodoro.vue`
Dependencies:
- `frontend/src/api/pomodoro.ts`
- `frontend/src/components/charts/BaseChart.vue`

## /tasks
Entry: `frontend/src/views/Tasks.vue`
Dependencies:
- `frontend/src/components/ui/EmptyState.vue`
- `frontend/src/components/charts/BaseChart.vue`
- `frontend/src/composables/useTaskBoard.ts`

## /diary
Entry: `frontend/src/views/Diary.vue`
Dependencies:
- `@guolao/vue-monaco-editor`
- `marked`

## /ai-chat
Entry: `frontend/src/views/AIChat.vue`
Dependencies:
- `frontend/src/api/ai.ts`

## /books
Entry: `frontend/src/views/Books.vue`
Dependencies:
- `frontend/src/api/books.ts`
- `frontend/src/api/ai.ts`

## /books/:id
Entry: `frontend/src/views/BookDetail.vue`
Dependencies:
- `frontend/src/api/books.ts`

Shared styling context:
- `frontend/src/styles/tokens.css`
- `frontend/src/App.vue`
- `frontend/src/main.ts`
