import request from './request';

export const createTask = (data: any) => request.post('/tasks', data);
export const listTasks = (params: any) => request.get('/tasks', { params });
export const getTask = (id: string) => request.get(`/tasks/${id}`);
export const updateTask = (id: string, data: any) => request.patch(`/tasks/${id}`, data);
export const deleteTask = (id: string) => request.delete(`/tasks/${id}`);

export const createSubtask = (id: string, data: any) => request.post(`/tasks/${id}/subtasks`, data);
export const getSubtasks = (id: string) => request.get(`/tasks/${id}/subtasks`);

export const addDependency = (id: string, depId: string) => request.post(`/tasks/${id}/dependencies`, null, { params: { depId } });
export const removeDependency = (id: string, depId: string) => request.delete(`/tasks/${id}/dependencies/${depId}`);

export const bulkStatus = (ids: string[], status: string) => request.post('/tasks/bulk/status', { ids, status });

export const getHistory = (id: string) => request.get(`/tasks/${id}/history`);

export const listShares = (id: string) => request.get(`/tasks/${id}/share`);
export const setShare = (id: string, userId: string, role: 'VIEW' | 'EDIT') => request.post(`/tasks/${id}/share`, null, { params: { userId, role } });
export const removeShare = (id: string, userId: string) => request.delete(`/tasks/${id}/share/${userId}`);

export const remindersSoon = (windowMinutes = 60) => request.get('/tasks/reminders/soon', { params: { windowMinutes } });
export const aggregateKanban = () => request.get('/tasks/aggregate/kanban');
export const aggregateStats = () => request.get('/tasks/aggregate/stats');
export const aggregateGantt = (start: string, end: string) => request.get('/tasks/aggregate/gantt', { params: { start, end } });



