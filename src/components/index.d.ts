declare module "@components/ErrorBoundary" {
  import { ComponentType } from "react";
  export const ErrorBoundary: ComponentType<{
    children: React.ReactNode;
    FallbackComponent: ComponentType<{ error: Error }>;
    onReset: () => void;
  }>;
}

declare module "@components/Layout" {
  const Layout: React.ComponentType<{ children: React.ReactNode }>;
  export default Layout;
}

declare module "@components/LoadingOverlay" {
  const LoadingOverlay: React.ComponentType<{
    progress?: number;
    message?: string;
  }>;
  export default LoadingOverlay;
}

declare module "@components/Dashboard" {
  const Dashboard: React.ComponentType;
  export default Dashboard;
}
