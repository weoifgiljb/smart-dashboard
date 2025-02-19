import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom"; // 新增路由包裹层
import "./index.css";
import App from "./App";

// 确保根节点存在性校验（可选安全增强）
const rootElement = document.getElementById("root");
if (!rootElement) throw new Error("Root element not found");

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      {" "}
      {/* 包裹整个应用 */}
      <App />
    </BrowserRouter>
  </StrictMode>
);
