export type TaskStatus = 'todo' | 'in_progress' | 'done' | 'blocked'
export type TaskPriority = 'low' | 'med' | 'high' | 'urgent'

export interface Task {
  id?: string
  ownerUserId?: string
  title: string
  description?: string
  status?: TaskStatus
  priority?: TaskPriority
  tags?: string[]
  startDate?: string
  dueDate?: string
  estimateMinutes?: number
  actualMinutes?: number
  parentId?: string
  dependencyIds?: string[]
  recurrenceRule?: string
  seriesId?: string
  remindAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskHistory {
  id: string
  taskId: string
  actorUserId: string
  action: string
  field?: string
  oldValue?: string
  newValue?: string
  createdAt: string
}
