# GitHub Pages 部署说明

## 自动部署

本项目已配置 GitHub Actions 工作流，当代码推送到 `main` 分支时会自动构建并部署到 GitHub Pages。

## 首次设置

1. 在 GitHub 仓库设置中启用 GitHub Pages：
   - 进入仓库的 `Settings` > `Pages`
   - 在 `Source` 中选择 `GitHub Actions`

2. 配置仓库 Secrets（可选）：
   - 如果后端 API 部署在其他域名，需要在仓库 `Settings` > `Secrets and variables` > `Actions` 中添加：
     - `VITE_API_BASE`: 生产环境的 API 地址（例如：`https://your-api-domain.com/api`）

## 自定义配置

### 修改 base 路径

如果你的仓库名不是 `username.github.io`，需要修改 `.github/workflows/deploy.yml` 中的 base 路径配置。

工作流会自动检测仓库名并设置正确的 base 路径，但如果你需要手动指定，可以修改：

```yaml
VITE_BASE_PATH: '/your-repo-name/'
```

### 配置生产环境 API 地址

1. 在 GitHub 仓库中添加 Secret：
   - `Settings` > `Secrets and variables` > `Actions` > `New repository secret`
   - Name: `VITE_API_BASE`
   - Value: 你的后端 API 地址（例如：`https://api.example.com/api`）

2. 或者直接修改 `.github/workflows/deploy.yml` 中的 `VITE_API_BASE` 值

## 访问地址

部署成功后，可以通过以下地址访问：
- `https://你的用户名.github.io/仓库名/`

如果仓库名是 `username.github.io`，则访问：
- `https://你的用户名.github.io/`

## 注意事项

- GitHub Pages 只能托管静态文件，后端 API 需要单独部署
- 确保后端 API 支持 CORS，允许来自 GitHub Pages 域名的请求
- 如果使用自定义域名，需要在仓库设置中配置，并更新 DNS 记录

