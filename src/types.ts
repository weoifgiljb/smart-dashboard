export interface Task {
  id?: number;
  title: string;
  description?: string;
  completed: boolean;
  status: "todo" | "progress" | "done";
  position: number;
  createdAt: Date;
  dueDate?: Date;
  tags?: string[];
}

export interface DashboardState {
  theme: "light" | "dark";
  layout: Array<{
    i: string;
    x: number;
    y: number;
    w: number;
    h: number;
    minW?: number;
    minH?: number;
  }>;
  lastUpdated: Date;
}

export type TaskStatus = "todo" | "progress" | "done";
