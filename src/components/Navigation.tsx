import React, { useState, useEffect, type FC, type ReactElement } from "react";
import { Link, type LinkProps } from "react-router-dom";
import { Switch } from "@headlessui/react";
import { MoonIcon, SunIcon } from "@heroicons/react/24/outline";

// 增强类型定义
export interface NavigationLink extends LinkProps {
  name: string;
}

export interface NavigationProps extends React.ComponentPropsWithoutRef<"nav"> {
  links?: NavigationLink[];
  initialDarkMode?: boolean;
}

const Navigation: FC<NavigationProps> = ({
  className = "",
  links = [
    { name: "仪表盘", to: "/" },
    { name: "任务管理", to: "/tasks" },
  ],
  initialDarkMode = false,
  ...props
}): ReactElement => {
  // 优化暗黑模式状态管理
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== "undefined") {
      const savedMode = localStorage.getItem("darkMode");
      return savedMode ? JSON.parse(savedMode) : initialDarkMode;
    }
    return initialDarkMode;
  });

  // 添加系统主题监听
  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => setDarkMode(mediaQuery.matches);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, []);

  // 优化DOM操作
  useEffect(() => {
    document.documentElement.classList.toggle("dark", darkMode);
    localStorage.setItem("darkMode", JSON.stringify(darkMode));
  }, [darkMode]);

  return (
    <nav
      className={`bg-white dark:bg-gray-800 shadow-sm px-4 py-3 ${className}`}
      {...props}
    >
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <div className="flex space-x-4">
          {links.map(({ name, to, ...linkProps }) => (
            <Link
              key={to.toString()}
              to={to}
              className="text-gray-700 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200"
              {...linkProps}
            >
              {name}
            </Link>
          ))}
        </div>

        <div className="flex items-center space-x-4">
          <Switch
            checked={darkMode}
            onChange={setDarkMode}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              darkMode ? "bg-gray-700" : "bg-gray-200"
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                darkMode ? "translate-x-6" : "translate-x-1"
              }`}
            >
              {darkMode ? (
                <MoonIcon className="h-3 w-3 text-gray-700" />
              ) : (
                <SunIcon className="h-3 w-3 text-yellow-500" />
              )}
            </span>
          </Switch>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
