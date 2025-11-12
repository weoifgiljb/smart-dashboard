# 重启后端服务脚本
# 用于快速重启 Spring Boot 后端

Write-Host "=== 重启后端服务 ===" -ForegroundColor Green
Write-Host ""

# 检查是否在正确的目录
if (-not (Test-Path "backend\pom.xml")) {
    Write-Host "错误: 请在项目根目录运行此脚本" -ForegroundColor Red
    exit 1
}

# 查找并停止正在运行的 Spring Boot 进程
Write-Host "1. 查找正在运行的后端进程..." -ForegroundColor Cyan
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -like "*SelfDisciplineApplication*" -or 
    $_.CommandLine -like "*spring-boot*" -or
    $_.CommandLine -like "*selfdiscipline*"
}

if ($javaProcesses) {
    Write-Host "   找到 $($javaProcesses.Count) 个后端进程，正在停止..." -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Stop-Process -Id $_.Id -Force
        Write-Host "   ✓ 已停止进程 ID: $($_.Id)" -ForegroundColor Green
    }
    Start-Sleep -Seconds 2
} else {
    Write-Host "   没有找到正在运行的后端进程" -ForegroundColor Yellow
}

# 检查端口 8080 是否被占用
Write-Host ""
Write-Host "2. 检查端口 8080..." -ForegroundColor Cyan
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "   端口 8080 仍被占用，尝试释放..." -ForegroundColor Yellow
    $processId = $port8080.OwningProcess
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "   ✓ 端口已释放" -ForegroundColor Green
} else {
    Write-Host "   ✓ 端口 8080 可用" -ForegroundColor Green
}

# 清理并重新编译
Write-Host ""
Write-Host "3. 清理并重新编译..." -ForegroundColor Cyan
Set-Location backend
mvn clean compile -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ 编译失败" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Write-Host "   ✓ 编译成功" -ForegroundColor Green

# 启动后端
Write-Host ""
Write-Host "4. 启动后端服务..." -ForegroundColor Cyan
Write-Host "   后端将在 http://localhost:8080 运行" -ForegroundColor Yellow
Write-Host "   按 Ctrl+C 可以停止服务" -ForegroundColor Yellow
Write-Host ""

mvn spring-boot:run

Set-Location ..

