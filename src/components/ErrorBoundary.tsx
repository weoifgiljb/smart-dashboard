import React, { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";

export interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode; // 可选的回退UI
  onError?: (error: Error, errorInfo: ErrorInfo) => void; // 新增错误回调
}

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

export default class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  public state: ErrorBoundaryState = {
    hasError: false,
  };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Error Boundary Catch:", error, errorInfo);
    this.props.onError?.(error, errorInfo);
    this.setState({ errorInfo });
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  public render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <div className="p-6 bg-red-50 dark:bg-red-900 text-red-700 dark:text-red-200 rounded-lg">
            <h3 className="text-lg font-semibold mb-4">
              {this.state.error?.name || "Application Error"}
            </h3>
            <pre className="whitespace-pre-wrap mb-4">
              {this.state.error?.message}
              {this.state.errorInfo?.componentStack}
            </pre>
            <button
              onClick={this.handleReset}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors"
            >
              Try Again
            </button>
          </div>
        )
      );
    }

    return this.props.children;
  }
}
