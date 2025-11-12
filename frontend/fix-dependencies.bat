@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 正在清理依赖...
if exist node_modules rmdir /s /q node_modules
if exist package-lock.json del /f /q package-lock.json

echo 正在重新安装依赖...
call npm install

echo 正在安装 rollup 可选依赖...
call npm install @rollup/rollup-win32-x64-msvc --save-optional --legacy-peer-deps

echo 完成！
pause

