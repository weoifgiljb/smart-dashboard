# 设置控制台编码为 UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 获取脚本所在目录
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

Write-Host "正在清理依赖..." -ForegroundColor Yellow
if (Test-Path "node_modules") {
    Remove-Item -Recurse -Force "node_modules"
}
if (Test-Path "package-lock.json") {
    Remove-Item -Force "package-lock.json"
}

Write-Host "正在重新安装依赖..." -ForegroundColor Yellow
npm install

Write-Host "正在安装 rollup 可选依赖..." -ForegroundColor Yellow
npm install @rollup/rollup-win32-x64-msvc --save-optional --legacy-peer-deps

Write-Host "完成！" -ForegroundColor Green

