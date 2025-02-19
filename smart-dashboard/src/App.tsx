import {
  Suspense,
  type ReactElement,
  type FC,
  type PropsWithChildren,
} from "react";
import ErrorBoundary from "../../src/components/ErrorBoundary";
import Layout from "../../src/components/Layout";
import Navigation from "../../src/components/Navigation";
import LoadingOverlay from "../../src/components/LoadingOverlay";
import Dashboard from "../../src/components/Dashboard";
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
  <div
    role="alert"
    style={{
      padding: "2rem",
      backgroundColor: "#ffeef0",
      borderRadius: "8px",
      maxWidth: "600px",
      margin: "2rem auto",
    }}
  >
    <h2 style={{ color: "#ff4d4f" }}>应用异常</h2>
    <pre
      style={{
        whiteSpace: "pre-wrap",
        backgroundColor: "#fff",
        padding: "1rem",
        borderRadius: "4px",
        margin: "1rem 0",
      }}
    >
      {error.message}
    </pre>
    <button
      onClick={resetErrorBoundary}
      style={{
        padding: "0.5rem 1rem",
        backgroundColor: "#1890ff",
        color: "white",
        border: "none",
        borderRadius: "4px",
        cursor: "pointer",
      }}
    >
      重新加载
    </button>
  </div>
);

// 应用布局
const AppLayout: FC<PropsWithChildren> = ({ children }) => (
  <Layout>
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "240px 1fr",
        minHeight: "100vh",
      }}
    >
      <aside
        style={{
          backgroundColor: "#001529",
          padding: "1rem",
          position: "sticky",
          top: 0,
          height: "100vh",
        }}
      >
        <Navigation />
      </aside>

      <main
        style={{
          padding: "2rem",
          overflow: "auto",
          maxHeight: "100vh",
        }}
      >
        {children}
      </main>
    </div>
  </Layout>
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
      <AppLayout>
        <Dashboard />
      </AppLayout>
    </Suspense>
  </ErrorBoundary>
);

App.displayName = "SmartDashboardApp";
export default App;
