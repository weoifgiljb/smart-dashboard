# 将本地项目推送到 GitHub，覆盖旧项目
# 警告：这会完全覆盖 GitHub 上的 smart-dashboard 项目

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "准备推送项目到 GitHub" -ForegroundColor Yellow
Write-Host "目标仓库: https://github.com/weoifgiljb/smart-dashboard.git" -ForegroundColor Yellow
Write-Host "警告：这将覆盖 GitHub 上的现有项目！" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# 检查是否已初始化 git
if (-not (Test-Path .git)) {
    Write-Host "初始化 Git 仓库..." -ForegroundColor Green
    git init
    Write-Host "✓ Git 仓库初始化完成" -ForegroundColor Green
} else {
    Write-Host "✓ Git 仓库已存在" -ForegroundColor Green
}

# 检查是否有 .gitignore，如果没有则创建
if (-not (Test-Path .gitignore)) {
    Write-Host "创建 .gitignore 文件..." -ForegroundColor Green
    @"
# 依赖
node_modules/
frontend/node_modules/
frontend/frontend/node_modules/

# 构建输出
dist/
build/
target/
*.jar
*.class

# 日志
*.log
backend.log

# 环境变量
.env
.env.local
backend/env.bat

# IDE
.idea/
.vscode/
*.iml
*.swp
*.swo

# 操作系统
.DS_Store
Thumbs.db

# 临时文件
*.tmp
*.temp
"@ | Out-File -FilePath .gitignore -Encoding UTF8
    Write-Host "✓ .gitignore 已创建" -ForegroundColor Green
}

# 添加远程仓库
Write-Host ""
Write-Host "配置远程仓库..." -ForegroundColor Green
$remoteUrl = "https://github.com/weoifgiljb/smart-dashboard.git"

# 检查是否已有远程仓库
$existingRemote = git remote get-url origin 2>$null
if ($LASTEXITCODE -eq 0) {
    if ($existingRemote -ne $remoteUrl) {
        Write-Host "更新远程仓库地址..." -ForegroundColor Yellow
        git remote set-url origin $remoteUrl
    } else {
        Write-Host "✓ 远程仓库地址正确" -ForegroundColor Green
    }
} else {
    Write-Host "添加远程仓库..." -ForegroundColor Yellow
    git remote add origin $remoteUrl
    Write-Host "✓ 远程仓库已添加" -ForegroundColor Green
}

# 添加所有文件
Write-Host ""
Write-Host "添加文件到暂存区..." -ForegroundColor Green
git add .
Write-Host "✓ 文件已添加" -ForegroundColor Green

# 创建提交
Write-Host ""
Write-Host "创建提交..." -ForegroundColor Green
$commitMessage = "重构：Vue 3 + Spring Boot 自律组件系统

- 前端：Vue 3 + TypeScript + Vite
- 后端：Spring Boot 3 + MongoDB
- 功能：打卡、背单词、番茄钟、AI对话、书籍推送、数据可视化
- 文档：完整的工作日志和用户手册"
git commit -m $commitMessage
Write-Host "✓ 提交已创建" -ForegroundColor Green

# 确认推送
Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "准备强制推送到 GitHub" -ForegroundColor Yellow
Write-Host "分支: main (如果不存在则创建)" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "按 Enter 继续，或 Ctrl+C 取消..." -ForegroundColor Cyan
Read-Host

# 强制推送到 main 分支
Write-Host ""
Write-Host "推送代码到 GitHub..." -ForegroundColor Green
git push -f origin HEAD:main

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ 推送成功！" -ForegroundColor Green
    Write-Host "项目已更新到: https://github.com/weoifgiljb/smart-dashboard" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "✗ 推送失败" -ForegroundColor Red
    Write-Host "请检查：" -ForegroundColor Yellow
    Write-Host "1. 是否已登录 GitHub" -ForegroundColor Yellow
    Write-Host "2. 是否有推送权限" -ForegroundColor Yellow
    Write-Host "3. 网络连接是否正常" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Red
}

