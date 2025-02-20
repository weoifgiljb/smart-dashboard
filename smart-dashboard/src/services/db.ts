import Dexie, { Table } from "dexie";
import type { DashboardState, Task } from "@/types";

export interface DashboardDB extends Dexie {
  state: Table<DashboardState>;
  tasks: Table<Task>;
}

export async function initDB(): Promise<DashboardDB> {
  const db = new Dexie("SmartDashboard") as DashboardDB;

  db.version(1).stores({
    state: "++id,theme,layout,lastUpdated",
    tasks: "++id,title,completed,dueDate,priority",
  });

  await db.open();
  return db;
}