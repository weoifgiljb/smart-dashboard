@echo off
chcp 65001 >nul
echo ========================================
echo 准备推送项目到 GitHub
echo 目标仓库: https://github.com/weoifgiljb/smart-dashboard.git
echo 警告：这将覆盖 GitHub 上的现有项目！
echo ========================================
echo.

REM 检查是否已初始化 git
if not exist .git (
    echo 初始化 Git 仓库...
    git init
    echo ✓ Git 仓库初始化完成
) else (
    echo ✓ Git 仓库已存在
)

REM 配置远程仓库
echo.
echo 配置远程仓库...
set REMOTE_URL=https://github.com/weoifgiljb/smart-dashboard.git

REM 检查是否已有远程仓库
git remote get-url origin >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ 远程仓库已存在
    git remote set-url origin %REMOTE_URL%
) else (
    echo 添加远程仓库...
    git remote add origin %REMOTE_URL%
    echo ✓ 远程仓库已添加
)

REM 添加所有文件
echo.
echo 添加文件到暂存区...
git add .
echo ✓ 文件已添加

REM 创建提交
echo.
echo 创建提交...
git commit -m "重构：Vue 3 + Spring Boot 自律组件系统

- 前端：Vue 3 + TypeScript + Vite
- 后端：Spring Boot 3 + MongoDB
- 功能：打卡、背单词、番茄钟、AI对话、书籍推送、数据可视化
- 文档：完整的工作日志和用户手册"
echo ✓ 提交已创建

REM 确认推送
echo.
echo ========================================
echo 准备强制推送到 GitHub
echo 分支: main (如果不存在则创建)
echo ========================================
echo.
pause

REM 强制推送到 main 分支
echo.
echo 推送代码到 GitHub...
git push -f origin HEAD:main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✓ 推送成功！
    echo 项目已更新到: https://github.com/weoifgiljb/smart-dashboard
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ✗ 推送失败
    echo 请检查：
    echo 1. 是否已登录 GitHub
    echo 2. 是否有推送权限
    echo 3. 网络连接是否正常
    echo ========================================
)

pause

