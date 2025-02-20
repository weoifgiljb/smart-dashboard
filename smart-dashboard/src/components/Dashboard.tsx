import { useState, useEffect, useCallback } from "react";
import GridLayout, { Layout } from "react-grid-layout";
import { DashboardDB, initDB } from "@/services/db";
import WeatherWidget from "@/components/WeatherWidget";
import TaskBoard from "@/components/TaskBoard";
import PomodoroTimer from "@/components/PomodoroTimer";
import DataChart from "@/components/DataChart";
import { DashboardState, Task } from "@/types";
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
  { i: "tasks", x: 0, y: 0, w: 6, h: 6, minW: 4, minH: 4 },
  { i: "weather", x: 6, y: 0, w: 6, h: 4, minW: 3, minH: 3 },
  { i: "timer", x: 0, y: 6, w: 6, h: 4, minW: 3, minH: 3 },
  { i: "chart", x: 6, y: 4, w: 6, h: 6, minW: 4, minH: 4 },
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
  const [containerWidth, setContainerWidth] = useState(1200);

  useEffect(() => {
    const updateWidth = () => {
      const width = window.innerWidth - 80; // 增加边距
      setContainerWidth(width);
    };

    updateWidth();
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);

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

          setState((prev: DashboardState) => ({
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
    <div className={`dashboard-container ${state.theme}-theme p-4 min-h-screen bg-white dark:bg-gray-900 transition-colors duration-200`}>
      <GridLayout
        className="responsive-grid"
        layout={state.layout}
        cols={12}
        rowHeight={30}
        width={containerWidth}
        margin={[15, 15]}
        containerPadding={[20, 20]}
        onLayoutChange={handleLayoutChange}
        draggableHandle=".widget-header"
        isBounded
        useCSSTransforms
      >
        <div key="tasks" className="widget-card bg-gradient-to-br from-blue-100/95 to-blue-200/95 dark:from-blue-800/95 dark:to-blue-900/95 backdrop-blur-sm rounded-xl shadow-lg hover:shadow-xl active:shadow-2xl transition-all duration-300 border-2 border-blue-200/70 dark:border-blue-600/70 transform hover:-translate-y-1 active:translate-y-0 group cursor-grab active:cursor-grabbing active:scale-[1.02] active:rotate-1">
          <div className="widget-header p-3 border-b border-gray-200 dark:border-gray-700 font-medium">任务看板</div>
          {db && <TaskBoard db={db} />}
        </div>
        <div key="weather" className="widget-card bg-gradient-to-br from-sky-100/95 to-sky-200/95 dark:from-sky-800/95 dark:to-sky-900/95 backdrop-blur-sm rounded-xl shadow-lg hover:shadow-xl active:shadow-2xl transition-all duration-300 border-2 border-sky-200/70 dark:border-sky-600/70 transform hover:-translate-y-1 active:translate-y-0 group cursor-grab active:cursor-grabbing active:scale-[1.02] active:rotate-1">
          <div className="widget-header p-3 border-b border-gray-200 dark:border-gray-700 font-medium">天气信息</div>
          <WeatherWidget />
        </div>
        <div key="timer" className="widget-card bg-gradient-to-br from-indigo-100/95 to-indigo-200/95 dark:from-indigo-800/95 dark:to-indigo-900/95 backdrop-blur-sm rounded-xl shadow-lg hover:shadow-xl active:shadow-2xl transition-all duration-300 border-2 border-indigo-200/70 dark:border-indigo-600/70 transform hover:-translate-y-1 active:translate-y-0 group cursor-grab active:cursor-grabbing active:scale-[1.02] active:rotate-1">
          <div className="widget-header p-3 border-b border-gray-200 dark:border-gray-700 font-medium">专注时钟</div>
          <PomodoroTimer />
        </div>
        <div key="chart" className="widget-card bg-gradient-to-br from-violet-100/95 to-violet-200/95 dark:from-violet-800/95 dark:to-violet-900/95 backdrop-blur-sm rounded-xl shadow-lg hover:shadow-xl active:shadow-2xl transition-all duration-300 border-2 border-violet-200/70 dark:border-violet-600/70 transform hover:-translate-y-1 active:translate-y-0 group cursor-grab active:cursor-grabbing active:scale-[1.02] active:rotate-1">
          <div className="widget-header p-3 border-b border-gray-200 dark:border-gray-700 font-medium">数据分析</div>
          <DataChart data={tasks} title="任务统计" />
        </div>
      </GridLayout>
    </div>
  );
}
