// src/components/TaskBoard.tsx 最终修正版
import { useEffect, useState } from "react";
import type { Task } from "../types";
import { DashboardDB } from "../services/db";
import {
  DragDropContext,
  Droppable,
  Draggable,
  type DropResult,
  type DraggableProvided,
  type DroppableProvided,
} from "@hello-pangea/dnd";
import styles from "../css/TaskBoard.module.scss";

type TaskStatus = "todo" | "progress" | "done";

interface Props {
  db: DashboardDB;
}

export default function TaskBoard({ db }: Props) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isCreating, setIsCreating] = useState(false);
  const [newTask, setNewTask] = useState<Partial<Task>>({
    title: "",
    priority: "medium",
    status: "todo",
    completed: false,
    description: "",
  });

  useEffect(() => {
    const loadTasks = async () => {
      const allTasks = await db.tasks.toArray();
      setTasks(allTasks.sort((a: Task, b: Task) => a.position - b.position));
    };
    loadTasks();
  }, [db]);

  const handleCreateTask = async () => {
    if (!newTask.title?.trim()) return;

    const taskToAdd: Task = {
      ...newTask as Task,
      createdAt: new Date(),
      updatedAt: new Date(),
      position: tasks.length,
    };

    try {
      const id = await db.tasks.add(taskToAdd);
      setTasks([...tasks, { ...taskToAdd, id }]);
      setNewTask({
        title: "",
        priority: "medium",
        status: "todo",
        completed: false,
        description: "",
      });
      setIsCreating(false);
    } catch (error) {
      console.error("创建任务失败:", error);
    }
  };

  const handleDragEnd = async (result: DropResult) => {
    if (!result.destination) return;

    const newTasks = [...tasks];
    const [movedTask] = newTasks.splice(result.source.index, 1);
    newTasks.splice(result.destination.index, 0, movedTask);

    const validStatuses: TaskStatus[] = ["todo", "progress", "done"];
    const newStatus = validStatuses.includes(
      result.destination.droppableId as TaskStatus
    )
      ? (result.destination.droppableId as TaskStatus)
      : movedTask.status;

    const updatedTasks = newTasks.map((task: Task, index: number) => ({
      ...task,
      position: index,
      status: task.id === movedTask.id ? newStatus : task.status,
    }));

    setTasks(updatedTasks);

    try {
      await db.transaction("rw", db.tasks, async () => {
        await Promise.all(
          updatedTasks.map((task) => {
            const updates: Partial<Task> = { position: task.position };
            if (task.id === movedTask.id) {
              updates.status = newStatus;
            }
            return db.tasks.update(task.id!, updates);
          })
        );
      });
    } catch (error) {
      console.error("更新任务失败:", error);
      const originalTasks = await db.tasks.toArray();
      setTasks(originalTasks.sort((a, b) => a.position - b.position));
    }
  };

  const getStatusTitle = (status: string) => {
    const titles = {
      todo: "待办",
      progress: "进行中",
      done: "已完成"
    };
    return titles[status as keyof typeof titles] || status;
  };

  const getStatusColor = (status: string) => {
    const colors = {
      todo: "bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900 dark:to-blue-800",
      progress: "bg-gradient-to-r from-sky-50 to-sky-100 dark:from-sky-900 dark:to-sky-800",
      done: "bg-gradient-to-r from-indigo-50 to-indigo-100 dark:from-indigo-900 dark:to-indigo-800"
    };
    return colors[status as keyof typeof colors] || "";
  };

  const getPriorityColor = (priority: Task['priority']) => {
    const colors = {
      low: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
      medium: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
      high: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    };
    return colors[priority] || colors.medium;
  };

  const formatDueDate = (date: Date) => {
    const now = new Date();
    const dueDate = new Date(date);
    const diffDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) return '已逾期';
    if (diffDays === 0) return '今天到期';
    if (diffDays === 1) return '明天到期';
    if (diffDays <= 7) return `${diffDays}天后到期`;
    return dueDate.toLocaleDateString();
  };

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <div className={styles.taskBoard}>
        <div className={styles.header}>
          <h1>任务看板</h1>
          <button onClick={() => setIsCreating(true)}>创建任务</button>
        </div>

        {isCreating && (
          <div className={styles.createForm}>
            <div className={styles.formContent}>
              <input
                type="text"
                placeholder="任务标题"
                value={newTask.title}
                onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
              />
              <textarea
                placeholder="任务描述（可选）"
                value={newTask.description}
                onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
              />
              <div className={styles.formControls}>
                <select
                  value={newTask.priority}
                  onChange={(e) => setNewTask({ ...newTask, priority: e.target.value as Task['priority'] })}
                >
                  <option value="low">低优先级</option>
                  <option value="medium">中优先级</option>
                  <option value="high">高优先级</option>
                </select>
                <input
                  type="date"
                  value={newTask.dueDate?.toISOString().split('T')[0] || ''}
                  onChange={(e) => setNewTask({ ...newTask, dueDate: new Date(e.target.value) })}
                />
              </div>
              <div className={styles.buttonGroup}>
                <button
                  onClick={() => setIsCreating(false)}
                  className={styles.cancel}
                >
                  取消
                </button>
                <button
                  onClick={handleCreateTask}
                  className={styles.create}
                >
                  创建
                </button>
              </div>
            </div>
          </div>
        )}

        {["todo", "progress", "done"].map((status) => (
          <div key={status} className={styles.statusColumn}>
            <div className={styles.columnHeader}>
              <h2>{getStatusTitle(status)}</h2>
              <span className={styles.count}>
                ({tasks.filter(task => task.status === status).length})
              </span>
            </div>
            <Droppable droppableId={status}>
              {(provided: DroppableProvided) => (
                <div
                  {...provided.droppableProps}
                  ref={provided.innerRef}
                  className={`${styles.dropZone} ${styles[status]}`}
                >
                  <div className={styles.taskList}>
                    {tasks
                      .filter((task) => task.status === status)
                      .map((task, index) => (
                        <Draggable
                          key={task.id}
                          draggableId={String(task.id)}
                          index={index}
                        >
                          {(provided: DraggableProvided) => (
                            <div
                              ref={provided.innerRef}
                              {...provided.draggableProps}
                              {...provided.dragHandleProps}
                              className={styles.taskCard}
                              style={provided.draggableProps.style}
                            >
                              <div className={styles.taskContent}>
                                <div className={styles.title}>{task.title}</div>
                                {task.description && (
                                  <div className={styles.description}>{task.description}</div>
                                )}
                                <div className={styles.meta}>
                                  <span className={`${styles.priority} ${styles[task.priority]}`}>
                                    {task.priority === 'low' ? '低' : task.priority === 'medium' ? '中' : '高'}
                                  </span>
                                  {task.dueDate && (
                                    <span className={styles.dueDate}>
                                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                      </svg>
                                      {formatDueDate(task.dueDate)}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          )}
                        </Draggable>
                      ))}
                  </div>
                  {provided.placeholder}
                </div>
              )}
            </Droppable>
          </div>
        ))}
      </div>
    </DragDropContext>
  );
}
