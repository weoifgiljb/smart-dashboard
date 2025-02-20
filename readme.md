# Smart Dashboard 🚀

## 项目概述

基于 React 18 + TypeScript 构建的现代化智能仪表盘系统，集成了任务管理、效率工具和实时数据展示功能。采用 Vite 4 构建，具备完整的测试覆盖和自动化部署流程。项目使用最新的 Web 技术栈，提供流畅的用户体验和高效的开发效率。

### 在线预览

访问地址：[http://localhost:3001](http://localhost:3001)

### 主要特点

- 🚀 基于 Vite 4 的极速开发体验
- 💪 TypeScript 全栈类型支持
- 🎨 现代化 UI 设计，支持响应式布局
- 📊 丰富的数据可视化组件
- 🔄 完善的状态管理方案
- 📱 支持 PWA，可离线使用

## 核心功能

### 任务管理看板 📋

- 可拖拽的看板式任务管理
- 支持任务状态实时切换（待处理/进行中/已完成）
- 任务优先级标记系统（高/中/低）
- 支持任务标签和分类管理
- 基于 IndexedDB 的本地数据持久化
- 支持任务导出和数据备份
- 任务完成度统计和进度追踪

### 番茄工作法计时器 ⏳

- 可自定义工作/休息时间配置
- 智能循环计时系统
- 专注时间统计和分析
- 每日/周/月工作报告
- 桌面通知提醒
- 自定义提醒音效
- 支持后台运行

### 天气组件 🌤️

- 实时天气信息展示
- 7 天详细天气预报
- 空气质量指数监测
- 极端天气预警通知
- 多城市天气追踪
- 基于 HTML5 地理定位
- 支持手动位置设置

### 开发特性 🛠️

- 基于 Vite 的极速 HMR
- 完整的单元测试覆盖（Vitest）
- E2E 测试（Cypress）
- 代码规范检查（ESLint + Prettier）
- 自动化的 CI/CD 流程

## 技术栈

- **核心框架**: React 18 + TypeScript 5
- **构建工具**: Vite 4
- **状态管理**: Zustand + Jotai
- **UI 框架**: Mantine UI + Headless UI
- **样式方案**: Tailwind CSS + CSS Modules
- **数据可视化**: Chart.js + React-Grid-Layout
- **数据存储**: Dexie.js (IndexedDB)
- **工具库**:
  - GSAP (动画效果)
  - date-fns (日期处理)
  - Howler.js (音频处理)
  - Lodash (工具函数)
- **测试框架**:
  - Vitest (单元测试)
  - Testing Library (组件测试)
  - Cypress (E2E 测试)
- **代码质量**:
  - ESLint
  - Prettier
  - TypeScript 严格模式
- **开发工具**:
  - Husky (Git Hooks)
  - npm-run-all (并行任务)
  - @faker-js/faker (测试数据生成)

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

[MIT](LICENSE) © 2025 Shiki Suki
