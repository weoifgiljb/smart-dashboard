# 环境变量配置说明

## 开发环境配置

在 `frontend` 目录下创建 `.env.local` 文件（此文件不会被提交到 Git）：

```env
# 后端 API 端口（开发环境）
VITE_BACKEND_PORT=8080

# API 基础路径（开发环境使用代理，生产环境需要配置实际 API 地址）
# 开发环境：/api（会被 Vite 代理到后端）
# 生产环境：需要配置实际的后端 API 地址，例如：https://your-api-domain.com/api
VITE_API_BASE=/api

# GitHub Pages base 路径
# 如果仓库名是 username.github.io，设置为 '/'
# 如果仓库名是其他名称，设置为 '/仓库名/'
# 例如：如果仓库名是 'my-project'，则设置为 '/my-project/'
VITE_BASE_PATH=/
```

## 生产环境配置（GitHub Pages）

生产环境的配置通过 GitHub Actions 工作流自动设置，无需手动创建 `.env` 文件。

如果需要自定义生产环境配置，可以在 GitHub 仓库的 `Settings` > `Secrets and variables` > `Actions` 中添加：

- `VITE_API_BASE`: 生产环境的 API 地址（例如：`https://your-api-domain.com/api`）

## 变量说明

- `VITE_BACKEND_PORT`: 开发环境后端服务端口，默认 8080
- `VITE_API_BASE`: API 请求的基础路径
  - 开发环境：`/api`（会被 Vite 代理到 `http://localhost:8080`）
  - 生产环境：需要设置为实际的后端 API 地址
- `VITE_BASE_PATH`: 应用部署的基础路径
  - GitHub Pages（仓库名不是 username.github.io）：`/仓库名/`
  - GitHub Pages（仓库名是 username.github.io）：`/`
  - 其他部署方式：根据实际情况设置

