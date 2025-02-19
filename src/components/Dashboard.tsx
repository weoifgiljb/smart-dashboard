import React, { useState, useEffect, useCallback } from "react";
import GridLayout, { Layout } from "react-grid-layout";
import { DashboardDB, initDB } from "../services/db";
import WeatherWidget from "./WeatherWidget";
import TaskBoard from "./TaskBoard";
import PomodoroTimer from "./PomodoroTimer";
import DataChart from "./DataChart";
import { DashboardState, Task } from "../types";
import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

// 增强类型定义
type EnhancedLayout = Layout & {
  minW: number;
  minH: number;
  maxW?: number;
  maxH?: number;
};

const defaultLayout: EnhancedLayout[] = [
  { i: "tasks", x: 0, y: 0, w: 6, h: 4, minW: 4, minH: 3 },
  { i: "weather", x: 6, y: 0, w: 3, h: 2, minW: 2, minH: 2 },
  { i: "timer", x: 9, y: 0, w: 3, h: 4, minW: 2, minH: 3 },
  { i: "chart", x: 0, y: 4, w: 12, h: 4, minW: 8, minH: 3 },
];

const getInitialState = (): DashboardState => ({
  theme: "light",
  layout: defaultLayout,
  lastUpdated: new Date(),
});

export default function Dashboard() {
  const [db, setDB] = useState<DashboardDB | null>(null);
  const [state, setState] = useState<DashboardState>(getInitialState());
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const abortController = new AbortController();

    const initializeDashboard = async () => {
      try {
        const database = await initDB();
        if (abortController.signal.aborted) return;

        const savedState = await database.state.get("dashboard");
        const tasks = await database.tasks.toArray();

        if (savedState) {
          const rawLayout = Array.isArray(savedState.layout)
            ? savedState.layout
            : JSON.parse(String(savedState.layout));

          // 安全合并布局
          const mergedLayout = defaultLayout.map((defaultItem) => {
            const savedItem =
              (rawLayout as EnhancedLayout[]).find(
                (l) => l.i === defaultItem.i
              ) || defaultItem;

            return {
              ...defaultItem,
              x: savedItem.x,
              y: savedItem.y,
              w: Math.max(savedItem.w, defaultItem.minW),
              h: Math.max(savedItem.h, defaultItem.minH),
            };
          });

          setState((prev) => ({
            ...prev,
            ...savedState,
            layout: mergedLayout,
          }));
        }

        setDB(database);
        setTasks(tasks);
        setLoading(false);
      } catch (err) {
        if (err instanceof Error) {
          setError(err.message);
        }
        setLoading(false);
      }
    };

    initializeDashboard();
    return () => abortController.abort();
  }, []);

  const handleLayoutChange = useCallback(
    (newLayout: Layout[]) => {
      if (!db) return;

      const updatedLayout = newLayout.map((l) => ({
        ...l,
        minW: defaultLayout.find((dl) => dl.i === l.i)?.minW || 1,
        minH: defaultLayout.find((dl) => dl.i === l.i)?.minH || 1,
      }));

      const updatedState: DashboardState = {
        ...state,
        layout: updatedLayout,
        lastUpdated: new Date(),
      };

      db.state.put(updatedState, "dashboard");
    },
    [db, state]
  );

  if (loading) return <div className="loading-indicator">加载仪表盘中...</div>;
  if (error) return <div className="error-message">错误: {error}</div>;

  return (
    <div className={`dashboard-container ${state.theme}-theme`}>
      <GridLayout
        className="responsive-grid"
        layout={state.layout}
        cols={12}
        rowHeight={30}
        width={1200}
        margin={[15, 15]}
        containerPadding={[20, 20]}
        onLayoutChange={handleLayoutChange}
        draggableHandle=".widget-header"
        isBounded
      >
        <div key="tasks" className="widget-card">
          <div className="widget-header">任务看板</div>
          {db && <TaskBoard db={db} />}
        </div>
        <div key="weather" className="widget-card">
          <div className="widget-header">天气信息</div>
          <WeatherWidget />
        </div>
        <div key="timer" className="widget-card">
          <div className="widget-header">专注时钟</div>
          <PomodoroTimer />
        </div>
        <div key="chart" className="widget-card">
          <div className="widget-header">数据分析</div>
          <DataChart data={tasks} title="任务统计" />
        </div>
      </GridLayout>
    </div>
  );
}
