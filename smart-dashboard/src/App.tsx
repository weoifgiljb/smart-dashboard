import { Suspense, type ReactElement, type FC } from "react";
import { Routes, Route } from "react-router-dom";
import ErrorBoundary from "@/components/ErrorBoundary";
import Layout from "@/components/Layout";
import LoadingOverlay from "@/components/LoadingOverlay";
import Dashboard from "@/components/Dashboard";
import "./App.css";

// 错误回退组件
interface ErrorFallbackProps {
  error: Error;
  resetErrorBoundary: () => void;
}

const ErrorFallback: FC<ErrorFallbackProps> = ({
  error,
  resetErrorBoundary,
}) => (
  <div className="p-8 bg-red-50 dark:bg-red-900 rounded-lg max-w-2xl mx-auto my-8" role="alert">
    <h2 className="text-red-600 dark:text-red-200 text-xl font-semibold">应用异常</h2>
    <pre className="mt-4 p-4 bg-white dark:bg-red-800 rounded whitespace-pre-wrap">
      {error.message}
    </pre>
    <button
      onClick={resetErrorBoundary}
      className="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
    >
      重新加载
    </button>
  </div>
);

// 应用根组件
const App: FC = (): ReactElement => (
  <ErrorBoundary
    fallback={
      <ErrorFallback
        error={new Error("初始化失败")}
        resetErrorBoundary={() => window.location.reload()}
      />
    }
  >
    <Suspense fallback={<LoadingOverlay />}>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
        </Routes>
      </Layout>
    </Suspense>
  </ErrorBoundary>
);

export default App;
