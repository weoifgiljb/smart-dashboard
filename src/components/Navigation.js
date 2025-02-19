import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Switch } from "@headlessui/react";
import { MoonIcon, SunIcon } from "@heroicons/react/24/outline";
const Navigation = ({ className, ...props }) => {
    // 带类型的状态管理
    const [darkMode, setDarkMode] = useState(() => {
        if (typeof window === "undefined")
            return false;
        return localStorage.getItem("darkMode") === "true";
    });
    // 主题切换逻辑
    useEffect(() => {
        try {
            const html = document.documentElement;
            darkMode ? html.classList.add("dark") : html.classList.remove("dark");
            localStorage.setItem("darkMode", darkMode.toString());
        }
        catch (error) {
            console.error("主题切换失败:", error);
        }
    }, [darkMode]);
    // 导航项配置
    const navLinks = [
        { name: "仪表盘", path: "/" },
        { name: "任务管理", path: "/tasks" },
    ];
    return (_jsx("nav", { className: `bg-white dark:bg-gray-800 shadow-sm px-4 py-3 ${className || ""}`, ...props, children: _jsxs("div", { className: "max-w-7xl mx-auto flex items-center justify-between", children: [_jsx("div", { className: "flex space-x-4", children: navLinks.map((link) => (_jsx(Link, { to: link.path, className: "text-gray-700 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200", children: link.name }, link.path))) }), _jsx("div", { className: "flex items-center space-x-4", children: _jsxs(Switch, { checked: darkMode, onChange: setDarkMode, className: `relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${darkMode ? "bg-gray-700" : "bg-gray-200"}`, children: [_jsx("span", { className: "sr-only", children: "\u5207\u6362\u4E3B\u9898\u6A21\u5F0F" }), _jsx("span", { className: `inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${darkMode ? "translate-x-6" : "translate-x-1"}` }), _jsx(MoonIcon, { className: `absolute left-1 h-4 w-4 ${darkMode ? "text-gray-300" : "text-gray-400"}` }), _jsx(SunIcon, { className: `absolute right-1 h-4 w-4 ${darkMode ? "text-yellow-300" : "text-yellow-500"}` })] }) })] }) }));
};
export default Navigation;
//# sourceMappingURL=Navigation.js.map