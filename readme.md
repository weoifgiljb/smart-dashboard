# Smart Dashboard 🚀

## 项目概述

这里是 Shikisuki , 是用 deepseek + cursor 基于 React + TypeScript 构建的智能仪表盘系统，集成了任务管理、效率工具和实时数据展示功能。使用 Vite 构建，包含完整的测试覆盖和 CI/CD 流水线。

![项目截图]<!-- 此处可以补充截图路径 -->

## 核心功能

### 任务管理看板 📋

- 可拖拽的任务卡片管理
- 任务状态跟踪（待处理/进行中/已完成）
- 任务优先级分类（高/中/低）
- 基于本地存储的数据持久化

### 番茄工作法计时器 ⏳

- 可自定义的工作/休息时间设置
- 循环计时提醒
- 工作历史统计图表
- 桌面通知提醒功能

### 天气组件 🌤️

- 实时天气信息展示
- 3 日天气预报
- 天气预警通知
- 基于地理位置的位置服务

### 开发特性 🛠️

- 基于 Vite 的极速 HMR
- 完整的单元测试覆盖（Vitest）
- E2E 测试（Cypress）
- 代码规范检查（ESLint + Prettier）
- 自动化的 CI/CD 流程

## 技术栈

- **前端框架**: React 18 + TypeScript
- **状态管理**: React Context API
- **样式方案**: CSS Modules + PostCSS
- **测试框架**: Vitest + Testing Library
- **E2E 测试**: Cypress
- **构建工具**: Vite 4
- **代码规范**: ESLint + Prettier
- **持续集成**: GitHub Actions

## 项目结构

```bash
smart-dashboard/
├── src/
│   ├── components/         # 可复用组件
│   ├── services/           # 数据服务层
│   ├── types/              # 类型定义
│   ├── utils/              # 工具函数
│   ├── setupTests.ts       # 测试配置
│   └── ...
├── cypress/                # E2E 测试
├── public/                 # 静态资源
├── .github/               # CI/CD 配置
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 生产构建

```bash
npm run build
```

### 测试

运行单元测试：

```bash
npm test
```

运行 E2E 测试：

```bash
npm run cypress:open
```

## 编码规范

1. **组件设计**:

   - 使用函数式组件 + Hooks
   - 组件目录结构：
     - ComponentName/
       ├── index.tsx # 组件主文件
       ├── styles.module.css
       └── **tests**/ # 测试文件

2. **代码质量**:
   - TypeScript 严格模式
   - 关键路径单元测试覆盖率 > 80%
   - 使用 React DevTools Profiler 优化性能

## 贡献指南

1. 创建特性分支：
   ```bash
   git checkout -b feature/amazing-feature
   ```
2. 提交代码变更
3. 推送到远程仓库：
   ```bash
   git push origin feature/amazing-feature
   ```
4. 提交 Pull Request

## 许可

[MIT](LICENSE) © 2025 ShikiSuki
