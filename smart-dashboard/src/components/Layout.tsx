// src/components/Layout.tsx
import React from "react";
import Navigation from "./Navigation";
import ErrorBoundary from "./ErrorBoundary";

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  return (
    <ErrorBoundary fallback={null}>
      <div className="min-h-screen flex flex-col bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 transition-all duration-500">
        {/* 导航栏 */}
        <Navigation className="sticky top-0 z-50 backdrop-blur-md bg-white/80 dark:bg-gray-800/80 border-b border-gray-200/50 dark:border-gray-700/50" />

        {/* 主体内容区域 */}
        <main className="flex-1 px-4 py-8 sm:px-6 lg:px-8 xl:px-12">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="bg-white dark:bg-gray-800 shadow-sm rounded-xl p-6 backdrop-blur-lg bg-opacity-95 dark:bg-opacity-95 transition-all duration-300 hover:shadow-md">
              {children}
            </div>
          </div>
        </main>

        {/* 页脚 */}
        <footer className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-md border-t border-gray-200/50 dark:border-gray-700/50 py-6 mt-auto">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-300">
              © {new Date().getFullYear()} Smart Dashboard. All rights reserved.
              <span className="mx-2 text-gray-400 dark:text-gray-500">•</span>
              <a
                href="/privacy"
                className="text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
              >
                Privacy Policy
              </a>
              <span className="mx-2 text-gray-400 dark:text-gray-500">•</span>
              <a
                href="/terms"
                className="text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
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
