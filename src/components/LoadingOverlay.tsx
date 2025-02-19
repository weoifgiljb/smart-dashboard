import React from "react";

interface LoadingOverlayProps {
  progress?: number;
}

export default function LoadingOverlay({ progress }: LoadingOverlayProps) {
  return (
    <div className="fixed inset-0 z-50 bg-white dark:bg-gray-800 bg-opacity-90 flex items-center justify-center">
      {/* 旋转加载图标 */}
      <div className="animate-spin">
        <svg
          className="w-16 h-16 text-blue-500"
          fill="none"
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      </div>

      {/* 可选文字提示 */}
      <span className="ml-4 text-gray-600 dark:text-gray-300 text-lg font-medium">
        Loading...
      </span>
    </div>
  );
}
