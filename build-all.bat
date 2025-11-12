@echo off
echo ========================================
echo 自律组件 - 前后端构建脚本
echo ========================================
echo.

REM 设置颜色和错误处理
setlocal enabledelayedexpansion

REM 前端构建
echo [1/2] 构建前端项目...
echo ========================================
cd frontend
if exist node_modules (
    echo 前端依赖已存在，跳过安装
) else (
    echo 安装前端依赖...
    call npm install --no-optional
    if !errorlevel! neq 0 (
        echo 错误：前端依赖安装失败！
        cd ..
        goto error_end
    )
)

echo 构建前端...
call npm run build
if !errorlevel! neq 0 (
    echo 警告：前端构建存在问题，继续后端构建...
) else (
    echo 前端构建成功！
)
cd ..
echo.

REM 后端构建
echo [2/2] 构建后端项目...
echo ========================================
cd backend

echo 编译后端项目...
call mvn clean compile
if !errorlevel! neq 0 (
    echo 错误：后端编译失败！
    cd ..
    goto error_end
)

echo 打包后端项目...
call mvn package -DskipTests
if !errorlevel! neq 0 (
    echo 错误：后端打包失败！
    cd ..
    goto error_end
)

cd ..
echo.

REM 验证构建结果
echo ========================================
echo 构建完成！验证产物...
echo ========================================
echo.

if exist "frontend\dist" (
    echo [✓] 前端构建产物: frontend\dist
) else (
    echo [✗] 前端构建产物未找到
)

if exist "backend\target\self-discipline-backend-1.0.0.jar" (
    echo [✓] 后端构建产物: backend\target\self-discipline-backend-1.0.0.jar
) else (
    echo [✗] 后端构建产物未找到
)

echo.
echo ========================================
echo 构建完成！
echo ========================================
echo.
echo 下一步：
echo 1. 启动 MongoDB: mongod
echo 2. 启动后端: cd backend && mvn exec:java
echo 3. 启动前端: cd frontend && npm run dev
echo.
pause
exit /b 0

:error_end
echo.
echo ========================================
echo 构建失败！
echo ========================================
pause
exit /b 1

