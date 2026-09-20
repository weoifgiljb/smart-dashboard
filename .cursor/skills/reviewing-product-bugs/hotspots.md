# 热区与已修特征

审查时按模块搜，不要全仓漫游。已修特征用**代码仍存在**判断，不要写死日期或 commit。

## 已修特征（命中则禁止再报）

| 主题 | 特征（还在就视为已修） |
| --- | --- |
| 默认 JWT | `JwtConfig.validateSecret`；`application.yml` 为 `${JWT_SECRET:}`；`JwtUtil` 不再 padding 短密钥 |
| 连续天数 | `CheckInService.countConsecutiveDays`：今天未打卡则从昨天起算 |
| 热力任务 | `CalendarService.getCalendarData` 任务流含 `"done".equalsIgnoreCase` |
| 导入 SSRF | 存在 `RemoteUrlGuard`；词库/书籍导入调用它且禁止跟随重定向 |
| 书图鉴权 | `generateBookCover` 要 username；限流键 `user:`；已有真实封面不覆盖 |
| 日记 XSS | `Diary.vue` 的 `formatContent` 走 `sanitizeHtml` |
| 番茄 duration | `PomodoroRequest.duration` 有 `@NotNull` |
| 后台计时 | `timerWorker.ts` 导出 `remainingFromDeadline`，用 `Date.now()` 算 deadline |
| 导出 SSRF | `DiaryExportService.safeRemoteImageUrl` 走 `RemoteUrlGuard`；不安全 URL 跳过该图，仍写分隔符 |
| 首页活动 NPE | `DashboardService.getRecentActivities` 用 `safeTime`；番茄 `getType`/`getDuration` 空安全；单词时间为空则跳过 |
| 休息番茄 | `CalendarService.isFocusPomodoro`；热力/KPI/`PomodoroService` 统计只计 `work`（空 type 当 work） |
| 看板 NPE | `TaskService` 的 `blankToDefault` 给 status/priority |
| 子任务扫描 | `TaskRepository.findByOwnerUserIdAndParentId`；`getSubtasks` 不再 `findAll` |
| 无主单词认领 | `ImageService.generateWordImage` 对空 owner / 非本人 403，不认领 |
| 弱注册密码 | `RegisterRequest` `@Size(min=8)`；`AuthService.register` 拒绝不足 8 位 |
| JWT 存储 | `authTokens.ts` 用 sessionStorage 恢复同标签刷新；`AuthCookies` 用 `ResponseCookie`+`SameSite=Lax`；Vite `cookieDomainRewrite`；守卫用 `routeRequiresAuth`；通配路由在子路由最后 |
| 日记 owner | `DiaryService` 写入 `user.getId()`，列表/删除兼容旧 username 行 |
| 日记 VNode JSON | `DiaryTimeline` / `DiaryTags` 为 setup 返回 render；禁止 `{{ h(ElTag) }}` / `:is="h(ElTimelineItem)"` |
| 日记历史回看 | `Diary.vue` 默认列出全部；`全部`/`上一月`/`本月`/`查看某一天` 筛选，不默认藏过去月份 |

首页时长不再 `/60`、仪表盘不再用词表长度覆盖 `wordCount`：也不要再报。

## 鉴权

去看：`JwtConfig`、`JwtUtil`、`SecurityConfig`、`frontend/src/api/request.ts`、`frontend/src/store/user.ts`。

典型坑：仓库内置可伪造密钥；短密钥被 padding；`permitAll` 过宽。JWT 禁止 `localStorage`；同标签刷新靠 `sessionStorage` + `/auth/me`，通配路由须在子路由最后。`ResponseCookie` + Vite `cookieDomainRewrite` 仍在时不要再报刷新掉登录。日记 XSS 已消过毒后不要升成 P0。

## 身份与租户

去看：各 `*Service` 里 `auth.getName()` vs `user.getId()`。

典型坑：日记 `userId` 曾存用户名；现写入 Mongo id 且兼容旧行时，不要再报串号，除非查询仍只按一种 id。

## 热力与节律

去看：`CalendarService`、`CheckInService`、`RhythmPlanner`、`DashboardService`、`PomodoroService`。

典型坑：未完成任务计入热力（已修则跳过）；休息番茄计入热力/KPI（已修则跳过）；`getTotalCount` 不区分 `work`/`break`（已修则跳过）。

## 远程 URL

去看：`WordService.fetchText`、`BookImportService`、`DiaryExportService`、`ImageService`。

典型坑：`file://`、回环、链路本地、`169.254.169.254`、跟随重定向绕过 allowlist。导入已有 `RemoteUrlGuard` 时，去导出/配图是否漏用。

## XSS

去看：`Diary.vue`、`AIChat.vue` 的 `v-html`。

典型坑：`marked.parse` 直接进 `v-html`；`javascript:` 链接。AI 气泡若先 escape 再有限 markdown，不要当未消毒 XSS。

## 空指针与旧数据

去看：`DashboardService.getRecentActivities`、`TaskService.getKanban` / `getStats`。

典型坑：`checkIn.getCreateTime().toString()`；`pomodoro.getType().equals(...)`；`word.getCreateTime()` 为空；`groupingBy(Task::getStatus)` 在 status/priority 为 null 时 NPE。上述已修则跳过。Mongo 反序列化可不走 Java 构造器。

## 任务与单词

去看：`TaskService.getSubtasks`、`createTask` / 依赖、`ImageService.generateWordImage`、`RegisterRequest`。

典型坑：`findAll()` 再滤 `parentId`（已修则跳过）；无主单词谁先点谁认领（已修则跳过）；注册密码只有 `@NotBlank`（已修则跳过）。
