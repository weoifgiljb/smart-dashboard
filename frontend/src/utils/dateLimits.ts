/**
 * 日期选择器的取值限制。
 */

/**
 * 禁用"今天之前"的日期（Issue #3 Bug 6）。
 *
 * <p>单词书的"开始学习日期"是排程基准：{@code dueDay = startDate + (sectionIndex - 1)}，
 * 选一个过去的日期会让前面若干分区一导入就处于逾期状态，整本书立刻涌进复习队列。
 * 后端 {@code WordService.importWords} 有同样的兜底校验（前端限制可被绕过）。
 *
 * @param date el-date-picker 传入的候选日期（时分秒为 0）
 * @returns 该日期是否应被禁用
 */
export function disablePastDates(date: Date): boolean {
  const startOfToday = new Date()
  startOfToday.setHours(0, 0, 0, 0)
  return date.getTime() < startOfToday.getTime()
}
