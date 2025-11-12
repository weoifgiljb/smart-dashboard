# GitHub Pages 部署失败修复指南

## 问题
部署失败，错误信息：
- "Branch "main" is not allowed to deploy to github-pages due to environment protection rules"
- "The deployment was rejected or didn't satisfy other protection rules"

## 解决方案

### 方案 1：禁用环境保护规则（推荐）

1. 进入你的 GitHub 仓库
2. 点击 `Settings`（设置）
3. 在左侧菜单中找到 `Environments`（环境）
4. 如果看到 `github-pages` 环境，点击它
5. 找到 `Deployment branches`（部署分支）部分
6. 选择 `All branches`（所有分支）或 `Selected branches` 并添加 `main` 分支
7. 或者直接删除/禁用这个环境（如果不需要保护）

### 方案 2：在仓库设置中启用 GitHub Pages

1. 进入仓库 `Settings` > `Pages`
2. 确保 `Source` 设置为 `GitHub Actions`（不是分支）
3. 如果之前设置的是分支部署，改为 `GitHub Actions`

### 方案 3：手动批准部署（如果需要保护）

如果启用了环境保护规则，每次部署需要手动批准：
1. 进入 `Actions` 标签页
2. 找到失败的部署工作流
3. 点击 `Review deployments`（审查部署）
4. 批准部署

## 快速修复步骤

**最简单的方法：**

1. 访问：`https://github.com/weoifgiljb/smart-dashboard/settings/pages`
2. 确认 `Source` 是 `GitHub Actions`
3. 访问：`https://github.com/weoifgiljb/smart-dashboard/settings/environments`
4. 如果存在 `github-pages` 环境：
   - 点击 `github-pages`
   - 在 `Deployment branches` 中选择 `All branches`
   - 或者删除这个环境（如果不需要）

完成以上步骤后，重新运行工作流或推送新的提交即可。

