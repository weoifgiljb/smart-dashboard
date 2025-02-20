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
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== "undefined") {
      const savedMode = localStorage.getItem("darkMode");
      return savedMode ? JSON.parse(savedMode) : initialDarkMode;
    }
    return initialDarkMode;
  });

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => setDarkMode(mediaQuery.matches);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", darkMode);
    localStorage.setItem("darkMode", JSON.stringify(darkMode));
  }, [darkMode]);

  return (
    <nav
      className={`backdrop-blur-md bg-white/95 dark:bg-gray-800/95 px-4 py-3 transition-all duration-300 border-b border-gray-200/50 dark:border-gray-700/50 ${className}`}
      {...props}
    >
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="text-xl font-semibold text-gray-900 dark:text-white">Smart</div>
          <div className="flex space-x-1">
            {links.map(({ name, to, ...linkProps }) => (
              <Link
                key={to.toString()}
                to={to}
                className="relative text-gray-600 dark:text-gray-300 hover:text-emerald-600 dark:hover:text-emerald-400 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 hover:bg-emerald-50 dark:hover:bg-emerald-900/30 group"
                {...linkProps}
              >
                {name}
              </Link>
            ))}
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <Switch
            checked={darkMode}
            onChange={setDarkMode}
            className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 ${
              darkMode ? "bg-blue-700/20 dark:bg-blue-600/30" : "bg-gray-200 dark:bg-gray-700/30"
            } p-1 cursor-pointer`}
          >
            <span
              className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-sm transition-all duration-300 ${
                darkMode ? "translate-x-5" : "translate-x-0"
              } flex items-center justify-center`}
            >
              {darkMode ? (
                <MoonIcon className="h-3 w-3 text-blue-600" />
              ) : (
                <SunIcon className="h-3 w-3 text-amber-500" />
              )}
            </span>
          </Switch>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
