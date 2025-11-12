@echo off
REM 本文件为本地环境变量配置（不会被提交仓库）。
REM 由 start-backend.bat 自动加载。

REM 你提供的 OpenAI 兼容服务（aabao.top）配置
set "OPENAI_API_KEY=sk-Pu0ISpRB7iRwKuosQ1kMmddc57FKwdGnCBXaJQ1yCPX5sXAN"
set "OPENAI_BASE_URL=https://api.aabao.top/v1"

REM 可选：图片模型与尺寸（按需修改）
set "OPENAI_IMAGE_MODEL=dall-e-3"
set "OPENAI_IMAGE_SIZE=512x512"

echo Local env loaded: OPENAI_BASE_URL=%OPENAI_BASE_URL%, MODEL=%OPENAI_IMAGE_MODEL%, SIZE=%OPENAI_IMAGE_SIZE%




