# GitHub Pages 快速设置指南

## 🚀 快速开始

### 步骤 1：启用 GitHub Pages

1. 进入你的 GitHub 仓库
2. 点击 `Settings`（设置）
3. 在左侧菜单中找到 `Pages`
4. 在 `Source` 部分，选择 `GitHub Actions`

### 步骤 2：推送代码

将代码推送到 `main` 分支，GitHub Actions 会自动开始构建和部署：

```bash
git add .
git commit -m "配置 GitHub Pages 部署"
git push origin main
```

### 步骤 3：查看部署状态

1. 进入仓库的 `Actions` 标签页
2. 查看 `Deploy to GitHub Pages` 工作流的运行状态
3. 等待部署完成（通常需要 2-5 分钟）

### 步骤 4：访问你的网站

部署成功后，你的网站地址将是：

- **如果仓库名是 `username.github.io`**：
  - `https://你的用户名.github.io/`

- **如果仓库名是其他名称**：
  - `https://你的用户名.github.io/仓库名/`

## ⚙️ 配置后端 API（重要）

GitHub Pages 只能托管静态文件，后端需要单独部署。

### 选项 1：使用 GitHub Secrets（推荐）

如果你的后端部署在其他域名（如 Heroku、Railway、Vercel 等）：

1. 进入仓库 `Settings` > `Secrets and variables` > `Actions`
2. 点击 `New repository secret`
3. 添加以下 Secret：
   - **Name**: `VITE_API_BASE`
   - **Value**: 你的后端 API 地址（例如：`https://your-api.herokuapp.com/api`）

### 选项 2：修改工作流文件

直接编辑 `.github/workflows/deploy.yml`，修改 `VITE_API_BASE` 的默认值：

```yaml
VITE_API_BASE: 'https://your-api-domain.com/api'
```

## 🔧 常见问题

### Q: 部署后页面显示 404？

A: 检查以下几点：
1. 确认 GitHub Pages 的 Source 设置为 `GitHub Actions`
2. 检查 base 路径是否正确（查看 `.github/workflows/deploy.yml`）
3. 如果仓库名不是 `username.github.io`，base 路径应该是 `/仓库名/`

### Q: API 请求失败？

A: 确保：
1. 后端 API 已正确部署并运行
2. 后端支持 CORS，允许来自 `*.github.io` 域名的请求
3. 在 GitHub Secrets 中正确配置了 `VITE_API_BASE`

### Q: 如何手动触发部署？

A: 
1. 进入 `Actions` 标签页
2. 选择 `Deploy to GitHub Pages` 工作流
3. 点击 `Run workflow` 按钮

### Q: 部署需要多长时间？

A: 通常需要 2-5 分钟，取决于：
- 依赖安装时间
- 构建时间
- GitHub 服务器负载

## 📝 注意事项

1. **首次部署**：第一次部署可能需要更长时间，因为需要安装所有依赖
2. **更新内容**：每次推送到 `main` 分支都会自动触发部署
3. **自定义域名**：如果需要使用自定义域名，需要在仓库 Settings > Pages 中配置
4. **HTTPS**：GitHub Pages 自动提供 HTTPS，无需额外配置

## 🔗 相关文档

- [GitHub Pages 官方文档](https://docs.github.com/en/pages)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- 项目环境变量配置：`frontend/ENV.md`
- 详细部署说明：`.github/workflows/README.md`

