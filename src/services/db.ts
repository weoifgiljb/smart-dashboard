import Dexie, { Table } from "dexie";

// 核心接口定义
export interface Task {
  id?: number;
  title: string;
  completed: boolean;
  createdAt: Date;
  status: "todo" | "progress" | "done";
  position: number;
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
  lastUpdated: Date; // 添加缺失的属性
}

export interface WeatherData {
  city: string;
  temperature: number;
  condition: string;
  humidity: number;
  windSpeed: number;
  lastUpdated: Date;
}

export class DashboardDB extends Dexie {
  declare tasks: Table<Task, number>;
  declare weather: Table<WeatherData, string>;
  state!: Table<DashboardState, string>;

  constructor() {
    super("SmartDashboardDB");

    // 版本升级策略
    this.version(2)
      .stores({
        tasks: "++id, title, completed, createdAt",
        state: "++id",
      })
      .upgrade((trans) => {
        trans
          .table("state")
          .toCollection()
          .modify((state) => {
            // 转换旧版布局格式
            if (state.layout && !Array.isArray(state.layout)) {
              state.layout = Object.values(state.layout);
            }
            // 添加默认时间戳
            if (!state.lastUpdated) {
              state.lastUpdated = new Date();
            }
          });
      });

    // 最新版本定义
    this.version(3).stores({
      tasks: "++id, title, completed, createdAt",
      state: "++id",
    });
  }
}

// 初始化数据库
export async function initDB() {
  const db = new DashboardDB();

  await db.transaction("rw", db.state, async () => {
    if ((await db.state.toCollection().count()) === 0) {
      await db.state.add({
        theme: window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light",
        layout: [], // 修正为数组类型
        lastUpdated: new Date(),
      });
    }
  });

  return db;
}
