import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, useEffect } from "react";
import GridLayout from "react-grid-layout";
import { initDB } from "../services/db";
import WeatherWidget from "./WeatherWidget";
import TaskBoard from "./TaskBoard";
import PomodoroTimer from "./PomodoroTimer";
import DataChart from "./DataChart";
const defaultLayout = [
  { i: "tasks", x: 0, y: 0, w: 6, h: 4, minW: 4, minH: 3 },
  { i: "weather", x: 6, y: 0, w: 3, h: 2, minW: 2, minH: 2 },
  { i: "timer", x: 9, y: 0, w: 3, h: 4, minW: 2, minH: 3 },
  { i: "chart", x: 0, y: 4, w: 12, h: 4, minW: 8, minH: 3 },
];
const getInitialState = () => ({
  theme: "light",
  layout: defaultLayout,
  lastUpdated: new Date(),
});
export default function Dashboard() {
  const [db, setDB] = useState();
  const [state, setState] = useState(getInitialState());
  const [tasks, setTasks] = useState([]);
  useEffect(() => {
    const initializeDashboard = async () => {
      try {
        const database = await initDB();
        const savedState = await database.state.get("dashboard");
        setDB(database);
        if (savedState) {
          // 类型安全的数据转换
          const rawLayout = Array.isArray(savedState.layout)
            ? savedState.layout
            : JSON.parse(savedState.layout);
          const mergedLayout = defaultLayout.map((item) => {
            const savedItem = rawLayout.find((l) => l.i === item.i);
            return savedItem
              ? {
                  ...item,
                  x: savedItem.x ?? item.x,
                  y: savedItem.y ?? item.y,
                  w: savedItem.w ?? item.w,
                  h: savedItem.h ?? item.h,
                }
              : item;
          });
          setState({
            theme: savedState.theme || "light",
            layout: mergedLayout,
            lastUpdated: savedState.lastUpdated || new Date(),
          });
        }
        const tasks = await database.tasks.toArray();
        setTasks(tasks);
      } catch (error) {
        console.error("看板初始化失败:", error);
      }
    };
    initializeDashboard();
  }, []);
  const handleLayoutChange = (newLayout) => {
    if (!db) return;
    const updatedState = {
      ...state,
      layout: newLayout.map((l) => ({
        ...l,
        minW: defaultLayout.find((dl) => dl.i === l.i)?.minW,
        minH: defaultLayout.find((dl) => dl.i === l.i)?.minH,
      })),
      lastUpdated: new Date(),
    };
    // 类型安全的存储操作
    db.state.put(
      {
        ...updatedState,
        layout: updatedState.layout.map((l) => ({
          i: l.i,
          x: l.x,
          y: l.y,
          w: l.w,
          h: l.h,
          minW: l.minW,
          minH: l.minH,
        })),
      },
      "dashboard"
    );
  };
  return _jsx("div", {
    className: `${state.theme}-theme p-4 min-h-screen`,
    children: _jsxs(GridLayout, {
      className: "layout",
      layout: state.layout,
      cols: 12,
      rowHeight: 30,
      width: 1200,
      onLayoutChange: handleLayoutChange,
      children: [
        _jsx("div", { children: _jsx(TaskBoard, { db: db }) }, "tasks"),
        _jsx("div", { children: _jsx(WeatherWidget, {}) }, "weather"),
        _jsx("div", { children: _jsx(PomodoroTimer, {}) }, "timer"),
        _jsx(
          "div",
          {
            children: _jsx(DataChart, {
              data: tasks,
              title: "\u4EFB\u52A1\u7EDF\u8BA1",
            }),
          },
          "chart"
        ),
      ],
    }),
  });
}
//# sourceMappingURL=Dashboard.js.map
