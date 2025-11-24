# 项目部署与运行（README）

本 README 聚焦“如何在本地或服务器上快速运行本项目”。功能使用说明请查看《用户手册》，历史沉淀请查看《工作日志》。

## 项目简介

一个帮助自我管理的 Web 应用，聚合了“日历与打卡、番茄钟、单词学习（SM-2 简化）、任务管理、AI 对话、书籍推送”等场景。前端基于 Vue 3 + TypeScript，后端基于 Spring Boot + MongoDB。AI 对话优先使用本地 Ollama，失败时回退到通义千问。

## 核心功能

- 日历 & 打卡：
  - 一键打卡；统计“连续/累计天数”
  - 年度热力图（按活动量量化：打卡/番茄/单词/任务）
- 番茄钟：
  - 25/5 分钟专注节奏，前后台运行稳定
- 单词学习：
  - SM-2 简化算法安排复习；新增/删除/复习与趋势图
- 任务与仪表盘：
  - 任务清单与完成统计，仪表盘汇总关键指标
- 我的日记：
  - 记录每日心情与感悟，支持标签分类与时间轴回顾
- AI 对话：
  - 前端调用后端 `/ai/chat`；后端优先走 Ollama，本地不可用时回退通义千问
- 书籍推送：
  - 瀑布流布局展示，支持分页、搜索、分类、收藏与详情查看

## 技术栈与架构

### 技术选型
- **前端**：Vue 3 (Composition API)、TypeScript、Vite、Pinia、Vue Router、Axios、Element Plus
- **后端**：Spring Boot 3、Spring Security、WebClient、JWT、Spring Data MongoDB
- **AI**：Ollama (本地大模型，优先)、阿里云通义千问 (云端回退)
- **数据库**：MongoDB 4.4+

### 系统架构图

```mermaid
graph TD
    Client[客户端 (Browser)]
    
    subgraph Frontend [前端 (Vue 3 + TypeScript)]
        View[页面视图 (Views)]
        Store[状态管理 (Pinia)]
        API[API 请求层 (Axios)]
    end
    
    subgraph Backend [后端 (Spring Boot 3)]
        Web[Web 层 (Controller)]
        Biz[业务层 (Service)]
        Auth[认证授权 (Security/JWT)]
        DataLayer[数据层 (Repository)]
    end
    
    subgraph Database [数据存储]
        Mongo[(MongoDB)]
    end
    
    subgraph AI_Services [AI 服务]
        LocalAI[本地模型 (Ollama)]
        CloudAI[云端模型 (通义千问)]
    end

    Client -->|HTTP/HTTPS| View
    View --> Store
    Store --> API
    API -->|RESTful API| Web
    
    Web --> Auth
    Web --> Biz
    
    Biz -->|读写| DataLayer
    DataLayer --> Mongo
    
    Biz -->|AI 对话/RAG| LocalAI
    LocalAI -.->|服务不可用| CloudAI
```

## 一、系统要求

- Node.js 18+（推荐 18/20 LTS）
- Java 17（强烈推荐，避免与插件不兼容）
- Maven 3.6+
- MongoDB 4.4+（本地或云端 Atlas 皆可）


## 二、快速启动（推荐）

提供跨平台启动脚本：

### Windows
```powershell
# 启动后端
.\start-backend.bat

# 启动前端
.\start-frontend.bat
```

### Linux / macOS
```bash
# 启动后端
./start-backend.sh

# 启动前端
./start-frontend.sh
```

> 如遇 Java/MongoDB 环境缺失，请参考“环境准备”。


## 三、手动启动（可选）

### 1）准备 MongoDB
```bash
# Windows（服务方式或直接 mongod）
mongod

# Linux/Mac
sudo systemctl start mongod
```

### 2）启动后端
```bash
cd backend
mvn clean compile -DskipTests
mvn spring-boot:run
# 访问 http://localhost:8080
```

### 3）启动前端
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
```


## 四、环境准备与配置

### 1）Java 17 安装（必要）
- Windows 推荐：Eclipse Temurin 17（.msi 安装并勾选 JAVA_HOME 与 PATH）
- 验证：`java -version` 应显示 17

### 2）MongoDB 安装
- 本地安装或使用 MongoDB Atlas 云服务
- 验证端口：`27017` 可连通

### 3）后端配置（backend/src/main/resources/application.yml）
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/self_discipline
```

（如使用 Atlas，请将 uri 替换为云端连接字符串）

### 4）前端开发代理（vite.config.ts，已配置）
- 默认将以 `/api` 代理到 `http://localhost:8080`
- Axios 已启用 `withCredentials: true`


## 五、常见问题（快速排查）

- 后端启动报 Java 版本不兼容
  - 切换到 Java 17；重新 `mvn clean compile && mvn spring-boot:run`
- 前端出现 CORS（跨域）提示
  - 确认后端已重启，前端代理与 Axios 配置生效
- 访问无数据或接口 404
  - 确认后端已启动、MongoDB 正常、接口路径以 `/api` 开头

更多详尽排障与 CORS 最佳实践见《工作日志》相关章节。


## 六、项目结构（简要）

```
.
├── backend/     # Spring Boot 后端
│   └── src/main/java/com/selfdiscipline/...
├── frontend/    # Vue 3 前端
│   └── src/...
├── 工作日志.md
├── 用户手册.md
└── README.md
```


## 七、参考与延伸

- 功能使用与说明：见《用户手册.md》
- 历史记录与技术沉淀：见《工作日志.md》


## 附：Ollama 本地启动与配置（可选，用于本地大模型）

后端已优先尝试通过本地 Ollama 提供 AI 回复（失败时回退到通义千问）。默认地址与模型：`http://127.0.0.1:11434`、`llama3.1`。

### 1）安装 Ollama

- Windows（推荐）
  - 使用 winget：
    ```powershell
    winget install Ollama.Ollama
    ```
  - 或到官网下载安装包：`https://ollama.com`

- macOS（Homebrew）
  ```bash
  brew install ollama
  ```

- Linux
  ```bash
  curl -fsSL https://ollama.com/install.sh | sh
  ```

### 2）启动服务

- 大多数情况下安装完成会自动作为服务运行；若需要手动：
  ```bash
  ollama serve
  ```

### 3）拉取并准备模型

```bash
ollama pull llama3.1
# 或者选择其他模型（例如通义）
# ollama pull qwen2.5:7b
```

### 4）本地连通性测试

```bash
curl http://127.0.0.1:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"llama3.1","prompt":"你好！"}'
```

### 5）后端配置（如需自定义）

在 `backend/src/main/resources/application.yml` 里添加/覆盖：

```yaml
ollama:
  base-url: http://127.0.0.1:11434
  model: llama3.1
```

说明：
- `ollama.base-url` 指向你本地或远程的 Ollama 服务地址
- `ollama.model` 为你已 `pull` 的模型名称

完成后重启后端：
```bash
cd backend
mvn spring-boot:run
```

最后更新：2025-11-21

