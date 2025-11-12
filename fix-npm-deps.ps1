# 修复 npm 依赖问题
$frontendPath = Join-Path $PSScriptRoot "frontend"

Write-Host "正在删除 node_modules 和 package-lock.json..." -ForegroundColor Yellow

# 删除 node_modules
if (Test-Path (Join-Path $frontendPath "node_modules")) {
    Remove-Item -Path (Join-Path $frontendPath "node_modules") -Recurse -Force
    Write-Host "已删除 node_modules" -ForegroundColor Green
}

# 删除 package-lock.json
if (Test-Path (Join-Path $frontendPath "package-lock.json")) {
    Remove-Item -Path (Join-Path $frontendPath "package-lock.json") -Force
    Write-Host "已删除 package-lock.json" -ForegroundColor Green
}

# 切换到 frontend 目录并重新安装依赖
Set-Location $frontendPath
Write-Host "正在重新安装依赖..." -ForegroundColor Yellow
npm install

Write-Host "完成！" -ForegroundColor Green

