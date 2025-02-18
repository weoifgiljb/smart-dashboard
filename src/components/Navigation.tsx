import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Switch } from "@headlessui/react";
import { MoonIcon, SunIcon } from "@heroicons/react/24/outline";

// 类型定义
interface NavigationLink {
  name: string;
  path: string;
}

interface NavigationProps extends React.HTMLAttributes<HTMLElement> {
  // 可扩展其他自定义 props
}

const Navigation: React.FC<NavigationProps> = ({ className, ...props }) => {
  // 带类型的状态管理
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("darkMode") === "true";
  });

  // 主题切换逻辑
  useEffect(() => {
    try {
      const html = document.documentElement;
      darkMode ? html.classList.add("dark") : html.classList.remove("dark");
      localStorage.setItem("darkMode", darkMode.toString());
    } catch (error) {
      console.error("主题切换失败:", error);
    }
  }, [darkMode]);

  // 导航项配置
  const navLinks: NavigationLink[] = [
    { name: "仪表盘", path: "/" },
    { name: "任务管理", path: "/tasks" },
  ];

  return (
    <nav
      className={`bg-white dark:bg-gray-800 shadow-sm px-4 py-3 ${
        className || ""
      }`}
      {...props}
    >
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        {/* 导航菜单 */}
        <div className="flex space-x-4">
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className="text-gray-700 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200"
            >
              {link.name}
            </Link>
          ))}
        </div>

        {/* 主题切换控件 */}
        <div className="flex items-center space-x-4">
          <Switch
            checked={darkMode}
            onChange={setDarkMode}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
              darkMode ? "bg-gray-700" : "bg-gray-200"
            }`}
          >
            <span className="sr-only">切换主题模式</span>
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                darkMode ? "translate-x-6" : "translate-x-1"
              }`}
            />
            <MoonIcon
              className={`absolute left-1 h-4 w-4 ${
                darkMode ? "text-gray-300" : "text-gray-400"
              }`}
            />
            <SunIcon
              className={`absolute right-1 h-4 w-4 ${
                darkMode ? "text-yellow-300" : "text-yellow-500"
              }`}
            />
          </Switch>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
