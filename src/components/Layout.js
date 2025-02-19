import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/components/Layout.tsx
import React from "react";
import { Outlet } from "react-router-dom";
import Navigation from "./Navigation";
import LoadingOverlay from "./LoadingOverlay";
import ErrorBoundary from "./ErrorBoundary";
export default function Layout() {
    return (_jsx(ErrorBoundary, { children: _jsxs("div", { className: "min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900 transition-colors duration-300", children: [_jsx(Navigation, { className: "sticky top-0 z-50 shadow-sm" }), _jsx("main", { className: "flex-1 p-6 md:p-8 lg:p-10", children: _jsx("div", { className: "max-w-7xl mx-auto", children: _jsx(React.Suspense, { fallback: _jsx("div", { className: "h-[calc(100vh-160px)] flex items-center justify-center", children: _jsx(LoadingOverlay, {}) }), children: _jsx(Outlet, {}) }) }) }), _jsx("footer", { className: "bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 py-6 mt-12", children: _jsx("div", { className: "max-w-7xl mx-auto px-4 md:px-6 lg:px-8 text-center", children: _jsxs("p", { className: "text-sm text-gray-500 dark:text-gray-400", children: ["\u00A9 ", new Date().getFullYear(), " Smart Dashboard. All rights reserved.", _jsx("span", { className: "mx-2", children: "\u2022" }), _jsx("a", { href: "/privacy", className: "hover:text-blue-600 dark:hover:text-blue-400 transition-colors", children: "Privacy Policy" }), _jsx("span", { className: "mx-2", children: "\u2022" }), _jsx("a", { href: "/terms", className: "hover:text-blue-600 dark:hover:text-blue-400 transition-colors", children: "Terms of Service" })] }) }) })] }) }));
}
//# sourceMappingURL=Layout.js.map