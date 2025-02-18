// src/components/Layout.tsx
import React from "react";
import { Outlet } from "react-router-dom";
import Navigation from "./Navigation";
import LoadingOverlay from "./LoadingOverlay";
import ErrorBoundary from "./ErrorBoundary";

export default function Layout() {
  return (
    <ErrorBoundary>
      <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
        {/* 导航栏 - 现在可以正确接收className */}
        <Navigation className="sticky top-0 z-50 shadow-sm" />

        {/* 主体内容区域 */}
        <main className="flex-1 p-6 md:p-8 lg:p-10">
          <div className="max-w-7xl mx-auto">
            <React.Suspense
              fallback={
                <div className="h-[calc(100vh-160px)] flex items-center justify-center">
                  <LoadingOverlay />
                </div>
              }
            >
              <Outlet />
            </React.Suspense>
          </div>
        </main>

        {/* 页脚 */}
        <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 py-6 mt-12">
          <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              © {new Date().getFullYear()} Smart Dashboard. All rights reserved.
              <span className="mx-2">•</span>
              <a
                href="/privacy"
                className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
              >
                Privacy Policy
              </a>
              <span className="mx-2">•</span>
              <a
                href="/terms"
                className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
              >
                Terms of Service
              </a>
            </p>
          </div>
        </footer>
      </div>
    </ErrorBoundary>
  );
}
